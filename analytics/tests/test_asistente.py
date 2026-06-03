from unittest.mock import patch


def test_consulta_asistente_estructura_response(client):
    with patch("app.routers.asistente.consultar_asistente", return_value="Respuesta de prueba"):
        with patch("app.routers.asistente.obtener_contexto_taller", return_value={"ordenes_pendientes": 5}):
            response = client.post(
                "/analytics/asistente/consulta",
                json={"pregunta": "¿Cuántas órdenes hay?"},
            )
    assert response.status_code == 200
    data = response.json()
    assert "respuesta" in data
    assert "contexto_utilizado" in data
    assert data["respuesta"] == "Respuesta de prueba"


def test_consulta_asistente_pregunta_vacia(client):
    response = client.post("/analytics/asistente/consulta", json={"pregunta": ""})
    assert response.status_code == 422


def test_consulta_asistente_pregunta_muy_corta(client):
    # min_length=3: "ab" debe fallar
    response = client.post("/analytics/asistente/consulta", json={"pregunta": "ab"})
    assert response.status_code == 422


def test_consulta_asistente_pregunta_muy_larga(client):
    response = client.post("/analytics/asistente/consulta", json={"pregunta": "a" * 501})
    assert response.status_code == 422


def test_consulta_asistente_sin_cuerpo(client):
    response = client.post("/analytics/asistente/consulta", json={})
    assert response.status_code == 422


def test_consulta_asistente_metodo_get_no_permitido(client):
    response = client.get("/analytics/asistente/consulta")
    assert response.status_code == 405


def test_groq_error_devuelve_500(client):
    with patch("app.routers.asistente.consultar_asistente", side_effect=Exception("Groq unavailable")):
        with patch("app.routers.asistente.obtener_contexto_taller", return_value={}):
            response = client.post(
                "/analytics/asistente/consulta",
                json={"pregunta": "¿Cuántas órdenes hay?"},
            )
    assert response.status_code == 500
    assert "Error al consultar el asistente" in response.json()["detail"]


def test_contexto_error_devuelve_500(client):
    with patch("app.routers.asistente.obtener_contexto_taller", side_effect=Exception("DB down")):
        response = client.post(
            "/analytics/asistente/consulta",
            json={"pregunta": "¿Cuántas órdenes hay?"},
        )
    assert response.status_code == 500


def test_consulta_asistente_pregunta_minima_valida(client):
    with patch("app.routers.asistente.consultar_asistente", return_value="OK"):
        with patch("app.routers.asistente.obtener_contexto_taller", return_value={}):
            response = client.post(
                "/analytics/asistente/consulta",
                json={"pregunta": "abc"},
            )
    assert response.status_code == 200
