from sqlalchemy import text
from datetime import date, timedelta
from typing import Literal


# ─── ÓRDENES ────────────────────────────────────────────────────────────────

def resumen_ordenes(db) -> dict:
    result = db.execute(text("""
        SELECT estado, COUNT(*) AS cantidad
        FROM ordenes_trabajo
        GROUP BY estado
    """))
    conteos = {r.estado: r.cantidad for r in result.fetchall()}
    total = sum(conteos.values())
    return {
        "pendientes": int(conteos.get("PENDIENTE", 0)),
        "en_proceso": int(conteos.get("EN_PROCESO", 0)),
        "listas":     int(conteos.get("LISTO", 0)),
        "entregadas": int(conteos.get("ENTREGADO", 0)),
        "total":      int(total),
    }


def ordenes_por_periodo(
    db,
    agrupacion: Literal["semana", "mes"] = "mes",
    meses_atras: int = 6,
) -> list:
    desde = date.today() - timedelta(days=meses_atras * 30)
    if agrupacion == "semana":
        trunc = "week"
        fmt = "IYYY-IW"
    else:
        trunc = "month"
        fmt = "YYYY-MM"

    result = db.execute(text(f"""
        SELECT TO_CHAR(DATE_TRUNC('{trunc}', created_at), '{fmt}') AS periodo,
               COUNT(*) AS cantidad
        FROM ordenes_trabajo
        WHERE created_at >= :desde
        GROUP BY periodo
        ORDER BY periodo
    """), {"desde": desde})
    return [{"periodo": r.periodo, "cantidad": int(r.cantidad)} for r in result.fetchall()]


def rendimiento_tecnicos(db, mes_actual: bool = True) -> list:
    filtro_fecha = ""
    if mes_actual:
        hoy = date.today()
        primer_dia = hoy.replace(day=1)
        filtro_fecha = f"AND ot.updated_at >= '{primer_dia}'"

    result = db.execute(text(f"""
        SELECT
            u.id AS tecnico_id,
            u.nombre,
            COUNT(ot.id) AS ordenes_cerradas,
            ROUND(
                AVG(EXTRACT(EPOCH FROM (ot.updated_at - ot.created_at)) / 86400)::numeric,
                2
            ) AS tiempo_promedio_dias
        FROM ordenes_trabajo ot
        JOIN usuarios u ON u.id = ot.tecnico_id
        WHERE ot.estado = 'ENTREGADO'
        {filtro_fecha}
        GROUP BY u.id, u.nombre
        ORDER BY ordenes_cerradas DESC
    """))
    return [
        {
            "tecnico_id": r.tecnico_id,
            "nombre": r.nombre,
            "ordenes_cerradas": int(r.ordenes_cerradas),
            "tiempo_promedio_dias": float(r.tiempo_promedio_dias or 0),
        }
        for r in result.fetchall()
    ]


# ─── STOCK ──────────────────────────────────────────────────────────────────

def get_stock_critico(db) -> list:
    result = db.execute(text("""
        SELECT
            id,
            nombre,
            categoria,
            stock_actual,
            stock_minimo,
            (stock_actual - stock_minimo) AS diferencia
        FROM repuestos
        WHERE stock_actual <= stock_minimo
        ORDER BY diferencia ASC
    """))
    return [
        {
            "id": r.id,
            "nombre": r.nombre,
            "categoria": r.categoria,
            "stock_actual": r.stock_actual,
            "stock_minimo": r.stock_minimo,
            "diferencia": int(r.diferencia),
        }
        for r in result.fetchall()
    ]


def get_repuestos_mas_usados(db, dias: int = 30, top: int = 10) -> list:
    desde = date.today() - timedelta(days=dias)
    result = db.execute(text("""
        SELECT
            r.id,
            r.nombre,
            r.categoria,
            SUM(ore.cantidad) AS total_usado
        FROM orden_repuestos ore
        JOIN repuestos r ON r.id = ore.repuesto_id
        JOIN ordenes_trabajo ot ON ot.id = ore.orden_id
        WHERE ot.created_at >= :desde
        GROUP BY r.id, r.nombre, r.categoria
        ORDER BY total_usado DESC
        LIMIT :top
    """), {"desde": desde, "top": top})
    return [
        {
            "id": r.id,
            "nombre": r.nombre,
            "categoria": r.categoria,
            "total_usado": int(r.total_usado),
        }
        for r in result.fetchall()
    ]


# ─── CAJA ────────────────────────────────────────────────────────────────────

def resumen_caja_diario(db, fecha: date | None = None) -> dict:
    if fecha is None:
        fecha = date.today()

    result = db.execute(text("""
        SELECT medio_pago, monto
        FROM cobros
        WHERE estado_pago = 'APROBADO'
          AND DATE(created_at) = :fecha
    """), {"fecha": fecha})
    rows = result.fetchall()

    if not rows:
        return {
            "fecha": str(fecha),
            "total_ingresos": 0.0,
            "cantidad_cobros": 0,
            "desglose": [],
        }

    desglose_map: dict = {}
    total = 0.0
    for r in rows:
        mp = r.medio_pago
        if mp not in desglose_map:
            desglose_map[mp] = {"medio_pago": mp, "total": 0.0, "cantidad": 0}
        desglose_map[mp]["total"] += float(r.monto)
        desglose_map[mp]["cantidad"] += 1
        total += float(r.monto)

    return {
        "fecha": str(fecha),
        "total_ingresos": round(total, 2),
        "cantidad_cobros": len(rows),
        "desglose": list(desglose_map.values()),
    }


def evolucion_mensual_caja(db, meses: int = 6) -> list:
    desde = date.today() - timedelta(days=meses * 30)
    result = db.execute(text("""
        SELECT
            TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') AS mes,
            SUM(monto) AS total_ingresos,
            COUNT(*) AS cantidad_cobros
        FROM cobros
        WHERE estado_pago = 'APROBADO'
          AND created_at >= :desde
        GROUP BY mes
        ORDER BY mes
    """), {"desde": desde})
    return [
        {
            "mes": r.mes,
            "total_ingresos": float(r.total_ingresos),
            "cantidad_cobros": int(r.cantidad_cobros),
        }
        for r in result.fetchall()
    ]


# ─── CONTEXTO PARA EL ASISTENTE ──────────────────────────────────────────────

def obtener_contexto_taller(db) -> dict:
    resumen = resumen_ordenes(db)
    criticos = get_stock_critico(db)
    caja_hoy = resumen_caja_diario(db)
    tecnicos = rendimiento_tecnicos(db, mes_actual=True)

    top_tecnico = tecnicos[0]["nombre"] if tecnicos else "N/A"
    nombres_criticos = [r["nombre"] for r in criticos[:5]]

    return {
        "ordenes_pendientes":   resumen["pendientes"],
        "ordenes_en_proceso":   resumen["en_proceso"],
        "ordenes_listas":       resumen["listas"],
        "ordenes_entregadas":   resumen["entregadas"],
        "repuestos_criticos":   nombres_criticos,
        "ingresos_hoy":         caja_hoy["total_ingresos"],
        "top_tecnico":          top_tecnico,
        "tecnicos_rendimiento": tecnicos[:3],
    }
