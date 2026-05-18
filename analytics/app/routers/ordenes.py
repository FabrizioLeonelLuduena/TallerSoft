"""
Analytics Service - Órdenes Router

Endpoints for retrieving analytics and insights about work orders.
"""

from fastapi import APIRouter

router = APIRouter()


@router.get("/resumen")
async def resumen_ordenes():
    """Get summary of orders by status"""
    # TODO: Implement in Sprint 4
    return {"message": "To be implemented in Sprint 4"}


@router.get("/por-periodo")
async def ordenes_por_periodo():
    """Get orders grouped by week for the last 4 weeks"""
    # TODO: Implement in Sprint 4
    return {"message": "To be implemented in Sprint 4"}


@router.get("/tecnicos/rendimiento")
async def rendimiento_tecnicos():
    """Get technician performance metrics"""
    # TODO: Implement in Sprint 4
    return {"message": "To be implemented in Sprint 4"}
