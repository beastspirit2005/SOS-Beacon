from sqlalchemy.orm import Session
from ..database.models import SosIncident

def is_duplicate(db: Session, msg_id: str) -> bool:
    existing = db.query(SosIncident).filter(SosIncident.msg_id == msg_id).first()
    return existing is not None

def get_existing_incident(db: Session, msg_id: str):
    return db.query(SosIncident).filter(SosIncident.msg_id == msg_id).first()
