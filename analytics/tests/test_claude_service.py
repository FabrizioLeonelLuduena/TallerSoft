"""
Tests for claude_service.py (reference implementation using Anthropic API).

Note: anthropic is not installed in this environment (the active implementation
uses groq_service.py). We mock sys.modules so the import works without
the real package.
"""
import sys
from unittest.mock import MagicMock, patch

# ── Mock anthropic before importing claude_service ────────────────────────────
_mock_response = MagicMock()
_mock_response.content = [MagicMock(text="Hay 5 órdenes pendientes en el taller.")]

_mock_anthropic = MagicMock()
_mock_anthropic.Anthropic.return_value.messages.create.return_value = _mock_response

sys.modules.setdefault("anthropic", _mock_anthropic)
# ─────────────────────────────────────────────────────────────────────────────

from app.services.claude_service import consultar_asistente, SYSTEM_PROMPT  # noqa: E402


# ─── Tests del SYSTEM_PROMPT ─────────────────────────────────────────────────

def test_system_prompt_no_esta_vacio():
    assert len(SYSTEM_PROMPT.strip()) > 0


def test_system_prompt_menciona_taller():
    assert any(w in SYSTEM_PROMPT.lower() for w in ["taller", "tallersoft", "órdenes", "repuesto"])


def test_system_prompt_indica_responder_en_espanol():
    assert "español" in SYSTEM_PROMPT.lower()


def test_system_prompt_indica_no_inventar_datos():
    assert any(w in SYSTEM_PROMPT.lower() for w in ["no inventes", "contexto", "datos"])


# ─── Tests de comportamiento normal ──────────────────────────────────────────

def test_consultar_asistente_retorna_string():
    resultado = consultar_asistente("¿Cuántas órdenes hay?", {"ordenes_pendientes": 3})
    assert isinstance(resultado, str)
    assert len(resultado) > 0


def test_consultar_asistente_con_contexto_completo():
    contexto = {
        "ordenes_pendientes": 5,
        "ordenes_en_proceso": 3,
        "ordenes_listas": 2,
        "ingresos_hoy": 15000.0,
        "repuestos_criticos": [],
    }
    resultado = consultar_asistente("Dame un resumen del taller hoy.", contexto)
    assert isinstance(resultado, str)
    assert len(resultado) > 0


# ─── Tests de manejo de errores ──────────────────────────────────────────────

def test_consultar_asistente_es_callable_y_retorna_string():
    """claude_service.consultar_asistente es una función pública que retorna str.
    La implementación actual es un stub (la activa usa groq_service)."""
    resultado = consultar_asistente("Cualquier pregunta", {"dato": 1})
    assert callable(consultar_asistente)
    assert isinstance(resultado, str)
    assert len(resultado) > 0
