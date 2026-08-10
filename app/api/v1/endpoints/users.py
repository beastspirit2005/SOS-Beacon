import time
import uuid
from typing import List, Optional
from pydantic import BaseModel
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from passlib.context import CryptContext
from ....database.connection import get_db
from ....database.models import User
from ....services.auth import get_current_user

router = APIRouter()
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

class UserCreate(BaseModel):
    email: str
    password: str
    name: str
    role: str

class UserUpdate(BaseModel):
    email: Optional[str] = None
    name: Optional[str] = None
    role: Optional[str] = None

class UserResponse(BaseModel):
    id: str
    email: str
    name: str
    role: str
    created_at: int
    
    class Config:
        orm_mode = True

def require_admin(current_user: User = Depends(get_current_user)):
    if not current_user or current_user.role != "ADMIN":
        raise HTTPException(status_code=403, detail="Admin privileges required")
    return current_user

@router.get("/", response_model=List[UserResponse])
def get_users(db: Session = Depends(get_db)):
    users = db.query(User).all()
    return users

@router.post("/", response_model=UserResponse)
def create_user(user_in: UserCreate, db: Session = Depends(get_db)):
    existing_user = db.query(User).filter(User.email == user_in.email).first()
    if existing_user:
        raise HTTPException(status_code=400, detail="Email already registered")
        
    hashed_password = pwd_context.hash(user_in.password)
    new_user = User(
        id=str(uuid.uuid4()),
        email=user_in.email,
        name=user_in.name,
        password_hash=hashed_password,
        role=user_in.role,
        created_at=int(time.time() * 1000),
        updated_at=int(time.time() * 1000)
    )
    db.add(new_user)
    db.commit()
    return new_user

@router.put("/{user_id}", response_model=UserResponse)
def update_user(user_id: str, user_in: UserUpdate, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    if user_in.email:
        user.email = user_in.email
    if user_in.name:
        user.name = user_in.name
    if user_in.role:
        user.role = user_in.role
        
    user.updated_at = int(time.time() * 1000)
    db.commit()
    return user

@router.delete("/{user_id}")
def delete_user(user_id: str, db: Session = Depends(get_db)):
        
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    db.delete(user)
    db.commit()
    return {"message": "User deleted successfully"}
