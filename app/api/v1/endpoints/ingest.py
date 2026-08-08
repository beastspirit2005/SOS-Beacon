import time
import uuid
from fastapi import APIRouter, Depends, Header, Request
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from ....database.connection import get_db
from ....database.models import SosIncident, GatewayLog
from ....schemas.ingest import IngestRequest, IngestResult
from ....services.signature import verify_signature
from ....services.dedup import is_duplicate
from ....services.groq_ai import enrich_incident
from ....services.sms_notifier import send_sms_notification
from ....services.websocket_manager import manager

router = APIRouter()

@router.post("/ingest", response_model=IngestResult)
async def ingest_sos(
    payload: IngestRequest,
    request: Request,
    x_gateway_id: str = Header(None, alias="X-Gateway-Id"),
    x_app_version: str = Header(None, alias="X-App-Version"),
    db: Session = Depends(get_db)
):
    if not x_gateway_id:
        return JSONResponse(status_code=422, content={"error": {"code": "MISSING_GATEWAY_ID", "message": "X-Gateway-Id header required", "request_id": str(uuid.uuid4())}})
    
    current_time = int(time.time() * 1000)
    packet = payload.packet
    
    try:
        verify_signature(packet, current_time)
    except ValueError as e:
        code = str(e)
        status_code = 401 if code == "BAD_SIGNATURE" else 410
        return JSONResponse(status_code=status_code, content={"error": {"code": code, "message": "Signature/Expiration failed", "request_id": str(uuid.uuid4())}})

    log = GatewayLog(msg_id=packet.msg_id, gateway_id=x_gateway_id, received_at=current_time)
    db.add(log)
    db.commit()

    if is_duplicate(db, packet.msg_id):
        existing = db.query(SosIncident).filter(SosIncident.msg_id == packet.msg_id).first()
        return IngestResult(
            sos_id=existing.sos_id,
            msg_id=packet.msg_id,
            status="duplicate",
            priority="high" if existing.priority >= 4 else "medium", 
            escalation=existing.escalation_tier or "contacts",
            request_id=str(uuid.uuid4())
        )

    summary, priority_tier, escalation = await enrich_incident(packet.payload, packet.severity, packet.priority)
    
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

    await send_sms_notification(packet.origin_id, summary, packet.lat, packet.lon, escalation)

    incident_data = {
        "sos_id": sos_id,
        "lat": packet.lat,
        "lon": packet.lon,
        "summary": summary,
        "severity": packet.severity,
        "escalation": escalation
    }
    await manager.broadcast_incident(incident_data)

    return IngestResult(
        sos_id=sos_id,
        msg_id=packet.msg_id,
        status="accepted",
        priority=priority_tier,
        escalation=escalation,
        request_id=str(uuid.uuid4())
    )
