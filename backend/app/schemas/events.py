from pydantic import BaseModel
from typing import Optional, Any, Dict

class EventStreamMessage(BaseModel):
    event_type: str  # 'sos.created', 'incident.triaged', 'officer.dispatched', 'incident.resolved'
    timestamp: int
    payload: Dict[str, Any]
    actor_id: Optional[str] = None
