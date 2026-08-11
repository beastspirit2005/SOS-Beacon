from pydantic import BaseModel
from typing import Optional

class DeliveryStatus(BaseModel):
    sos_id: str
    delivery: str
    notified_at: Optional[int] = None
    request_id: str

class AckRequest(BaseModel):
    responder_id: str
