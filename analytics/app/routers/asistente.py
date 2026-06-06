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
    except Exception:
        raise HTTPException(status_code=500, detail="Error al consultar el asistente: no se pudo obtener el contexto del taller")

    try:
        respuesta = consultar_asistente(request.pregunta, contexto)
        return ConsultaResponse(respuesta=respuesta, contexto_utilizado=contexto)
    except Exception:
        # Respuesta amigable cuando la API de IA no está disponible
        mensaje_fallback = (
            "Lo siento, el asistente no está disponible en este momento. "
            "Intentá de nuevo en unos segundos."
        )
        return ConsultaResponse(respuesta=mensaje_fallback, contexto_utilizado=contexto)
