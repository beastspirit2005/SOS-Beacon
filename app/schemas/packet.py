from pydantic import BaseModel, Field
from typing import Literal

class SosPacket(BaseModel):
    msg_id: str = Field(..., description="uuid v4 — unique per SOS; DEDUP KEY")
    origin_id: str = Field(..., description="anonymised victim device id")
    created_at: int = Field(..., description="epoch ms", gt=0)
    lat: float = Field(..., ge=-90.0, le=90.0)
    lon: float = Field(..., ge=-180.0, le=180.0)
    acc: float = Field(..., ge=0.0, description="gps accuracy (m)")
    severity: Literal["info", "warn", "critical"] = Field(..., description="info | warn | critical")
    priority: int = Field(3, ge=1, le=5, description="1..5 (Packet Priority Engine; default 3)")
    confidence: float = Field(..., ge=0.0, le=1.0, description="0.0..1.0 (trigger confidence)")
    trigger_type: Literal["manual", "partial", "fall", "scream", "no_motion", "missed_checkin", "crash"] = Field(...)
    ttl: int = Field(..., ge=0, description="remaining hops; decremented each relay")
    hops: int = Field(..., ge=0, description="hops so far; incremented each relay")
    payload: str = Field(..., max_length=240, description="short message / partial text (<=240 chars; may be ENCRYPTED)")
    sig: str = Field(..., min_length=1, description="signature over the packet for authenticity")
