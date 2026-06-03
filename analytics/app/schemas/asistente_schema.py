from pydantic import BaseModel, Field


class ConsultaRequest(BaseModel):
    pregunta: str = Field(..., min_length=3, max_length=500)


class ConsultaResponse(BaseModel):
    respuesta: str
    contexto_utilizado: dict
