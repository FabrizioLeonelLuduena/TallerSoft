"""Tests para los endpoints de alertas del Analytics Service."""
from unittest.mock import patch
from sqlalchemy import text

# Respuestas mock de las funciones analíticas que usan SQL de PostgreSQL
_NO_ORDENES     = []
_NO_STOCK       = []
_NO_RECHAZOS    = {"cantidad_rechazos": 0, "total_rechazado": 0.0, "cantidad_rechazos": 0, "por_medio": []}
_NO_CONVERSION  = {"tasa_conversion_pct": 80.0, "total_con_presupuesto": 0, "total_no_cobradas": 0}

# Todos los patches necesarios para aislar generar_alertas del SQL real
_PATCHES = {
    "app.services.alertas_service.svc.ordenes_sin_movimiento":  _NO_ORDENES,
    "app.services.alertas_service.svc.ordenes_alta_prioridad":  _NO_ORDENES,
    "app.services.alertas_service.svc.get_stock_critico":       _NO_STOCK,
    "app.services.alertas_service.svc.get_stock_bajo":          _NO_STOCK,
    "app.services.alertas_service.svc.rechazos_cobros":         _NO_RECHAZOS,
    "app.services.alertas_service.svc.conversion_presupuesto":  _NO_CONVERSION,
}


def _apply_patches(fn):
    """Decorador que aplica todos los patches de _PATCHES."""
    import functools
    @functools.wraps(fn)
    def wrapper(*args, **kwargs):
        with patch("app.services.alertas_service.svc.ordenes_sin_movimiento", return_value=_NO_ORDENES), \
             patch("app.services.alertas_service.svc.ordenes_alta_prioridad", return_value=_NO_ORDENES), \
             patch("app.services.alertas_service.svc.get_stock_critico",      return_value=_NO_STOCK), \
             patch("app.services.alertas_service.svc.get_stock_bajo",         return_value=_NO_STOCK), \
             patch("app.services.alertas_service.svc.rechazos_cobros",        return_value=_NO_RECHAZOS), \
             patch("app.services.alertas_service.svc.conversion_presupuesto", return_value=_NO_CONVERSION):
            return fn(*args, **kwargs)
    return wrapper


@_apply_patches
def test_alertas_activas_sin_datos(client):
    """Con BD vacía y sin alarmas, /alertas/activas retorna lista vacía."""
    response = client.get("/analytics/alertas/activas")
    assert response.status_code == 200
    assert response.json() == []


@_apply_patches
def test_alertas_resumen_sin_datos(client):
    """Con BD vacía, /alertas/resumen retorna totales en cero."""
    response = client.get("/analytics/alertas/resumen")
    assert response.status_code == 200
    body = response.json()
    assert "total" in body
    assert "sin_leer" in body
    assert body["total"] == 0
    assert body["sin_leer"] == 0


@_apply_patches
def test_alertas_activas_acepta_usuario_id(client):
    """El query param usuario_id debe ser aceptado sin error."""
    response = client.get("/analytics/alertas/activas?usuario_id=1")
    assert response.status_code == 200


@_apply_patches
def test_alertas_resumen_acepta_usuario_id(client):
    """El query param usuario_id en resumen debe ser aceptado sin error."""
    response = client.get("/analytics/alertas/resumen?usuario_id=1")
    assert response.status_code == 200


def test_marcar_leida_retorna_405(client):
    """El endpoint de escritura debe retornar 405 porque Analytics es read-only."""
    response = client.post("/analytics/alertas/sin-movimiento-1/marcar-leida")
    assert response.status_code == 405
    body = response.json()
    assert "Core Service" in body["detail"]


def test_alertas_con_stock_critico_leida_false(client):
    """Alerta generada sin registro en alertas_leidas debe aparecer leida=False."""
    stock_critico_mock = [{"id": 5, "nombre": "Filtro", "categoria": "F",
                           "stock_actual": 0, "stock_minimo": 3, "stock_bajo": 5, "diferencia": -3}]
    with patch("app.services.alertas_service.svc.ordenes_sin_movimiento", return_value=_NO_ORDENES), \
         patch("app.services.alertas_service.svc.ordenes_alta_prioridad", return_value=_NO_ORDENES), \
         patch("app.services.alertas_service.svc.get_stock_critico",      return_value=stock_critico_mock), \
         patch("app.services.alertas_service.svc.get_stock_bajo",         return_value=_NO_STOCK), \
         patch("app.services.alertas_service.svc.rechazos_cobros",        return_value=_NO_RECHAZOS), \
         patch("app.services.alertas_service.svc.conversion_presupuesto", return_value=_NO_CONVERSION):
        response = client.get("/analytics/alertas/activas?usuario_id=1")

    assert response.status_code == 200
    alertas = response.json()
    alerta = next((a for a in alertas if a["id"] == "stock-critico-5"), None)
    assert alerta is not None
    assert alerta["leida"] is False  # No hay registro en alertas_leidas para usuario 1


def test_alertas_leida_persiste_en_bd(client, db_session):
    """Si una alerta está en alertas_leidas, debe aparecer como leida=True."""
    # Insertar usuario y alerta leída en la BD de test (SQLite soporta esto)
    db_session.execute(text("INSERT OR IGNORE INTO usuarios (id, nombre, rol) VALUES (99, 'Test User', 'ADMIN')"))
    db_session.execute(text(
        "INSERT OR IGNORE INTO alertas_leidas (usuario_id, alerta_key, leida_en) "
        "VALUES (99, 'stock-critico-7', CURRENT_TIMESTAMP)"
    ))
    db_session.commit()

    stock_critico_mock = [{"id": 7, "nombre": "Pastilla freno", "categoria": "Frenos",
                           "stock_actual": 1, "stock_minimo": 5, "stock_bajo": 8, "diferencia": -4}]

    with patch("app.services.alertas_service.svc.ordenes_sin_movimiento", return_value=_NO_ORDENES), \
         patch("app.services.alertas_service.svc.ordenes_alta_prioridad", return_value=_NO_ORDENES), \
         patch("app.services.alertas_service.svc.get_stock_critico",      return_value=stock_critico_mock), \
         patch("app.services.alertas_service.svc.get_stock_bajo",         return_value=_NO_STOCK), \
         patch("app.services.alertas_service.svc.rechazos_cobros",        return_value=_NO_RECHAZOS), \
         patch("app.services.alertas_service.svc.conversion_presupuesto", return_value=_NO_CONVERSION):
        response = client.get("/analytics/alertas/activas?usuario_id=99")

    assert response.status_code == 200
    alertas = response.json()
    alerta = next((a for a in alertas if a["id"] == "stock-critico-7"), None)
    assert alerta is not None, "Debe existir alerta de stock-critico-7"
    assert alerta["leida"] is True, "La alerta debe aparecer como leída (viene de la BD)"
