from fastapi import APIRouter, Depends, Header
from sqlalchemy.orm import Session
from ....database.connection import get_db
from ....schemas.packet import SosPacket
from ....services.incident_service import process_incoming_packet

router = APIRouter()

@router.post("/ingest")
def ingest_sos_packet(
    packet: SosPacket,
    x_gateway_id: str = Header(None),
    db: Session = Depends(get_db)
):
    """
    Gateway Layer Endpoint: Ingests compact SOS packets originating from Android mesh nodes or desktop gateways.
    Deduplicates, executes Groq AI triage, persists to DB, and returns global SOS ID.
    """
    if x_gateway_id and not packet.gateway_id:
        packet.gateway_id = x_gateway_id

    result = process_incoming_packet(packet, db)
    return {
        "status": "success",
        "sos_id": result["sos_id"],
        "duplicate": result["duplicate"],
        "message": "Emergency packet processed successfully"
    }
