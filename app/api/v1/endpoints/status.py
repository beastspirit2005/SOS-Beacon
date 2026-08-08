import uuid
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from ....database.connection import get_db
from ....database.models import SosIncident
from ....schemas.status import DeliveryStatus, AckRequest

router = APIRouter()

@router.get("/{sos_id}/status", response_model=DeliveryStatus)
def get_status(sos_id: str, db: Session = Depends(get_db)):
    incident = db.query(SosIncident).filter(SosIncident.sos_id == sos_id).first()
    if not incident:
        raise HTTPException(status_code=404, detail="Incident not found")
        
    return DeliveryStatus(
        sos_id=incident.sos_id,
        delivery=incident.delivery_status,
        notified_at=incident.received_at, # Simplification for MVP
        request_id=str(uuid.uuid4())
    )

@router.post("/{sos_id}/ack", response_model=DeliveryStatus)
def ack_incident(sos_id: str, payload: AckRequest, db: Session = Depends(get_db)):
    incident = db.query(SosIncident).filter(SosIncident.sos_id == sos_id).first()
    if not incident:
        raise HTTPException(status_code=404, detail="Incident not found")
        
    incident.delivery_status = "acknowledged"
    db.commit()
    
    return DeliveryStatus(
        sos_id=incident.sos_id,
        delivery="acknowledged",
        notified_at=incident.received_at,
        request_id=str(uuid.uuid4())
    )
