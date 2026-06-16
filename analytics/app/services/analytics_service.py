from sqlalchemy import text
from datetime import date, timedelta
from typing import Literal

_DIAS_POR_MES = 30
UMBRAL_DIAS_SIN_MOVIMIENTO = 5
UMBRAL_DIAS_ALTA_PRIORIDAD = 2


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
    desde = date.today() - timedelta(days=meses_atras * _DIAS_POR_MES)
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
            stock_bajo,
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
            "stock_bajo": r.stock_bajo,
            "diferencia": int(r.diferencia),
        }
        for r in result.fetchall()
    ]


def get_stock_bajo(db) -> list:
    result = db.execute(text("""
        SELECT
            id,
            nombre,
            categoria,
            stock_actual,
            stock_minimo,
            stock_bajo,
            (stock_actual - stock_bajo) AS diferencia
        FROM repuestos
        WHERE stock_actual > stock_minimo AND stock_actual <= stock_bajo
        ORDER BY diferencia ASC
    """))
    return [
        {
            "id": r.id,
            "nombre": r.nombre,
            "categoria": r.categoria,
            "stock_actual": r.stock_actual,
            "stock_minimo": r.stock_minimo,
            "stock_bajo": r.stock_bajo,
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
    desde = date.today() - timedelta(days=meses * _DIAS_POR_MES)
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


# ─── NUEVOS KPIs ─────────────────────────────────────────────────────────────

def ordenes_alta_prioridad(db, dias_minimos: int = 1) -> list:
    result = db.execute(text("""
        SELECT
            ot.id,
            c.nombre  AS cliente_nombre,
            e.tipo    AS equipo_tipo,
            e.marca   AS equipo_marca,
            u.nombre  AS tecnico_nombre,
            ot.estado,
            EXTRACT(DAY FROM NOW() - ot.updated_at)::int AS dias_sin_avanzar
        FROM ordenes_trabajo ot
        JOIN clientes  c ON c.id = ot.cliente_id
        JOIN equipos   e ON e.id = ot.equipo_id
        LEFT JOIN usuarios u ON u.id = ot.tecnico_id
        WHERE ot.prioridad = 'ALTA'
          AND ot.estado != 'ENTREGADO'
          AND EXTRACT(DAY FROM NOW() - ot.updated_at) >= :dias_minimos
        ORDER BY dias_sin_avanzar DESC
    """), {"dias_minimos": dias_minimos})
    return [
        {
            "id": r.id,
            "cliente_nombre": r.cliente_nombre,
            "equipo_tipo": r.equipo_tipo,
            "equipo_marca": r.equipo_marca,
            "tecnico_nombre": r.tecnico_nombre,
            "estado": r.estado,
            "dias_sin_avanzar": int(r.dias_sin_avanzar or 0),
        }
        for r in result.fetchall()
    ]


def ordenes_sin_movimiento(db, dias_umbral: int = 5) -> list:
    result = db.execute(text("""
        SELECT
            ot.id,
            c.nombre  AS cliente_nombre,
            e.tipo    AS equipo_tipo,
            ot.estado,
            ot.prioridad,
            EXTRACT(DAY FROM NOW() - ot.updated_at)::int AS dias_estancada
        FROM ordenes_trabajo ot
        JOIN clientes c ON c.id = ot.cliente_id
        JOIN equipos  e ON e.id = ot.equipo_id
        WHERE ot.estado != 'ENTREGADO'
          AND EXTRACT(DAY FROM NOW() - ot.updated_at) >= :dias_umbral
        ORDER BY dias_estancada DESC
    """), {"dias_umbral": dias_umbral})
    return [
        {
            "id": r.id,
            "cliente_nombre": r.cliente_nombre,
            "equipo_tipo": r.equipo_tipo,
            "estado": r.estado,
            "prioridad": r.prioridad,
            "dias_estancada": int(r.dias_estancada or 0),
        }
        for r in result.fetchall()
    ]


def tiempo_promedio_por_estado(db) -> list:
    result = db.execute(text("""
        SELECT 'ENTREGADO' AS estado,
               AVG(EXTRACT(DAY FROM updated_at - created_at)) AS promedio_dias_acum
        FROM ordenes_trabajo WHERE estado = 'ENTREGADO'

        UNION ALL

        SELECT 'PENDIENTE_actual' AS estado,
               AVG(EXTRACT(DAY FROM NOW() - created_at)) AS promedio_dias_acum
        FROM ordenes_trabajo WHERE estado = 'PENDIENTE'

        UNION ALL

        SELECT 'EN_PROCESO_actual' AS estado,
               AVG(EXTRACT(DAY FROM NOW() - updated_at)) AS promedio_dias_acum
        FROM ordenes_trabajo WHERE estado = 'EN_PROCESO'

        UNION ALL

        SELECT 'LISTO_actual' AS estado,
               AVG(EXTRACT(DAY FROM NOW() - updated_at)) AS promedio_dias_acum
        FROM ordenes_trabajo WHERE estado = 'LISTO'
    """))
    rows = [(r.estado, float(r.promedio_dias_acum or 0)) for r in result.fetchall()]
    total = sum(v for _, v in rows)
    if total == 0:
        return []
    return [
        {
            "estado": estado,
            "promedio_dias": round(dias, 1),
            "porcentaje_del_total": round(dias / total * 100, 1),
        }
        for estado, dias in rows
    ]


def rechazos_cobros(db, dias: int = 7) -> dict:
    desde = date.today() - timedelta(days=dias)
    result = db.execute(text("""
        SELECT medio_pago, monto
        FROM cobros
        WHERE estado_pago = 'RECHAZADO'
          AND created_at >= :desde
    """), {"desde": desde})
    rows = result.fetchall()

    if not rows:
        return {"periodo_dias": dias, "total_rechazado": 0.0, "cantidad_rechazos": 0, "por_medio": []}

    por_medio: dict = {}
    total = 0.0
    for r in rows:
        mp = r.medio_pago
        if mp not in por_medio:
            por_medio[mp] = {"medio_pago": mp, "cantidad_rechazos": 0, "monto_total_rechazado": 0.0}
        por_medio[mp]["cantidad_rechazos"] += 1
        por_medio[mp]["monto_total_rechazado"] += float(r.monto)
        total += float(r.monto)

    return {
        "periodo_dias": dias,
        "total_rechazado": round(total, 2),
        "cantidad_rechazos": len(rows),
        "por_medio": list(por_medio.values()),
    }


def conversion_presupuesto(db) -> dict:
    result = db.execute(text("""
        SELECT
            COUNT(*) AS total_con_presupuesto,
            COUNT(DISTINCT c.orden_id) AS total_cobradas
        FROM ordenes_trabajo ot
        LEFT JOIN cobros c ON c.orden_id = ot.id AND c.estado_pago = 'APROBADO'
        WHERE ot.presupuesto IS NOT NULL
    """))
    r = result.fetchone()
    total    = int(r.total_con_presupuesto)
    cobradas = int(r.total_cobradas)
    tasa     = round(cobradas / total * 100, 1) if total > 0 else 0.0
    return {
        "total_con_presupuesto": total,
        "total_cobradas":        cobradas,
        "total_no_cobradas":     total - cobradas,
        "tasa_conversion_pct":   tasa,
    }


def recurrencia_clientes(db, meses: int = 6) -> list:
    desde = date.today() - timedelta(days=meses * _DIAS_POR_MES)
    # Refactorizado de subquery correlacionada O(N²) a window function O(N log N).
    # La versión anterior ejecutaba una subquery por cada fila del resultado.
    # Con COUNT(*) OVER (PARTITION BY ... ORDER BY ... ROWS UNBOUNDED PRECEDING),
    # PostgreSQL hace un único scan con agregación incremental por cliente.
    result = db.execute(text("""
        WITH ordenes_con_historial AS (
            SELECT
                created_at,
                cliente_id,
                COUNT(*) OVER (
                    PARTITION BY cliente_id
                    ORDER BY created_at
                    ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                ) AS ordenes_previas
            FROM ordenes_trabajo
            WHERE created_at >= :desde
        )
        SELECT
            TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') AS mes,
            COUNT(*) AS total_ordenes,
            SUM(CASE WHEN ordenes_previas > 0 THEN 1 ELSE 0 END) AS clientes_recurrentes
        FROM ordenes_con_historial
        GROUP BY DATE_TRUNC('month', created_at)
        ORDER BY mes
    """), {"desde": desde})
    resultado = []
    for r in result.fetchall():
        total      = int(r.total_ordenes)
        recurrentes = int(r.clientes_recurrentes)
        pct        = round(recurrentes / total * 100, 1) if total > 0 else 0.0
        resultado.append({
            "mes": r.mes,
            "total_ordenes": total,
            "clientes_recurrentes": recurrentes,
            "clientes_nuevos": total - recurrentes,
            "porcentaje_recurrentes": pct,
        })
    return resultado


def ingresos_rango(db, desde: date, hasta: date) -> float:
    result = db.execute(text("""
        SELECT COALESCE(SUM(monto), 0) AS total
        FROM cobros
        WHERE estado_pago = 'APROBADO'
          AND DATE(created_at) BETWEEN :desde AND :hasta
    """), {"desde": desde, "hasta": hasta})
    return float(result.scalar())


def stock_por_categoria(db) -> list:
    result = db.execute(text("""
        SELECT
            COALESCE(r.categoria, 'Sin categoría') AS categoria,
            SUM(ore.cantidad) AS total_unidades,
            COUNT(DISTINCT ore.orden_id) AS ordenes_count,
            COUNT(DISTINCT r.id) AS tipos_repuesto
        FROM orden_repuestos ore
        JOIN repuestos r ON r.id = ore.repuesto_id
        GROUP BY r.categoria
        ORDER BY total_unidades DESC
    """))
    return [
        {
            "categoria": r.categoria,
            "total_unidades": int(r.total_unidades),
            "ordenes_count": int(r.ordenes_count),
            "tipos_repuesto": int(r.tipos_repuesto),
        }
        for r in result.fetchall()
    ]


# aliases para compatibilidad con alertas_service
stock_critico = get_stock_critico
stock_bajo = get_stock_bajo


# ─── CONTEXTO PARA EL ASISTENTE ──────────────────────────────────────────────

def obtener_contexto_taller(db) -> dict:
    hoy    = date.today()
    ayer   = hoy - timedelta(days=1)
    inicio_semana = hoy - timedelta(days=6)
    inicio_mes    = hoy.replace(day=1)

    resumen      = resumen_ordenes(db)
    criticos     = get_stock_critico(db)
    caja_hoy     = resumen_caja_diario(db)
    caja_ayer    = resumen_caja_diario(db, ayer)
    tecnicos     = rendimiento_tecnicos(db, mes_actual=True)
    alta_prio    = ordenes_alta_prioridad(db, dias_minimos=UMBRAL_DIAS_ALTA_PRIORIDAD)
    sin_mov      = ordenes_sin_movimiento(db, dias_umbral=UMBRAL_DIAS_SIN_MOVIMIENTO)
    conv         = conversion_presupuesto(db)
    rechazos_hoy = rechazos_cobros(db, dias=1)
    recurrencia  = recurrencia_clientes(db, meses=1)

    ingresos_7d    = ingresos_rango(db, inicio_semana, hoy)
    ingresos_mes   = ingresos_rango(db, inicio_mes, hoy)
    evolucion_6m   = evolucion_mensual_caja(db, meses=6)
    tendencia_6m   = ordenes_por_periodo(db, agrupacion="mes", meses_atras=6)

    top_tecnico      = tecnicos[0]["nombre"] if tecnicos else "N/A"
    nombres_criticos = [r["nombre"] for r in criticos[:5]]
    recurrencia_mes  = recurrencia[0] if recurrencia else {}

    return {
        # snapshot actual
        "ordenes_pendientes":             resumen["pendientes"],
        "ordenes_en_proceso":             resumen["en_proceso"],
        "ordenes_listas":                 resumen["listas"],
        "ordenes_entregadas":             resumen["entregadas"],
        "repuestos_criticos":             nombres_criticos,
        "ingresos_hoy":                   caja_hoy["total_ingresos"],
        "top_tecnico":                    top_tecnico,
        "tecnicos_rendimiento":           tecnicos[:3],
        "ordenes_alta_prioridad_paradas": len(alta_prio),
        "ordenes_sin_movimiento":         len(sin_mov),
        "conversion_presupuesto_pct":     conv["tasa_conversion_pct"],
        "rechazos_hoy_monto":             rechazos_hoy["total_rechazado"],
        "clientes_recurrentes_pct":       recurrencia_mes.get("porcentaje_recurrentes", 0),
        # datos históricos
        "fecha_hoy":                      str(hoy),
        "ingresos_ayer":                  caja_ayer["total_ingresos"],
        "fecha_ayer":                     str(ayer),
        "ingresos_ultimos_7_dias":        round(ingresos_7d, 2),
        "ingresos_mes_actual":            round(ingresos_mes, 2),
        "evolucion_caja_6_meses":         evolucion_6m,
        "tendencia_ordenes_6_meses":      tendencia_6m,
    }
