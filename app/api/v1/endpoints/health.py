from fastapi import APIRouter
from sqlalchemy.orm import Session
from fastapi import Depends
from sqlalchemy import text
from ....database.connection import get_db

router = APIRouter()

@router.get("/health")
def health_check(db: Session = Depends(get_db)):
    status = "ok"
    notifier = "connected"
    try:
        db.execute(text("SELECT 1"))
    except Exception:
        status = "db_down"
        
    return {
        "status": status,
        "notifier": notifier,
        "version": "2.0.0"
    }
