import time
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from ....database.connection import get_db
from ....database.models import SosIncident
from ....schemas.packet import SosPacket
from ....services.incident_service import process_incoming_packet

router = APIRouter()

@router.post("/sos/trigger")
def trigger_sos(packet: SosPacket, db: Session = Depends(get_db)):
    """
    Victim API: Direct SOS transmission from browser or field device.
    """
    result = process_incoming_packet(packet, db)
    return {"sos_id": result["sos_id"], "status": "DELIVERED"}

@router.get("/sos/{sos_id}/status")
def get_sos_transmission_status(sos_id: str, db: Session = Depends(get_db)):
    """
    Victim API: Visible progress tracking (Created -> Mesh -> Gateway -> Backend -> Officer Responding).
    """
    incident = db.query(SosIncident).filter(SosIncident.sos_id == sos_id).first()
    if not incident:
        raise HTTPException(status_code=404, detail="SOS Incident not found")
        
    return {
        "sos_id": incident.sos_id,
        "status": incident.status,
        "severity": incident.severity,
        "assigned_officer_id": incident.assigned_officer_id,
        "received_at": incident.received_at,
        "ai_summary": incident.ai_summary
    }

@router.post("/sos/{sos_id}/cancel")
def cancel_sos(sos_id: str, db: Session = Depends(get_db)):
    """
    Victim API: Cancel SOS within 5-second window or false alarm toggle.
    """
    incident = db.query(SosIncident).filter(SosIncident.sos_id == sos_id).first()
    if not incident:
        raise HTTPException(status_code=404, detail="Incident not found")
        
    incident.status = "CANCELLED"
    db.commit()
    return {"message": "SOS Emergency signal cancelled.", "sos_id": sos_id}
