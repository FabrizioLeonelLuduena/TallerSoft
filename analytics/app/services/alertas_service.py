from datetime import datetime
from app.services import analytics_service as svc

_alertas_leidas: set[str] = set()


def _leida(alerta_id: str) -> bool:
    return alerta_id in _alertas_leidas


def marcar_leida(alerta_id: str) -> None:
    _alertas_leidas.add(alerta_id)


def generar_alertas(db) -> list[dict]:
    alertas: list[dict] = []
    now = datetime.now()

    # 1. Órdenes sin movimiento (≥ 5 días)
    for o in svc.ordenes_sin_movimiento(db, dias_umbral=5):
        alerta_id = f"sin-movimiento-{o['id']}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "danger" if o["dias_estancada"] >= 7 else "warn",
            "titulo":      f"Orden #{o['id']} sin movimiento hace {o['dias_estancada']} días",
            "descripcion": f"{o['equipo_tipo']} — {o['cliente_nombre']}",
            "modulo":      "ordenes",
            "created_at":  now.isoformat(),
            "leida":       _leida(alerta_id),
            "datos_extra": {"orden_id": o["id"]},
        })

    # 2. Alta prioridad paradas (≥ 2 días)
    for o in svc.ordenes_alta_prioridad(db, dias_minimos=2):
        alerta_id = f"alta-prioridad-{o['id']}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "danger",
            "titulo":      f"Orden #{o['id']} — ALTA prioridad sin avanzar",
            "descripcion": f"{o['dias_sin_avanzar']} días parada — {o['cliente_nombre']}",
            "modulo":      "ordenes",
            "created_at":  now.isoformat(),
            "leida":       _leida(alerta_id),
            "datos_extra": {"orden_id": o["id"]},
        })

    # 3. Stock crítico
    for r in svc.get_stock_critico(db):
        alerta_id = f"stock-critico-{r['id']}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "warn",
            "titulo":      f"Stock crítico: {r['nombre']}",
            "descripcion": f"Quedan {r['stock_actual']} unidades (mínimo: {r['stock_minimo']})",
            "modulo":      "stock",
            "created_at":  now.isoformat(),
            "leida":       _leida(alerta_id),
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
            "leida":       _leida(alerta_id),
            "datos_extra": {},
        })

    # 5. Conversión baja (< 60% con al menos 5 órdenes con presupuesto)
    conv = svc.conversion_presupuesto(db)
    if conv["tasa_conversion_pct"] < 60 and conv["total_con_presupuesto"] >= 5:
        alerta_id = f"conversion-baja-{now.strftime('%Y-%m')}"
        alertas.append({
            "id":          alerta_id,
            "tipo":        "warn",
            "titulo":      f"Tasa de conversión baja: {conv['tasa_conversion_pct']}%",
            "descripcion": f"{conv['total_no_cobradas']} presupuestos sin cobrar este mes",
            "modulo":      "caja",
            "created_at":  now.isoformat(),
            "leida":       _leida(alerta_id),
            "datos_extra": {},
        })

    orden_tipo = {"danger": 0, "warn": 1, "info": 2, "success": 3}
    alertas.sort(key=lambda a: orden_tipo.get(a["tipo"], 99))
    return alertas


def resumen_alertas(db) -> dict:
    alertas  = generar_alertas(db)
    sin_leer = [a for a in alertas if not a["leida"]]
    por_tipo: dict = {}
    for a in sin_leer:
        por_tipo[a["tipo"]] = por_tipo.get(a["tipo"], 0) + 1
    return {"total": len(alertas), "sin_leer": len(sin_leer), "por_tipo": por_tipo}
