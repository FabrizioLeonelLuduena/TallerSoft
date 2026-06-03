from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services.groq_service import consultar_asistente
from app.services.analytics_service import obtener_contexto_taller
from app.schemas.asistente_schema import ConsultaRequest, ConsultaResponse

router = APIRouter()


@router.post("/consulta", response_model=ConsultaResponse)
def chat_asistente(request: ConsultaRequest, db: Session = Depends(get_db)):
    """
    Recibe una pregunta en lenguaje natural, enriquece el prompt con datos
    reales del taller y devuelve la respuesta del asistente IA.
    """
    try:
        contexto = obtener_contexto_taller(db)
        respuesta = consultar_asistente(request.pregunta, contexto)
        return ConsultaResponse(respuesta=respuesta, contexto_utilizado=contexto)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al consultar el asistente: {str(e)}")
