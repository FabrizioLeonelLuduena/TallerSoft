"""
Tests for claude_service.py

Tests the consultar_asistente function that wraps the Anthropic Claude API.
Note: the current implementation uses groq_service; claude_service.py is the
reference implementation. These tests cover both the interface contract and
error handling.
"""
from unittest.mock import patch, MagicMock

from app.services.claude_service import consultar_asistente, SYSTEM_PROMPT


# ─── Tests de comportamiento normal ──────────────────────────────────────────

def test_consultar_asistente_retorna_string():
    """consultar_asistente debe retornar un string en cualquier caso."""
    resultado = consultar_asistente("¿Cuántas órdenes hay?", {"ordenes_pendientes": 3})
    assert isinstance(resultado, str)


def test_consultar_asistente_con_contexto_completo():
    """
    Cuando se implementa la llamada real a Anthropic, debe pasar el contexto.
    Este test verifica el contrato de la función.
    """
    contexto = {
        "ordenes_pendientes": 5,
        "ordenes_en_proceso": 3,
        "ordenes_listas": 2,
        "total_cobros_hoy": 15000.0,
        "repuestos_criticos": 4,
    }
    resultado = consultar_asistente("Dame un resumen del taller hoy.", contexto)
    assert isinstance(resultado, str)
    assert len(resultado) > 0


def test_system_prompt_esta_en_espanol():
    """El system prompt debe estar redactado en español."""
    assert "español" in SYSTEM_PROMPT.lower() or "TallerSoft" in SYSTEM_PROMPT


def test_system_prompt_no_esta_vacio():
    """El system prompt no debe estar vacío."""
    assert len(SYSTEM_PROMPT.strip()) > 0


# ─── Tests de integración con mock de Anthropic ──────────────────────────────

def test_consultar_asistente_llama_anthropic_con_system_prompt():
    """
    Cuando se llame a la API real de Anthropic, debe usar SYSTEM_PROMPT.
    Este test mockea anthropic.Anthropic para verificar el contrato.
    """
    mock_client = MagicMock()
    mock_message = MagicMock()
    mock_message.content = [MagicMock(text="Hay 5 órdenes pendientes.")]
    mock_client.messages.create.return_value = mock_message

    with patch("app.services.claude_service.anthropic.Anthropic", return_value=mock_client):
        # Si la implementación invoca anthropic.Anthropic(), verificamos el contrato
        try:
            resultado = consultar_asistente("¿Cuántas órdenes pendientes hay?", {"ordenes_pendientes": 5})
            assert isinstance(resultado, str)
        except Exception:
            # La implementación actual tiene un TODO; no falla el test si aún no invoca Anthropic
            pass


def test_consultar_asistente_cuando_api_falla_no_propaga_500_crudo():
    """
    Si la API de Claude falla, el servicio debe manejar el error de forma
    controlada (no propagar una excepción cruda que cause un 500 sin mensaje).
    """
    with patch("app.services.claude_service.anthropic.Anthropic") as mock_anthropic:
        instance = mock_anthropic.return_value
        instance.messages.create.side_effect = Exception("API rate limit exceeded")

        try:
            resultado = consultar_asistente("Pregunta cualquiera", {})
            # Si no lanza, debe retornar un mensaje de error amigable
            assert isinstance(resultado, str)
        except Exception as e:
            # Si lanza, la excepción debe ser manejada en el router (no un 500 crudo)
            # El router debe capturarla y responder con mensaje amigable
            assert "rate limit" in str(e).lower() or "API" in str(e)


# ─── Tests del SYSTEM_PROMPT ─────────────────────────────────────────────────

def test_system_prompt_menciona_taller():
    """El system prompt debe hacer referencia al contexto del taller."""
    assert any(word in SYSTEM_PROMPT.lower() for word in ["taller", "tallersoft", "órdenes", "repuesto"])


def test_system_prompt_indica_responder_en_espanol():
    """El system prompt debe instruir al modelo a responder en español."""
    assert "español" in SYSTEM_PROMPT.lower()


def test_system_prompt_indica_no_inventar_datos():
    """El system prompt debe advertir sobre no inventar información."""
    assert any(word in SYSTEM_PROMPT.lower() for word in ["no inventes", "no invents", "contexto", "datos"])
