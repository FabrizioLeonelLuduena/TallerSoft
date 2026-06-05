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


@router.get("/alta-prioridad")
def ordenes_alta_prioridad(
    dias_minimos: int = Query(1, ge=0),
    db: Session = Depends(get_db),
):
    """Órdenes ALTA prioridad que llevan N+ días sin avanzar."""
    return svc.ordenes_alta_prioridad(db, dias_minimos)


@router.get("/sin-movimiento")
def ordenes_sin_movimiento(
    dias_umbral: int = Query(5, ge=1),
    db: Session = Depends(get_db),
):
    """Órdenes activas sin cambios en más de N días."""
    return svc.ordenes_sin_movimiento(db, dias_umbral)


@router.get("/tiempo-por-estado")
def tiempo_por_estado(db: Session = Depends(get_db)):
    """Tiempo promedio en días por estado (embudo de reparación)."""
    return svc.tiempo_promedio_por_estado(db)
