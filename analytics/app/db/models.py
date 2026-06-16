from sqlalchemy import Column, BigInteger, String, DateTime
from sqlalchemy.orm import declarative_base

Base = declarative_base()


class AlertaLeida(Base):
    """Read-only ORM mapping for the alertas_leidas table owned by the Core Service."""
    __tablename__ = "alertas_leidas"

    id = Column(BigInteger, primary_key=True)
    usuario_id = Column(BigInteger, nullable=False)
    alerta_key = Column(String(255), nullable=False)
    leida_en = Column(DateTime, nullable=False)
