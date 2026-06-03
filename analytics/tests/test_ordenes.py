from unittest.mock import patch
from sqlalchemy import text

RESUMEN_MOCK = {
    "pendientes": 5, "en_proceso": 3, "listas": 2, "entregadas": 10, "total": 20
}
PERIODO_MOCK = [{"periodo": "2025-06", "cantidad": 8}]
TECNICOS_MOCK = [
    {"tecnico_id": 1, "nombre": "Carlos", "ordenes_cerradas": 5, "tiempo_promedio_dias": 2.5}
]


# ─── /resumen ────────────────────────────────────────────────────────────────

def test_resumen_ordenes_estructura_correcta(client):
    with patch("app.routers.ordenes.svc.resumen_ordenes", return_value=RESUMEN_MOCK):
        response = client.get("/analytics/ordenes/resumen")
    assert response.status_code == 200
    data = response.json()
    for campo in ("pendientes", "en_proceso", "listas", "entregadas", "total"):
        assert campo in data


def test_resumen_ordenes_valores_numericos(client):
    with patch("app.routers.ordenes.svc.resumen_ordenes", return_value=RESUMEN_MOCK):
        response = client.get("/analytics/ordenes/resumen")
    data = response.json()
    for campo in ("pendientes", "en_proceso", "listas", "entregadas", "total"):
        assert isinstance(data[campo], int)


def test_resumen_ordenes_con_datos_reales(client, db_session):
    """Verifica el conteo real con datos en SQLite (SQL compatible)."""
    db_session.execute(text("DELETE FROM ordenes_trabajo"))
    db_session.execute(text("""
        INSERT INTO ordenes_trabajo (id, estado) VALUES
            (1, 'PENDIENTE'), (2, 'PENDIENTE'),
            (3, 'EN_PROCESO'),
            (4, 'ENTREGADO')
    """))
    db_session.commit()

    response = client.get("/analytics/ordenes/resumen")
    assert response.status_code == 200
    data = response.json()
    assert data["pendientes"] == 2
    assert data["en_proceso"] == 1
    assert data["entregadas"] == 1
    assert data["total"] == 4


# ─── /por-periodo ────────────────────────────────────────────────────────────

def test_ordenes_por_periodo_devuelve_lista(client):
    with patch("app.routers.ordenes.svc.ordenes_por_periodo", return_value=PERIODO_MOCK):
        response = client.get("/analytics/ordenes/por-periodo")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_ordenes_por_periodo_acepta_parametros(client):
    with patch("app.routers.ordenes.svc.ordenes_por_periodo", return_value=PERIODO_MOCK):
        response = client.get("/analytics/ordenes/por-periodo?agrupacion=semana&meses_atras=3")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_ordenes_por_periodo_agrupacion_invalida(client):
    response = client.get("/analytics/ordenes/por-periodo?agrupacion=anio")
    assert response.status_code == 422


def test_ordenes_por_periodo_meses_atras_fuera_de_rango(client):
    response = client.get("/analytics/ordenes/por-periodo?meses_atras=0")
    assert response.status_code == 422
    response = client.get("/analytics/ordenes/por-periodo?meses_atras=25")
    assert response.status_code == 422


def test_ordenes_por_periodo_estructura_item(client):
    with patch("app.routers.ordenes.svc.ordenes_por_periodo", return_value=PERIODO_MOCK):
        response = client.get("/analytics/ordenes/por-periodo")
    data = response.json()
    assert len(data) >= 1
    assert "periodo" in data[0]
    assert "cantidad" in data[0]


# ─── /tecnicos/rendimiento ────────────────────────────────────────────────────

def test_rendimiento_tecnicos_devuelve_lista(client):
    with patch("app.routers.ordenes.svc.rendimiento_tecnicos", return_value=TECNICOS_MOCK):
        response = client.get("/analytics/ordenes/tecnicos/rendimiento")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_rendimiento_tecnicos_acepta_mes_actual_false(client):
    with patch("app.routers.ordenes.svc.rendimiento_tecnicos", return_value=TECNICOS_MOCK):
        response = client.get("/analytics/ordenes/tecnicos/rendimiento?mes_actual=false")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_rendimiento_tecnicos_estructura_item(client):
    with patch("app.routers.ordenes.svc.rendimiento_tecnicos", return_value=TECNICOS_MOCK):
        response = client.get("/analytics/ordenes/tecnicos/rendimiento")
    data = response.json()
    assert len(data) >= 1
    tecnico = data[0]
    for campo in ("tecnico_id", "nombre", "ordenes_cerradas", "tiempo_promedio_dias"):
        assert campo in tecnico
