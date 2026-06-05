from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services import analytics_service as svc

router = APIRouter()


@router.get("/recurrencia")
def recurrencia_clientes(
    meses: int = Query(6, ge=1, le=24),
    db: Session = Depends(get_db),
):
    """Clientes recurrentes vs nuevos por mes."""
    return svc.recurrencia_clientes(db, meses)
