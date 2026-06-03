from unittest.mock import patch
from sqlalchemy import text
from datetime import date

EVOLUCION_MOCK = [
    {"mes": "2025-05", "total_ingresos": 35000.0, "cantidad_cobros": 10},
    {"mes": "2025-06", "total_ingresos": 45000.0, "cantidad_cobros": 12},
]


# ─── /resumen-diario ─────────────────────────────────────────────────────────

def test_resumen_caja_diario_estructura(client):
    response = client.get("/analytics/caja/resumen-diario")
    assert response.status_code == 200
    data = response.json()
    assert "fecha" in data
    assert "total_ingresos" in data
    assert "cantidad_cobros" in data
    assert "desglose" in data
    assert isinstance(data["desglose"], list)


def test_resumen_caja_sin_cobros_hoy(client, db_session):
    db_session.execute(text("DELETE FROM cobros"))
    db_session.commit()

    response = client.get("/analytics/caja/resumen-diario")
    data = response.json()
    assert data["total_ingresos"] == 0.0
    assert data["cantidad_cobros"] == 0
    assert data["desglose"] == []


def test_resumen_caja_con_cobros(client, db_session):
    hoy = date.today().isoformat()
    db_session.execute(text("DELETE FROM cobros"))
    db_session.execute(text(f"""
        INSERT INTO cobros (id, monto, medio_pago, estado_pago, created_at) VALUES
            (1, 1500.0, 'EFECTIVO', 'APROBADO', '{hoy}'),
            (2, 2000.0, 'TARJETA',  'APROBADO', '{hoy}'),
            (3,  500.0, 'EFECTIVO', 'APROBADO', '{hoy}'),
            (4,  300.0, 'EFECTIVO', 'PENDIENTE', '{hoy}')
    """))
    db_session.commit()

    response = client.get(f"/analytics/caja/resumen-diario?fecha={hoy}")
    data = response.json()
    # Solo cobros APROBADOS: 1500 + 2000 + 500 = 4000
    assert data["total_ingresos"] == 4000.0
    assert data["cantidad_cobros"] == 3
    assert len(data["desglose"]) == 2


def test_resumen_caja_acepta_fecha_custom(client):
    response = client.get("/analytics/caja/resumen-diario?fecha=2025-01-15")
    assert response.status_code == 200
    assert response.json()["fecha"] == "2025-01-15"


def test_resumen_caja_fecha_invalida(client):
    response = client.get("/analytics/caja/resumen-diario?fecha=no-es-fecha")
    assert response.status_code == 422


# ─── /evolucion-mensual ───────────────────────────────────────────────────────

def test_evolucion_mensual_devuelve_lista(client):
    with patch("app.routers.caja.svc.evolucion_mensual_caja", return_value=EVOLUCION_MOCK):
        response = client.get("/analytics/caja/evolucion-mensual")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_evolucion_mensual_acepta_parametro_meses(client):
    with patch("app.routers.caja.svc.evolucion_mensual_caja", return_value=EVOLUCION_MOCK):
        response = client.get("/analytics/caja/evolucion-mensual?meses=3")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_evolucion_mensual_fuera_de_rango(client):
    response = client.get("/analytics/caja/evolucion-mensual?meses=0")
    assert response.status_code == 422
    response = client.get("/analytics/caja/evolucion-mensual?meses=25")
    assert response.status_code == 422


def test_evolucion_mensual_estructura_item(client):
    with patch("app.routers.caja.svc.evolucion_mensual_caja", return_value=EVOLUCION_MOCK):
        response = client.get("/analytics/caja/evolucion-mensual?meses=6")
    data = response.json()
    assert len(data) >= 1
    item = data[0]
    for campo in ("mes", "total_ingresos", "cantidad_cobros"):
        assert campo in item
