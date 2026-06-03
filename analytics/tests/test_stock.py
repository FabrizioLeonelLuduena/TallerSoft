from sqlalchemy import text


def test_stock_critico_devuelve_lista(client):
    response = client.get("/analytics/stock/critico")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_stock_critico_solo_bajo_minimo(client, db_session):
    db_session.execute(text("DELETE FROM repuestos"))
    db_session.execute(text("""
        INSERT INTO repuestos (id, nombre, categoria, stock_actual, stock_minimo) VALUES
            (1, 'Filtro aceite', 'Filtros', 2, 5),
            (2, 'Bujía NGK',    'Encendido', 10, 3),
            (3, 'Correa dist.', 'Correas', 0, 2)
    """))
    db_session.commit()

    response = client.get("/analytics/stock/critico")
    data = response.json()

    ids_criticos = {r["id"] for r in data}
    assert 1 in ids_criticos   # stock 2 <= minimo 5
    assert 3 in ids_criticos   # stock 0 <= minimo 2
    assert 2 not in ids_criticos  # stock 10 > minimo 3


def test_stock_critico_estructura_item(client, db_session):
    db_session.execute(text("DELETE FROM repuestos"))
    db_session.execute(text("""
        INSERT INTO repuestos (id, nombre, categoria, stock_actual, stock_minimo)
        VALUES (1, 'Filtro', 'Cat', 1, 5)
    """))
    db_session.commit()

    response = client.get("/analytics/stock/critico")
    data = response.json()
    assert len(data) == 1
    item = data[0]
    for campo in ("id", "nombre", "categoria", "stock_actual", "stock_minimo", "diferencia"):
        assert campo in item


def test_stock_critico_diferencia_negativa(client, db_session):
    db_session.execute(text("DELETE FROM repuestos"))
    db_session.execute(text("""
        INSERT INTO repuestos (id, nombre, categoria, stock_actual, stock_minimo)
        VALUES (1, 'Pieza X', 'Cat', 1, 5)
    """))
    db_session.commit()

    response = client.get("/analytics/stock/critico")
    item = response.json()[0]
    # diferencia = stock_actual - stock_minimo = 1 - 5 = -4
    assert item["diferencia"] < 0


def test_repuestos_mas_usados_devuelve_lista(client):
    response = client.get("/analytics/stock/mas-usados")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_repuestos_mas_usados_acepta_parametros(client):
    response = client.get("/analytics/stock/mas-usados?top=5&dias=15")
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_repuestos_mas_usados_top_fuera_de_rango(client):
    response = client.get("/analytics/stock/mas-usados?top=0")
    assert response.status_code == 422
    response = client.get("/analytics/stock/mas-usados?top=101")
    assert response.status_code == 422


def test_repuestos_mas_usados_estructura_item(client, db_session):
    db_session.execute(text("DELETE FROM orden_repuestos"))
    db_session.execute(text("DELETE FROM repuestos"))
    db_session.execute(text("DELETE FROM ordenes_trabajo"))
    db_session.execute(text("""
        INSERT INTO repuestos (id, nombre, categoria, stock_actual, stock_minimo)
        VALUES (1, 'Filtro aceite', 'Filtros', 10, 2)
    """))
    db_session.execute(text("""
        INSERT INTO ordenes_trabajo (id, estado, created_at, updated_at)
        VALUES (1, 'ENTREGADO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    """))
    db_session.execute(text("""
        INSERT INTO orden_repuestos (id, orden_id, repuesto_id, cantidad)
        VALUES (1, 1, 1, 3)
    """))
    db_session.commit()

    response = client.get("/analytics/stock/mas-usados?top=10&dias=365")
    data = response.json()
    assert len(data) >= 1
    item = data[0]
    for campo in ("id", "nombre", "categoria", "total_usado"):
        assert campo in item
