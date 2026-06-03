from pydantic import BaseModel
from typing import Optional


class RepuestoCritico(BaseModel):
    id: int
    nombre: str
    categoria: Optional[str] = None
    stock_actual: int
    stock_minimo: int
    diferencia: int


class RepuestoMasUsado(BaseModel):
    id: int
    nombre: str
    categoria: Optional[str] = None
    total_usado: int
