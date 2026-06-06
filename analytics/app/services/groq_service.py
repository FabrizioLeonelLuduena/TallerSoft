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

    evolucion = contexto.get("evolucion_caja_6_meses", [])
    evolucion_str = "\n".join(
        f"  - {m['mes']}: ${m['total_ingresos']:,.2f} ({m['cantidad_cobros']} cobros)"
        for m in evolucion
    ) or "  - Sin datos"

    tendencia = contexto.get("tendencia_ordenes_6_meses", [])
    tendencia_str = "\n".join(
        f"  - {p['periodo']}: {p['cantidad']} órdenes"
        for p in tendencia
    ) or "  - Sin datos"

    return f"""
FECHA DE HOY: {contexto.get('fecha_hoy', 'N/A')}

ESTADO ACTUAL DEL TALLER:

Órdenes de trabajo:
  - Pendientes: {contexto.get('ordenes_pendientes', 0)}
  - En proceso: {contexto.get('ordenes_en_proceso', 0)}
  - Listas para entregar: {contexto.get('ordenes_listas', 0)}
  - Entregadas (histórico total): {contexto.get('ordenes_entregadas', 0)}

Alertas operativas:
  - Órdenes ALTA prioridad paradas (≥2 días): {contexto.get('ordenes_alta_prioridad_paradas', 0)}
  - Órdenes sin movimiento (≥5 días): {contexto.get('ordenes_sin_movimiento', 0)}

Stock:
  - Repuestos con stock crítico: {criticos_str}

FINANZAS:

Resumen reciente:
  - Ingresos de hoy ({contexto.get('fecha_hoy', '')}): ${contexto.get('ingresos_hoy', 0):,.2f}
  - Ingresos de ayer ({contexto.get('fecha_ayer', '')}): ${contexto.get('ingresos_ayer', 0):,.2f}
  - Ingresos últimos 7 días: ${contexto.get('ingresos_ultimos_7_dias', 0):,.2f}
  - Ingresos acumulados mes actual: ${contexto.get('ingresos_mes_actual', 0):,.2f}
  - Tasa conversión presupuesto→cobro: {contexto.get('conversion_presupuesto_pct', 0)}%
  - Cobros rechazados hoy: ${contexto.get('rechazos_hoy_monto', 0):,.0f}

Evolución de ingresos (últimos 6 meses):
{evolucion_str}

TENDENCIA DE ÓRDENES (últimos 6 meses):
{tendencia_str}

Clientes:
  - % clientes recurrentes este mes: {contexto.get('clientes_recurrentes_pct', 0)}%

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
