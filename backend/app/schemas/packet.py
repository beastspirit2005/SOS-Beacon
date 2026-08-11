from pydantic import BaseModel, Field, AliasChoices
from typing import Optional

class SosPacket(BaseModel):
    msg_id: str = Field(..., description="Unique packet identifier (e.g. bcn-9a8f2c)")
    origin_id: str = Field(..., description="Unique sender/device ID")
    created_at: int = Field(..., description="Epoch timestamp in ms when created")
    lat: float = Field(..., ge=-90.0, le=90.0)
    lon: float = Field(..., ge=-180.0, le=180.0)
    acc: float = Field(default=10.0, description="GPS accuracy in meters")
    severity: str = Field(default="CRITICAL", description="CRITICAL, WARNING, INFO")
    priority: int = Field(default=3, ge=1, le=5)
    confidence: float = Field(default=1.0, ge=0.0, le=1.0)
    trigger_type: str = Field(default="MANUAL", description="MANUAL, FALL_DETECTION, STILLNESS, SOUND")
    ttl: int = Field(default=7, ge=0, le=15, description="Time to live / max hops remaining")
    hops: int = Field(default=0, ge=0, description="Number of hops traversed")
    payload: str = Field(..., description="Emergency description or message")
    sig: str = Field(default="UNSIGNED", validation_alias=AliasChoices("sig", "signature"), description="ECDSA digital signature or HMAC hash")
    gateway_id: Optional[str] = Field(default=None, description="ID of gateway that ingested this packet")
