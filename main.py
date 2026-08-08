import os
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from app.api.v1.router import api_router
from app.database.connection import engine, Base
import uuid
import logging

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)

Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="Project Beacon — Cloud Command Node",
    version="2.0.0",
    description="Self-organizing emergency communication infrastructure backend"
)

ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS", "*").split(",")

app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["POST", "GET"],
    allow_headers=["X-Gateway-Id", "X-App-Version", "Content-Type"],
)

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    return JSONResponse(
        status_code=422,
        content={
            "error": {
                "code": "INVALID_PACKET",
                "message": "Schema/field validation failed",
                "request_id": str(uuid.uuid4())
            }
        },
    )

app.include_router(api_router, prefix="/api/v1")
