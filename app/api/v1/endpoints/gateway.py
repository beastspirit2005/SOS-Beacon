from fastapi import APIRouter, Depends, Header, HTTPException, BackgroundTasks
import time
from sqlalchemy.orm import Session
from ....database.connection import get_db
from ....schemas.packet import SosPacket
from ....services.incident_service import process_incoming_packet
from ....services.signature import verify_signature

router = APIRouter()

@router.post("/ingest")
def ingest_sos_packet(
    packet: SosPacket,
    background_tasks: BackgroundTasks,
    x_gateway_id: str = Header(None),
    db: Session = Depends(get_db)
):
    """
    Gateway Layer Endpoint: Ingests compact SOS packets originating from Android mesh nodes or desktop gateways.
    Deduplicates, executes Groq AI triage, persists to DB, and returns global SOS ID.
    """
    if x_gateway_id and not packet.gateway_id:
        packet.gateway_id = x_gateway_id

    # --- Guard: Signature verification & expiry ---
    current_time = int(time.time() * 1000)
    try:
        verify_signature(packet, current_time)
    except ValueError as e:
        code = str(e)
        status_code = 401 if code == "BAD_SIGNATURE" else 410
        raise HTTPException(status_code=status_code, detail=f"{code}: Signature or expiration check failed")

    result = process_incoming_packet(packet, db, background_tasks)
    return {
        "status": "success",
        "sos_id": result["sos_id"],
        "duplicate": result["duplicate"],
        "message": "Emergency packet processed successfully"
    }
