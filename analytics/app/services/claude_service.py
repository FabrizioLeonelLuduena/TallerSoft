"""
Claude AI Service for Analytics

Integration with Anthropic Claude API for intelligent assistant responses.
"""

import os
import anthropic


def consultar_asistente(pregunta: str, contexto: dict) -> str:
    """
    Query the Claude AI assistant with workshop context.
    
    Args:
        pregunta: User question in Spanish
        contexto: Dict with workshop data (orders, stock, revenue, etc.)
    
    Returns:
        Response from Claude in Spanish
    """
    # TODO: Implement in Sprint 5
    return "The assistant response will be implemented in Sprint 5"


SYSTEM_PROMPT = """
Sos el asistente inteligente de TallerSoft, un sistema de gestión para talleres de servicio técnico.
Tu rol es ayudar al dueño y empleados del taller a entender el estado de su negocio respondiendo
preguntas sobre órdenes de trabajo, stock de repuestos, ingresos y rendimiento del equipo.

Reglas:
- Respondé siempre en español, de forma clara y concisa.
- Usá los datos del contexto que se te proveen. No inventes información.
- Si no tenés suficiente información para responder, decilo claramente.
- Podés hacer cálculos simples con los datos disponibles.
- Nunca menciones detalles técnicos de implementación al usuario.
"""
