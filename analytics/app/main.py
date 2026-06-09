"""
TallerSoft Analytics Service - Main FastAPI Application

This is the analytics microservice that provides read-only access to the database
for generating reports, KPIs, and powering the AI assistant.

Runs on port 8082.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import ordenes, stock, caja, asistente, clientes, alertas
from app.config import settings

# Create FastAPI app
app = FastAPI(
    title="TallerSoft Analytics API",
    description="Analytics and reporting service for TallerSoft ERP",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    openapi_url="/openapi.json"
)

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
