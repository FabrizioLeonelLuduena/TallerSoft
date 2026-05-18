"""
Analytics Service - Stock Router

Endpoints for retrieving inventory and stock analytics.
"""

from fastapi import APIRouter

router = APIRouter()


@router.get("/critico")
async def stock_critico():
    """Get parts with critical stock levels"""
    # TODO: Implement in Sprint 4
    return {"message": "To be implemented in Sprint 4"}


@router.get("/mas-usados")
async def repuestos_mas_usados(limit: int = 10):
    """Get top most-used parts"""
    # TODO: Implement in Sprint 4
    return {"message": "To be implemented in Sprint 4"}
