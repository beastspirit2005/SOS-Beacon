from fastapi import APIRouter
from .endpoints import ingest, status, health, ws_dashboard

api_router = APIRouter()
api_router.include_router(ingest.router, prefix="/sos", tags=["ingest"])
api_router.include_router(status.router, prefix="/sos", tags=["status"])
api_router.include_router(health.router, tags=["health"])
api_router.include_router(ws_dashboard.router, tags=["dashboard"])
