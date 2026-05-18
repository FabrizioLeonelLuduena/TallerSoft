"""
Analytics Service - Caja Router

Endpoints for cash and payment analytics.
"""

from fastapi import APIRouter

router = APIRouter()


@router.get("/resumen-diario")
async def resumen_caja_diario():
    """Get daily cash summary"""
    # TODO: Implement in Sprint 4
    return {"message": "To be implemented in Sprint 4"}


@router.get("/evolucion-mensual")
async def evolucion_caja_mensual(meses: int = 12):
    """Get monthly revenue evolution"""
    # TODO: Implement in Sprint 4
    return {"message": "To be implemented in Sprint 4"}
