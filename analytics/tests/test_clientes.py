"""Tests para el router de clientes — recurrencia_clientes."""
from unittest.mock import patch

RECURRENCIA_MOCK = [
    {
        "mes": "2026-05",
        "total_ordenes": 10,
        "clientes_recurrentes": 4,
        "clientes_nuevos": 6,
        "porcentaje_recurrentes": 40.0,
    }
]


def test_recurrencia_devuelve_lista(client):
    with patch("app.routers.clientes.svc.recurrencia_clientes", return_value=RECURRENCIA_MOCK):
        response = client.get("/analytics/clientes/recurrencia")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_recurrencia_estructura_item(client):
    with patch("app.routers.clientes.svc.recurrencia_clientes", return_value=RECURRENCIA_MOCK):
        response = client.get("/analytics/clientes/recurrencia")
    data = response.json()
    assert len(data) == 1
    item = data[0]
    for campo in ("mes", "total_ordenes", "clientes_recurrentes", "clientes_nuevos", "porcentaje_recurrentes"):
        assert campo in item, f"Campo faltante: {campo}"


def test_recurrencia_acepta_param_meses(client):
    with patch("app.routers.clientes.svc.recurrencia_clientes", return_value=RECURRENCIA_MOCK):
        response = client.get("/analytics/clientes/recurrencia?meses=3")
    assert response.status_code == 200


def test_recurrencia_meses_invalido(client):
    """meses=0 debe retornar 422 por la validación ge=1."""
    response = client.get("/analytics/clientes/recurrencia?meses=0")
    assert response.status_code == 422


def test_recurrencia_lista_vacia(client):
    """Cuando no hay datos el servicio retorna lista vacía."""
    with patch("app.routers.clientes.svc.recurrencia_clientes", return_value=[]):
        response = client.get("/analytics/clientes/recurrencia")
    assert response.status_code == 200
    assert response.json() == []
