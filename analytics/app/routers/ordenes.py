from fastapi import APIRouter, Depends, Query
from typing import Literal
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services import analytics_service as svc

router = APIRouter()


@router.get("/resumen")
def resumen_ordenes(db: Session = Depends(get_db)):
    """Totales de órdenes por estado."""
    return svc.resumen_ordenes(db)


@router.get("/por-periodo")
def ordenes_por_periodo(
    agrupacion: Literal["semana", "mes"] = Query("mes"),
    meses_atras: int = Query(6, ge=1, le=24),
    db: Session = Depends(get_db),
):
    """Órdenes agrupadas por semana o mes en los últimos N meses."""
    return svc.ordenes_por_periodo(db, agrupacion, meses_atras)


@router.get("/tecnicos/rendimiento")
def rendimiento_tecnicos(
    mes_actual: bool = Query(True),
    db: Session = Depends(get_db),
):
    """Rendimiento de técnicos: órdenes cerradas y tiempo promedio."""
    return svc.rendimiento_tecnicos(db, mes_actual)
