from pydantic import BaseModel
from typing import Optional
from .packet import SosPacket

class IngestRequest(BaseModel):
    packet: SosPacket
    received_at: int

class IngestResult(BaseModel):
    sos_id: str
    msg_id: str
    status: str
    priority: str
    escalation: str
    request_id: str
