from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services.alertas_service import generar_alertas, resumen_alertas, marcar_leida

router = APIRouter()


@router.get("/activas")
def alertas_activas(db: Session = Depends(get_db)):
    """Lista completa de alertas vigentes ordenadas por severidad."""
    return generar_alertas(db)


@router.get("/resumen")
def alertas_resumen(db: Session = Depends(get_db)):
    """Conteo rápido para el badge de la campana en el topbar."""
    return resumen_alertas(db)


@router.post("/{alerta_id}/marcar-leida")
def marcar_alerta_leida(alerta_id: str):
    """Marca una alerta como leída (persiste en memoria mientras el servicio esté corriendo)."""
    marcar_leida(alerta_id)
    return {"ok": True, "alerta_id": alerta_id}
