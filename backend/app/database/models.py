from sqlalchemy import Column, Integer, BigInteger, String, Float, Text, Boolean, ForeignKey
from .connection import Base

class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, index=True) # UUID or email
    name = Column(String, nullable=False)
    email = Column(String, unique=True, index=True, nullable=False)
    phone = Column(String, nullable=True)
    password_hash = Column(String, nullable=True)
    role = Column(String, nullable=False) # 'VICTIM', 'OFFICER', 'ADMIN'
    status = Column(String, default="ACTIVE")
    region_id = Column(String, nullable=True)
    created_at = Column(BigInteger, nullable=False)
    updated_at = Column(BigInteger, nullable=False)

class Officer(Base):
    __tablename__ = "officers"

    id = Column(String, primary_key=True, index=True)
    user_id = Column(String, ForeignKey("users.id"), nullable=False)
    organization = Column(String, nullable=False)
    availability = Column(String, default="AVAILABLE") # 'AVAILABLE', 'DISPATCHED', 'OFF_DUTY'
    current_lat = Column(Float, nullable=True)
    current_lon = Column(Float, nullable=True)
    last_seen = Column(BigInteger, nullable=False)

class SosIncident(Base):
    __tablename__ = "sos_incidents"

    sos_id = Column(String, primary_key=True, index=True)
    msg_id = Column(String, unique=True, index=True, nullable=False)
    origin_id = Column(String, index=True, nullable=False)
    victim_id = Column(String, nullable=True)
    assigned_officer_id = Column(String, nullable=True)
    
    created_at = Column(BigInteger, nullable=False) 
    received_at = Column(BigInteger, nullable=False) 
    resolved_at = Column(BigInteger, nullable=True)
    
    lat = Column(Float, nullable=False)
    lon = Column(Float, nullable=False)
    acc = Column(Float, nullable=False)
    region_id = Column(String, nullable=True)
    
    severity = Column(String, nullable=False) # 'CRITICAL', 'WARNING', 'INFO'
    priority = Column(Integer, default=3)
    confidence = Column(Float, nullable=False)
    trigger_type = Column(String, nullable=False)
    payload = Column(Text, nullable=False)
    
    status = Column(String, default="CREATED") # CREATED, DELIVERED, ACKNOWLEDGED, ASSIGNED, RESPONDING, ON_SCENE, RESOLVED, CANCELLED
    
    ai_summary = Column(Text, nullable=True)
    escalation_tier = Column(String, nullable=True)
    cluster_id = Column(String, nullable=True)
    
    notified_at = Column(BigInteger, nullable=True)
    delivery_status = Column(String, default="pending") 

class IncidentEvent(Base):
    __tablename__ = "incident_events"

    id = Column(String, primary_key=True, index=True)
    incident_id = Column(String, ForeignKey("sos_incidents.sos_id"), nullable=False)
    event_type = Column(String, nullable=False)
    actor_id = Column(String, nullable=True)
    metadata_json = Column(Text, nullable=True)
    timestamp = Column(BigInteger, nullable=False)

class IncidentCluster(Base):
    __tablename__ = "incident_clusters"

    id = Column(String, primary_key=True, index=True)
    name = Column(String, nullable=False)
    center_lat = Column(Float, nullable=False)
    center_lon = Column(Float, nullable=False)
    radius_meters = Column(Float, nullable=False)
    victim_count = Column(Integer, default=1)
    confidence = Column(Float, nullable=False)
    created_at = Column(BigInteger, nullable=False)

class MeshNode(Base):
    __tablename__ = "mesh_nodes"

    id = Column(String, primary_key=True, index=True)
    device_id = Column(String, unique=True, index=True, nullable=False)
    role = Column(String, default="RELAY")
    battery = Column(Integer, default=100)
    lat = Column(Float, nullable=True)
    lon = Column(Float, nullable=True)
    gateway_status = Column(Boolean, default=False)
    last_seen = Column(BigInteger, nullable=False)

class GatewayLog(Base):
    __tablename__ = "gateway_logs"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    msg_id = Column(String, index=True, nullable=False)
    gateway_id = Column(String, index=True, nullable=False)
    received_at = Column(BigInteger, nullable=False)

class OtpToken(Base):
    __tablename__ = "otp_tokens"

    id = Column(String, primary_key=True, index=True)
    email = Column(String, index=True, nullable=False)
    otp_code = Column(String(6), nullable=False)
    expires_at = Column(BigInteger, nullable=False)
    is_used = Column(Boolean, default=False)
    created_at = Column(BigInteger, nullable=False)

class AuditLog(Base):
    __tablename__ = "audit_logs"

    id = Column(String, primary_key=True, index=True)
    actor_id = Column(String, nullable=False)
    action = Column(String, nullable=False)
    resource = Column(String, nullable=False)
    resource_id = Column(String, nullable=True)
    metadata_json = Column(Text, nullable=True)
    timestamp = Column(BigInteger, nullable=False)
