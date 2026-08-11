from fastapi import APIRouter, Depends, Header, Request
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from typing import Union
import time
import uuid

from ....database.connection import get_db
from ....schemas.packet import SosPacket
from ....schemas.ingest import IngestRequest, IngestResult
from ....services.incident_service import process_incoming_packet
from ....services.signature import verify_signature

router = APIRouter()

@router.post("/ingest")
def ingest_sos_packet(
    payload: Union[IngestRequest, SosPacket],
    x_gateway_id: str = Header(None, alias="X-Gateway-Id"),
    db: Session = Depends(get_db)
):
    """
    Gateway Layer Endpoint: Ingests SOS packets. Supports both wrapped IngestRequest
    and direct SosPacket body payloads (backward compatibility).
    Performs signature checks, deduplication, and LLM triage.
    """
    req_id = str(uuid.uuid4())
    
    # 1. Resolve payload type to extraction of packet
    if isinstance(payload, IngestRequest):
        packet = payload.packet
    else:
        packet = payload

    # 2. X-Gateway-Id header injection
    if x_gateway_id and not packet.gateway_id:
        packet.gateway_id = x_gateway_id

    # 3. Perform signature verification & expiry checks
    current_time = int(time.time() * 1000)
    try:
        verify_signature(packet, current_time)
    except ValueError as e:
        code = str(e)
        status_code = 401 if code == "BAD_SIGNATURE" else 410
        return JSONResponse(
            status_code=status_code,
            content={
                "error": {
                    "code": code,
                    "message": "Signature or expiration check failed",
                    "request_id": req_id
                }
            }
        )

    # 4. Process packet (deduplicate, AI enrich, persist, broadcast)
    result = process_incoming_packet(packet, db)
    
    priority_tier = "high" if packet.priority >= 4 else ("medium" if packet.priority == 3 else "low")
    
    # Return structure matching the IngestResult REST contract
    return {
        "sos_id": result["sos_id"],
        "msg_id": packet.msg_id,
        "status": "duplicate" if result.get("duplicate") else "accepted",
        "priority": priority_tier,
        "escalation": "responders" if packet.priority >= 4 else "contacts",
        "request_id": req_id
    }
