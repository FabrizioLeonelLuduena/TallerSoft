from fastapi import APIRouter, Depends, Query
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services.alertas_service import generar_alertas, resumen_alertas

router = APIRouter()


@router.get("/activas")
def alertas_activas(
    usuario_id: int | None = Query(None, description="ID del usuario para filtrar alertas leídas"),
    db: Session = Depends(get_db),
):
    """Lista completa de alertas vigentes ordenadas por severidad."""
    return generar_alertas(db, usuario_id)


@router.get("/resumen")
def alertas_resumen(
    usuario_id: int | None = Query(None, description="ID del usuario para filtrar alertas leídas"),
    db: Session = Depends(get_db),
):
    """Conteo rápido para el badge de la campana en el topbar."""
    return resumen_alertas(db, usuario_id)


@router.post("/{alerta_id}/marcar-leida")
def marcar_alerta_leida(alerta_id: str):
    """
    El Analytics Service es de solo lectura y no puede persistir este estado.
    Usar POST /api/alertas/{alerta_key}/leer en el Core Service (puerto 8081 / Gateway 8080).
    """
    return JSONResponse(
        status_code=405,
        content={
            "detail": (
                "El Analytics Service no puede escribir datos. "
                "Para marcar una alerta como leída usá "
                "POST /api/alertas/{alerta_key}/leer en el Core Service."
            )
        },
    )
