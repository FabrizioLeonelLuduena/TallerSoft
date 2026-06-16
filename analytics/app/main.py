"""
TallerSoft Analytics Service - Main FastAPI Application

This is the analytics microservice that provides read-only access to the database
for generating reports, KPIs, and powering the AI assistant.

Runs on port 8082.
"""

import uuid
import logging
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware
from app.routers import ordenes, stock, caja, asistente, clientes, alertas
from app.config import settings

logger = logging.getLogger(__name__)

CORRELATION_ID_HEADER = "X-Correlation-ID"


class CorrelationIdMiddleware(BaseHTTPMiddleware):
    """Propaga o genera el header X-Correlation-ID en cada request/response."""

    async def dispatch(self, request: Request, call_next):
        correlation_id = request.headers.get(CORRELATION_ID_HEADER, str(uuid.uuid4()))
        response = await call_next(request)
        response.headers[CORRELATION_ID_HEADER] = correlation_id
        logger.debug("correlation_id=%s path=%s status=%s",
                     correlation_id, request.url.path, response.status_code)
        return response

# Create FastAPI app — el CorrelationIdMiddleware se agrega ANTES del CORS
app = FastAPI(
    title="TallerSoft Analytics API",
    description="Analytics and reporting service for TallerSoft ERP",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)

app.add_middleware(CorrelationIdMiddleware)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins.split(","),
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)

# Include routers
app.include_router(ordenes.router, prefix="/analytics/ordenes", tags=["Órdenes"])
app.include_router(stock.router, prefix="/analytics/stock", tags=["Stock"])
app.include_router(caja.router, prefix="/analytics/caja", tags=["Caja"])
app.include_router(asistente.router, prefix="/analytics/asistente", tags=["Asistente IA"])
app.include_router(clientes.router, prefix="/analytics/clientes", tags=["Clientes"])
app.include_router(alertas.router,  prefix="/analytics/alertas",  tags=["Alertas"])


@app.get("/", tags=["Health"])
async def root():
    """Root endpoint - health check"""
    return {
        "message": "TallerSoft Analytics Service",
        "version": "1.0.0",
        "status": "running"
    }


@app.get("/health", tags=["Health"])
async def health():
    """Health check endpoint for container orchestration"""
    return {"status": "healthy"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8082,
        reload=False,
        log_level="info"
    )
