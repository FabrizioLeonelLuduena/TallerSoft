from pydantic import BaseModel
from typing import List


class ResumenOrdenes(BaseModel):
    pendientes: int
    en_proceso: int
    listas: int
    entregadas: int
    total: int


class OrdenesPorPeriodo(BaseModel):
    periodo: str
    cantidad: int


class RendimientoTecnico(BaseModel):
    tecnico_id: int
    nombre: str
    ordenes_cerradas: int
    tiempo_promedio_dias: float
