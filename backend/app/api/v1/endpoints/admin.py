import time
import uuid
from pydantic import BaseModel
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from ....database.connection import get_db
from ....database.models import User, SosIncident, GatewayLog, IncidentCluster, MeshNode
from ....services.auth import require_roles

router = APIRouter()

class UserProvisionRequest(BaseModel):
    email: str
    name: str
    role: str # VICTIM, OFFICER, ADMIN
    region_id: str | None = None

@router.get("/stats")
def get_national_stats(
    db: Session = Depends(get_db)
):
    total_incidents = db.query(SosIncident).count()
    active_emergencies = db.query(SosIncident).filter(SosIncident.status != "RESOLVED").count()
    active_gateways = db.query(GatewayLog.gateway_id).distinct().count()
    total_clusters = db.query(IncidentCluster).count()
    
    return {
        "total_incidents": total_incidents,
        "active_emergencies": active_emergencies,
        "active_gateways": active_gateways,
        "mass_casualty_clusters": total_clusters
    }

@router.get("/clusters")
def get_mass_casualty_clusters(
    db: Session = Depends(get_db)
):
    clusters = db.query(IncidentCluster).all()
    return {"clusters": clusters}

@router.post("/users/provision")
def provision_user(
    req: UserProvisionRequest,
    db: Session = Depends(get_db)
):
    existing = db.query(User).filter(User.email == req.email).first()
    if existing:
        raise HTTPException(status_code=400, detail="User already exists")
        
    user = User(
        id=str(uuid.uuid4()),
        email=req.email,
        name=req.name,
        role=req.role.upper(),
        region_id=req.region_id,
        created_at=int(time.time() * 1000),
        updated_at=int(time.time() * 1000)
    )
    db.add(user)
    db.commit()
    return {"message": "User provisioned successfully", "user_id": user.id}
