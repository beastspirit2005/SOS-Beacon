from pydantic import BaseModel, Field
from typing import Optional

class SosPacket(BaseModel):
    msg_id: str = Field(..., description="uuid v4 — unique per SOS; DEDUP KEY")
    origin_id: str = Field(..., description="anonymised victim device id")
    created_at: int = Field(..., description="epoch ms")
    lat: float
    lon: float
    acc: float = Field(..., description="gps accuracy (m)")
    severity: str = Field(..., description="'info' | 'warn' | 'critical'")
    priority: int = Field(3, description="1..5 (Packet Priority Engine; default 3)")
    confidence: float = Field(..., description="0.0..1.0 (trigger confidence)")
    trigger_type: str = Field(..., description="'manual'|'partial'|'fall'|'scream'|'no_motion'|'missed_checkin'|'crash'")
    ttl: int = Field(..., description="remaining hops; decremented each relay")
    hops: int = Field(..., description="hops so far; incremented each relay")
    payload: str = Field(..., max_length=240, description="short message / partial text (<=240 chars; may be ENCRYPTED)")
    sig: str = Field(..., description="signature over the packet for authenticity")
