"""
Analytics Service - Stock Router

Endpoints for retrieving inventory and stock analytics.
"""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services.analytics_service import get_stock_critico, get_repuestos_mas_usados

router = APIRouter()


@router.get("/critico")
def stock_critico(db: Session = Depends(get_db)):
    """
    Returns parts with stock_actual <= stock_minimo, ordered by most critical first
    (lowest stock_actual - stock_minimo difference).
    """
    return get_stock_critico(db)


@router.get("/mas-usados")
def repuestos_mas_usados(
    dias: int = Query(default=30, ge=1, le=365, description="Lookback window in days"),
    top: int = Query(default=10, ge=1, le=100, description="Number of results"),
    db: Session = Depends(get_db),
):
    """
    Returns the top N most-used parts in orders from the last `dias` days.
    """
    return get_repuestos_mas_usados(db, dias=dias, top=top)
