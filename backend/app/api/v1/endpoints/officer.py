import time
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from ....database.connection import get_db
from ....database.models import User, SosIncident, IncidentEvent
from ....services.auth import require_roles
from ....services.email_service import send_emergency_alert_email

router = APIRouter()

@router.get("/incidents")
def get_officer_incidents(
    since_ms: int = Query(0),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles(["OFFICER", "ADMIN"]))
):
    """
    Officer API: Fetches priority active incident queue & tactical map data.
    Role-scoped by region if officer has an assigned region.
    """
    query = db.query(SosIncident).filter(SosIncident.received_at > since_ms)
    
    if current_user.role == "OFFICER" and current_user.region_id:
        query = query.filter(SosIncident.region_id == current_user.region_id)
        
    incidents = query.order_by(SosIncident.priority.desc(), SosIncident.received_at.desc()).all()
    
    return {
        "count": len(incidents),
        "incidents": [
            {
                "sos_id": inc.sos_id,
                "msg_id": inc.msg_id,
                "lat": inc.lat,
                "lon": inc.lon,
                "severity": inc.severity,
                "priority": inc.priority,
                "status": inc.status,
                "payload": inc.payload,
                "ai_summary": inc.ai_summary,
                "escalation_tier": inc.escalation_tier,
                "cluster_id": inc.cluster_id,
                "received_at": inc.received_at,
                "assigned_officer_id": inc.assigned_officer_id
            } for inc in incidents
        ]
    }

@router.post("/incidents/{sos_id}/accept")
def accept_assignment(
    sos_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles(["OFFICER", "ADMIN"]))
):
    incident = db.query(SosIncident).filter(SosIncident.sos_id == sos_id).first()
    if not incident:
        raise HTTPException(status_code=404, detail="Incident not found")
        
    incident.status = "ASSIGNED"
    incident.assigned_officer_id = current_user.id
    db.commit()
    return {"message": "Incident assigned to officer", "sos_id": sos_id}

@router.post("/incidents/{sos_id}/status")
def update_response_status(
    sos_id: str,
    new_status: str = Query(..., description="RESPONDING, ON_SCENE, RESOLVED"),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_roles(["OFFICER", "ADMIN"]))
):
    incident = db.query(SosIncident).filter(SosIncident.sos_id == sos_id).first()
    if not incident:
        raise HTTPException(status_code=404, detail="Incident not found")
        
    incident.status = new_status.upper()
    if new_status.upper() == "RESOLVED":
        incident.resolved_at = int(time.time() * 1000)
        
    db.commit()
    return {"message": f"Status updated to {new_status}", "sos_id": sos_id}
