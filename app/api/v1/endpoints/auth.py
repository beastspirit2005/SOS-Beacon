import time
import random
import uuid
from pydantic import BaseModel
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from ....database.connection import get_db
from ....database.models import User, OtpToken
from ....services.email_service import send_otp_email
from ....services.auth import create_access_token, get_current_user

router = APIRouter()

class OtpRequest(BaseModel):
    email: str

class OtpVerifyRequest(BaseModel):
    email: str
    otp_code: str

@router.post("/request-otp")
def request_otp(request: OtpRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == request.email).first()
    
    # Auto-provision user on first OTP request if database is empty or user doesn't exist
    if not user:
        role = "ADMIN" if db.query(User).count() == 0 else "VICTIM"
        user = User(
            id=str(uuid.uuid4()),
            email=request.email,
            name=request.email.split("@")[0].capitalize(),
            role=role,
            created_at=int(time.time() * 1000),
            updated_at=int(time.time() * 1000)
        )
        db.add(user)
        db.commit()

    otp_code = str(random.randint(100000, 999999))
    expires_at = int(time.time() * 1000) + (5 * 60 * 1000)
    
    token_record = OtpToken(
        id=str(uuid.uuid4()),
        email=request.email,
        otp_code=otp_code,
        expires_at=expires_at,
        created_at=int(time.time() * 1000)
    )
    db.add(token_record)
    db.commit()
    
    send_otp_email(request.email, otp_code)
    
    return {"message": "OTP sent successfully to email", "email": request.email}

@router.post("/verify-otp")
def verify_otp(request: OtpVerifyRequest, db: Session = Depends(get_db)):
    current_time = int(time.time() * 1000)
    
    token_record = db.query(OtpToken).filter(
        OtpToken.email == request.email,
        OtpToken.otp_code == request.otp_code,
        OtpToken.is_used == False,
        OtpToken.expires_at > current_time
    ).first()
    
    if not token_record:
        raise HTTPException(status_code=401, detail="Invalid or expired OTP code")
        
    token_record.is_used = True
    db.commit()
    
    user = db.query(User).filter(User.email == request.email).first()
    if not user:
        raise HTTPException(status_code=404, detail="User account not found")
        
    access_token = create_access_token(data={"sub": user.email, "role": user.role, "id": user.id})
    
    return {
        "access_token": access_token,
        "token_type": "bearer",
        "user": {
            "id": user.id,
            "email": user.email,
            "name": user.name,
            "role": user.role
        }
    }

@router.get("/me")
def get_me(current_user: User = Depends(get_current_user)):
    if not current_user:
        raise HTTPException(status_code=401, detail="Not authenticated")
    return {
        "id": current_user.id,
        "email": current_user.email,
        "name": current_user.name,
        "role": current_user.role,
        "region_id": current_user.region_id
    }
