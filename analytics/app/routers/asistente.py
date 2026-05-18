"""
Analytics Service - Asistente Router

Endpoints for AI Assistant queries.
"""

from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class ConsultaRequest(BaseModel):
    """Request model for assistant query"""
    pregunta: str


@router.post("/consulta")
async def consultar_asistente(request: ConsultaRequest):
    """Send a question to the AI assistant"""
    # TODO: Implement in Sprint 5
    return {
        "respuesta": "The AI assistant will be implemented in Sprint 5"
    }
