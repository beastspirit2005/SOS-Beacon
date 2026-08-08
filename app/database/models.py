from sqlalchemy import Column, Integer, BigInteger, String, Float, Text
from .connection import Base

class SosIncident(Base):
    __tablename__ = "sos_incidents"

    sos_id = Column(String, primary_key=True, index=True)
    msg_id = Column(String, unique=True, index=True, nullable=False)
    origin_id = Column(String, index=True, nullable=False)
    created_at = Column(BigInteger, nullable=False) 
    lat = Column(Float, nullable=False)
    lon = Column(Float, nullable=False)
    acc = Column(Float, nullable=False)
    severity = Column(String, nullable=False)
    priority = Column(Integer, default=3)
    confidence = Column(Float, nullable=False)
    trigger_type = Column(String, nullable=False)
    payload = Column(Text, nullable=False)
    
    ai_summary = Column(Text, nullable=True)
    escalation_tier = Column(String, nullable=True)
    
    delivery_status = Column(String, default="pending") 
    received_at = Column(BigInteger, nullable=False) 

class GatewayLog(Base):
    __tablename__ = "gateway_logs"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    msg_id = Column(String, index=True, nullable=False)
    gateway_id = Column(String, index=True, nullable=False)
    received_at = Column(BigInteger, nullable=False)
