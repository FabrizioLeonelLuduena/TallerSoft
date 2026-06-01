"""
Analytics Service - Analytics Service Module

Business logic for generating analytics and reports.
"""

from sqlalchemy import text


def get_stock_critico(db):
    """
    Returns parts where stock_actual <= stock_minimo, ordered by most critical first.
    """
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
        ORDER BY (stock_actual - stock_minimo) ASC
    """))
    rows = result.fetchall()
    return [
        {
            "id": r.id,
            "nombre": r.nombre,
            "categoria": r.categoria,
            "stock_actual": r.stock_actual,
            "stock_minimo": r.stock_minimo,
            "diferencia": r.diferencia,
        }
        for r in rows
    ]


def get_repuestos_mas_usados(db, dias: int = 30, top: int = 10):
    """
    Returns the top N most-used parts in orders from the last `dias` days.
    """
    result = db.execute(text("""
        SELECT
            r.id,
            r.nombre,
            r.categoria,
            SUM(or_.cantidad) AS total_usado
        FROM orden_repuestos or_
        JOIN repuestos r ON r.id = or_.repuesto_id
        JOIN ordenes_trabajo ot ON ot.id = or_.orden_id
        WHERE ot.created_at >= NOW() - (INTERVAL '1 day' * :dias)
        GROUP BY r.id, r.nombre, r.categoria
        ORDER BY total_usado DESC
        LIMIT :top
    """), {"dias": dias, "top": top})
    rows = result.fetchall()
    return [
        {
            "id": r.id,
            "nombre": r.nombre,
            "categoria": r.categoria,
            "total_usado": r.total_usado,
        }
        for r in rows
    ]


def obtener_contexto_taller(db):
    """
    Obtain real-time workshop context for AI assistant.
    """
    ordenes_result = db.execute(text("""
        SELECT estado, COUNT(*) AS total
        FROM ordenes_trabajo
        GROUP BY estado
    """))
    ordenes_por_estado = {r.estado: r.total for r in ordenes_result.fetchall()}

    repuestos_criticos = get_stock_critico(db)

    return {
        "ordenes_pendientes": ordenes_por_estado.get("PENDIENTE", 0),
        "ordenes_en_proceso": ordenes_por_estado.get("EN_PROCESO", 0),
        "ordenes_listas": ordenes_por_estado.get("LISTO", 0),
        "repuestos_criticos": repuestos_criticos,
        "ingresos_hoy": 0.0,
        "top_tecnico": None,
    }
