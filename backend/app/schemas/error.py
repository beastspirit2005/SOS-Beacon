from pydantic import BaseModel

class ErrorDetails(BaseModel):
    code: str
    message: str
    request_id: str

class ErrorEnvelope(BaseModel):
    error: ErrorDetails
