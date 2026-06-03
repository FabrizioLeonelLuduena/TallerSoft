from groq import Groq
from app.config import settings

_client = Groq(api_key=settings.groq_api_key)

SYSTEM_PROMPT = """
Sos el asistente inteligente de TallerSoft, un sistema de gestión para talleres de servicio técnico.
Tu rol es ayudar al dueño y empleados del taller a entender el estado de su negocio respondiendo
preguntas sobre órdenes de trabajo, stock de repuestos, ingresos y rendimiento del equipo técnico.

Reglas estrictas:
- Respondé siempre en español rioplatense, de forma clara y concisa.
- Usá exclusivamente los datos del CONTEXTO que se te proveen. No inventes ni estimes información.
- Si los datos del contexto no son suficientes para responder, decílo con claridad y sugerí qué informe revisar.
- Podés hacer cálculos simples con los datos disponibles (promedios, porcentajes, comparaciones).
- Nunca menciones detalles técnicos de implementación (SQL, Python, APIs, etc.) al usuario.
- Usá un tono profesional pero cercano, como el de un consultor de confianza del negocio.
- Si la pregunta es ambigua, interpretá la intención más probable en el contexto de un taller.
"""


def _construir_contexto_texto(contexto: dict) -> str:
    criticos = contexto.get("repuestos_criticos", [])
    criticos_str = ", ".join(criticos) if criticos else "ninguno"

    tecnicos = contexto.get("tecnicos_rendimiento", [])
    tecnicos_str = "\n".join(
        f"  - {t['nombre']}: {t['ordenes_cerradas']} órdenes cerradas, "
        f"promedio {t.get('tiempo_promedio_dias', 0)} días"
        for t in tecnicos
    ) or "  - Sin datos este mes"

    return f"""
ESTADO ACTUAL DEL TALLER:

Órdenes de trabajo:
  - Pendientes: {contexto.get('ordenes_pendientes', 0)}
  - En proceso: {contexto.get('ordenes_en_proceso', 0)}
  - Listas para entregar: {contexto.get('ordenes_listas', 0)}
  - Entregadas (histórico): {contexto.get('ordenes_entregadas', 0)}

Stock:
  - Repuestos con stock crítico: {criticos_str}

Caja del día:
  - Ingresos de hoy: ${contexto.get('ingresos_hoy', 0):,.2f}

Rendimiento del equipo (mes actual):
{tecnicos_str}
"""


def consultar_asistente(pregunta: str, contexto: dict) -> str:
    contexto_texto = _construir_contexto_texto(contexto)

    chat = _client.chat.completions.create(
        model=settings.groq_model,
        max_tokens=settings.groq_max_tokens,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {
                "role": "user",
                "content": f"{contexto_texto}\n\nPREGUNTA: {pregunta}",
            },
        ],
    )

    return chat.choices[0].message.content
