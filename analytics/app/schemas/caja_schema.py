from pydantic import BaseModel
from typing import List
from datetime import date


class DesgloseMedioPago(BaseModel):
    medio_pago: str
    total: float
    cantidad: int


class ResumenDiario(BaseModel):
    fecha: str
    total_ingresos: float
    cantidad_cobros: int
    desglose: List[DesgloseMedioPago]


class EvolucionMensual(BaseModel):
    mes: str
    total_ingresos: float
    cantidad_cobros: int
