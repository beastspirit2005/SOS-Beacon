import os
import httpx
from fastapi import APIRouter
from sqlalchemy.orm import Session
from fastapi import Depends
from sqlalchemy import text
from ....database.connection import get_db

router = APIRouter()

@router.get("/health")
async def health_check(db: Session = Depends(get_db)):
    db_status = "ok"
    notifier_status = "connected"

    # --- Check DB connectivity ---
    try:
        db.execute(text("SELECT 1"))
    except Exception:
        db_status = "db_down"

    # --- Check notifier / Groq connectivity ---
    groq_api_key = os.getenv("GROQ_API_KEY")
    if not groq_api_key:
        notifier_status = "down"
    else:
        try:
            async with httpx.AsyncClient() as client:
                resp = await client.get(
                    "https://api.groq.com/openai/v1/models",
                    headers={"Authorization": f"Bearer {groq_api_key}"},
                    timeout=3.0
                )
            notifier_status = "connected" if resp.status_code == 200 else "down"
        except Exception:
            notifier_status = "down"

    return {
        "status": db_status,
        "notifier": notifier_status,
        "version": os.getenv("APP_VERSION", "2.0.0")
    }
