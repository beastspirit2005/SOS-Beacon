import time
import uuid
import logging
from fastapi import APIRouter, Depends, Header, Request
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from ....database.connection import get_db
from ....database.models import SosIncident, GatewayLog
from ....schemas.ingest import IngestRequest, IngestResult
from ....services.signature import verify_signature
from ....services.dedup import is_duplicate, get_existing_incident
from ....services.groq_ai import enrich_incident
from ....services.sms_notifier import send_sms_notification
from ....services.websocket_manager import manager

router = APIRouter()
logger = logging.getLogger(__name__)

# Simple in-memory rate limiter: gateway_id -> (count, window_start_ms)
_rate_limit_store: dict = {}
RATE_LIMIT_MAX = 20       # max requests per window
RATE_LIMIT_WINDOW_MS = 60 * 1000  # 1 minute window

def _check_rate_limit(gateway_id: str, current_time_ms: int) -> bool:
    """Returns True if the request is allowed, False if rate-limited."""
    record = _rate_limit_store.get(gateway_id)
    if record is None:
        _rate_limit_store[gateway_id] = (1, current_time_ms)
        return True
    count, window_start = record
    if current_time_ms - window_start > RATE_LIMIT_WINDOW_MS:
        _rate_limit_store[gateway_id] = (1, current_time_ms)
        return True
    if count >= RATE_LIMIT_MAX:
        return False
    _rate_limit_store[gateway_id] = (count + 1, window_start)
    return True


@router.post("/ingest", response_model=IngestResult)
async def ingest_sos(
    payload: IngestRequest,
    request: Request,
    x_gateway_id: str = Header(None, alias="X-Gateway-Id"),
    x_app_version: str = Header(None, alias="X-App-Version"),
    db: Session = Depends(get_db)
):
    req_id = str(uuid.uuid4())

    # --- Guard: X-Gateway-Id required ---
    if not x_gateway_id:
        return JSONResponse(
            status_code=422,
            content={"error": {"code": "MISSING_GATEWAY_ID", "message": "X-Gateway-Id header required", "request_id": req_id}}
        )

    current_time = int(time.time() * 1000)
    packet = payload.packet

    # --- Guard: Rate limiting per gateway ---
    if not _check_rate_limit(x_gateway_id, current_time):
        logger.warning(f"Rate limit exceeded for gateway: {x_gateway_id}")
        return JSONResponse(
            status_code=429,
            content={"error": {"code": "RATE_LIMITED", "message": "Too many packets from this gateway", "request_id": req_id}}
        )

    # --- Guard: Signature verification & expiry ---
    try:
        verify_signature(packet, current_time)
    except ValueError as e:
        code = str(e)
        status_code = 401 if code == "BAD_SIGNATURE" else 410
        return JSONResponse(
            status_code=status_code,
            content={"error": {"code": code, "message": "Signature or expiration check failed", "request_id": req_id}}
        )

    # --- Audit: Log gateway forwarding attempt ---
    log = GatewayLog(msg_id=packet.msg_id, gateway_id=x_gateway_id, received_at=current_time)
    db.add(log)
    db.commit()

    # --- Guard: Idempotent deduplication on msg_id ---
    if is_duplicate(db, packet.msg_id):
        existing = get_existing_incident(db, packet.msg_id)
        logger.info(f"Duplicate packet received: msg_id={packet.msg_id} from gateway={x_gateway_id}")
        priority_tier = "high" if existing.priority >= 4 else ("medium" if existing.priority == 3 else "low")
        return IngestResult(
            sos_id=existing.sos_id,
            msg_id=packet.msg_id,
            status="duplicate",
            priority=priority_tier,
            escalation=existing.escalation_tier or "contacts",
            request_id=req_id
        )

    # --- Groq AI Enrichment (with safe fallback) ---
    summary, priority_tier, escalation = await enrich_incident(packet.payload, packet.severity, packet.priority)

    # --- Persist Incident to Neon DB ---
    sos_id = f"beacon-sos-{uuid.uuid4().hex[:8]}"
    incident = SosIncident(
        sos_id=sos_id,
        msg_id=packet.msg_id,
        origin_id=packet.origin_id,
        created_at=packet.created_at,
        lat=packet.lat,
        lon=packet.lon,
        acc=packet.acc,
        severity=packet.severity,
        priority=packet.priority,
        confidence=packet.confidence,
        trigger_type=packet.trigger_type,
        payload=packet.payload,
        ai_summary=summary,
        escalation_tier=escalation,
        received_at=current_time
    )
    db.add(incident)
    db.commit()

    logger.info(f"Incident accepted: sos_id={sos_id}, priority={priority_tier}, escalation={escalation}")

    # --- Dispatch SMS ---
    await send_sms_notification(packet.origin_id, summary, packet.lat, packet.lon, escalation)

    # --- Broadcast to WebSocket Dashboard ---
    await manager.broadcast_incident({
        "sos_id": sos_id,
        "lat": packet.lat,
        "lon": packet.lon,
        "summary": summary,
        "severity": packet.severity,
        "priority": priority_tier,
        "escalation": escalation
    })

    return IngestResult(
        sos_id=sos_id,
        msg_id=packet.msg_id,
        status="accepted",
        priority=priority_tier,
        escalation=escalation,
        request_id=req_id
    )
