from fastapi import APIRouter
from .endpoints import auth, gateway, victim, officer, admin, status, health, ws_dashboard, users

api_router = APIRouter()

api_router.include_router(auth.router, prefix="/auth", tags=["auth"])
# Admin User Management
api_router.include_router(users.router, prefix="/users", tags=["users"])
# SOS packet ingestion + deduplication (gateway handles ingest)
api_router.include_router(gateway.router, prefix="/sos", tags=["gateway"])
api_router.include_router(victim.router, prefix="/victim", tags=["victim"])
api_router.include_router(officer.router, prefix="/officer", tags=["officer"])
api_router.include_router(admin.router, prefix="/admin", tags=["admin"])
api_router.include_router(status.router, prefix="/sos", tags=["status"])
api_router.include_router(health.router, tags=["system"])
