from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from datetime import date
from typing import Optional
from app.db.database import get_db
from app.services import analytics_service as svc

router = APIRouter()


@router.get("/resumen-diario")
def resumen_caja_diario(
    fecha: Optional[date] = Query(default=None),
    db: Session = Depends(get_db),
):
    """Resumen de ingresos del día (o de la fecha indicada)."""
    return svc.resumen_caja_diario(db, fecha)


@router.get("/evolucion-mensual")
def evolucion_mensual(
    meses: int = Query(6, ge=1, le=24),
    db: Session = Depends(get_db),
):
    """Evolución de ingresos mes a mes."""
    return svc.evolucion_mensual_caja(db, meses)
