from datetime import datetime
from sqlalchemy.orm import Session
from app.services import analytics_service as svc
from app.services.analytics_service import (
    UMBRAL_DIAS_SIN_MOVIMIENTO,
    UMBRAL_DIAS_ALTA_PRIORIDAD,
)
from app.db.models import AlertaLeida

_UMBRAL_CONVERSION_PCT = 60
_MIN_ORDENES_CON_PRESUPUESTO = 5


def _esta_leida(db: Session, alerta_key: str, usuario_id: int | None) -> bool:
    """Consulta la BD para saber si el usuario ya marcó esta alerta como leída."""
    if usuario_id is None:
        return False
    return (
        db.query(AlertaLeida)
        .filter(
            AlertaLeida.usuario_id == usuario_id,
            AlertaLeida.alerta_key == alerta_key,
        )
        .first()
        is not None
    )


def generar_alertas(db: Session, usuario_id: int | None = None) -> list[dict]:
    alertas: list[dict] = []
    now = datetime.now()

    # 1. Órdenes sin movimiento
    for o in svc.ordenes_sin_movimiento(db, dias_umbral=UMBRAL_DIAS_SIN_MOVIMIENTO):
        alerta_id = f"sin-movimiento-{o['id']}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "danger" if o["dias_estancada"] >= 7 else "warn",
            "titulo":      f"Orden #{o['id']} sin movimiento hace {o['dias_estancada']} días",
            "descripcion": f"{o['equipo_tipo']} — {o['cliente_nombre']}",
            "modulo":      "ordenes",
            "created_at":  now.isoformat(),
            "leida":       _esta_leida(db, alerta_id, usuario_id),
            "datos_extra": {"orden_id": o["id"]},
        })

    # 2. Alta prioridad paradas
    for o in svc.ordenes_alta_prioridad(db, dias_minimos=UMBRAL_DIAS_ALTA_PRIORIDAD):
        alerta_id = f"alta-prioridad-{o['id']}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "danger",
            "titulo":      f"Orden #{o['id']} — ALTA prioridad sin avanzar",
            "descripcion": f"{o['dias_sin_avanzar']} días parada — {o['cliente_nombre']}",
            "modulo":      "ordenes",
            "created_at":  now.isoformat(),
            "leida":       _esta_leida(db, alerta_id, usuario_id),
            "datos_extra": {"orden_id": o["id"]},
        })

    # 3. Stock crítico (stockActual <= stockMinimo)
    for r in svc.get_stock_critico(db):
        alerta_id = f"stock-critico-{r['id']}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "danger",
            "titulo":      f"Stock crítico: {r['nombre']}",
            "descripcion": f"Quedan {r['stock_actual']} unidades (mínimo: {r['stock_minimo']})",
            "modulo":      "stock",
            "created_at":  now.isoformat(),
            "leida":       _esta_leida(db, alerta_id, usuario_id),
            "datos_extra": {"repuesto_id": r["id"]},
        })

    # 3b. Stock bajo (stockMinimo < stockActual <= stockBajo)
    for r in svc.get_stock_bajo(db):
        alerta_id = f"stock-bajo-{r['id']}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "warn",
            "titulo":      f"Stock bajo: {r['nombre']}",
            "descripcion": f"Quedan {r['stock_actual']} unidades (umbral de alerta: {r['stock_bajo']})",
            "modulo":      "stock",
            "created_at":  now.isoformat(),
            "leida":       _esta_leida(db, alerta_id, usuario_id),
            "datos_extra": {"repuesto_id": r["id"]},
        })

    # 4. Cobros rechazados del día
    rechazos = svc.rechazos_cobros(db, dias=1)
    if rechazos["cantidad_rechazos"] > 0:
        alerta_id = f"rechazos-hoy-{now.strftime('%Y-%m-%d')}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "danger",
            "titulo":      f"{rechazos['cantidad_rechazos']} cobro(s) rechazado(s) hoy",
            "descripcion": f"Total: ${rechazos['total_rechazado']:,.0f} — Revisar en Caja",
            "modulo":      "caja",
            "created_at":  now.isoformat(),
            "leida":       _esta_leida(db, alerta_id, usuario_id),
            "datos_extra": {},
        })

    # 5. Conversión baja
    conv = svc.conversion_presupuesto(db)
    if conv["tasa_conversion_pct"] < _UMBRAL_CONVERSION_PCT and conv["total_con_presupuesto"] >= _MIN_ORDENES_CON_PRESUPUESTO:
        alerta_id = f"conversion-baja-{now.strftime('%Y-%m')}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "warn",
            "titulo":      f"Tasa de conversión baja: {conv['tasa_conversion_pct']}%",
            "descripcion": f"{conv['total_no_cobradas']} presupuestos sin cobrar este mes",
            "modulo":      "caja",
            "created_at":  now.isoformat(),
            "leida":       _esta_leida(db, alerta_id, usuario_id),
            "datos_extra": {},
        })

    orden_tipo = {"danger": 0, "warn": 1, "info": 2, "success": 3}
    alertas.sort(key=lambda a: orden_tipo.get(a["tipo"], 99))
    return alertas


def resumen_alertas(db: Session, usuario_id: int | None = None) -> dict:
    alertas  = generar_alertas(db, usuario_id)
    sin_leer = [a for a in alertas if not a["leida"]]
    por_tipo: dict = {}
    for a in sin_leer:
        por_tipo[a["tipo"]] = por_tipo.get(a["tipo"], 0) + 1
    return {"total": len(alertas), "sin_leer": len(sin_leer), "por_tipo": por_tipo}
