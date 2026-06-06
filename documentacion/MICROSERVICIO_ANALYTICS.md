# Microservicio Analytics — TallerSoft

## Propósito

El Analytics Service existe separado del Core Service por tres razones:

1. **Separación de responsabilidades:** El Core Service maneja transacciones críticas del negocio. Las operaciones analíticas (GROUP BY, agregaciones, reportes) son costosas y no deben afectar la latencia del core.
2. **Aislamiento de datos:** El Analytics Service tiene permisos de solo lectura sobre la base de datos. Esto garantiza que ningún bug en el código de análisis pueda modificar datos de producción.
3. **Flexibilidad tecnológica:** Python con Pandas y SQLAlchemy es más expresivo para análisis de datos y más fácil de integrar con la Anthropic Claude API que Java.

---

## Stack

| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| Python | 3.11 | Lenguaje |
| FastAPI | 0.109+ | Framework web async |
| SQLAlchemy | 2.x | ORM / queries SQL raw |
| Pandas | 2.x | Análisis y transformación de datos |
| Pydantic | 2.x | Validación de schemas |
| Anthropic SDK | 0.x | Integración con Claude API |
| Groq SDK | — | LLM alternativo para el asistente |
| Uvicorn | — | Servidor ASGI |
| pytest | 7+ | Tests |
| pytest-mock | — | Mocking para tests |

---

## Regla Crítica: Solo Lectura

El Analytics Service **nunca escribe datos**. Está conectado con un usuario de PostgreSQL que tiene únicamente permisos `SELECT`:

```sql
CREATE USER analytics_readonly WITH PASSWORD 'password_seguro';
GRANT CONNECT ON DATABASE tallersoft TO analytics_readonly;
GRANT USAGE ON SCHEMA public TO analytics_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_readonly;
```

---

## Estructura de Carpetas

```
analytics/
├── app/
│   ├── main.py              — Aplicación FastAPI, configuración CORS, routers
│   ├── config.py            — Variables de entorno (Pydantic Settings)
│   ├── db/
│   │   └── database.py      — Sesión SQLAlchemy, get_db() dependency
│   ├── routers/
│   │   ├── ordenes.py       — Endpoints de órdenes
│   │   ├── stock.py         — Endpoints de stock
│   │   ├── caja.py          — Endpoints de caja
│   │   ├── asistente.py     — Endpoint del chat IA
│   │   ├── clientes.py      — Endpoints de clientes
│   │   └── alertas.py       — Alertas automáticas
│   ├── services/
│   │   ├── analytics_service.py — Toda la lógica de queries y cálculos
│   │   ├── claude_service.py    — Integración con Anthropic Claude (referencia)
│   │   └── groq_service.py      — Integración con Groq (implementación activa)
│   └── schemas/
│       ├── asistente_schema.py  — ConsultaRequest, ConsultaResponse
│       ├── ordenes_schema.py    — Schemas de órdenes
│       ├── stock_schema.py      — Schemas de stock
│       └── caja_schema.py       — Schemas de caja
├── tests/
│   ├── conftest.py          — SQLite in-memory para tests, fixtures
│   ├── test_ordenes.py
│   ├── test_stock.py
│   ├── test_caja.py
│   ├── test_asistente.py
│   └── test_claude_service.py
└── wsgi.py                  — Entry point para Gunicorn/Uvicorn
```

---

## Endpoints

### GET /analytics/ordenes/resumen

Totales de órdenes por estado.

**Autenticación:** No requerida (el Gateway lo protege internamente).

**Respuesta:**
```json
{
  "pendientes": 5,
  "en_proceso": 3,
  "listas": 2,
  "entregadas": 47,
  "total": 57
}
```

---

### GET /analytics/ordenes/por-periodo

Órdenes agrupadas por semana o mes en los últimos N meses.

**Query params:**
| Parámetro | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `agrupacion` | `"semana"` \| `"mes"` | `"mes"` | Granularidad temporal |
| `meses_atras` | int (1-24) | 6 | Ventana de tiempo |

**Respuesta:**
```json
[
  { "periodo": "2025-05", "cantidad": 12 },
  { "periodo": "2025-06", "cantidad": 15 }
]
```

---

### GET /analytics/ordenes/tecnicos/rendimiento

Rendimiento de técnicos: órdenes cerradas y tiempo promedio en días.

**Query params:**
| Parámetro | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `mes_actual` | bool | `true` | Si `true`, solo el mes corriente |

**Respuesta:**
```json
[
  {
    "tecnico_id": 2,
    "nombre": "Carlos Gómez",
    "ordenes_cerradas": 8,
    "tiempo_promedio_dias": 2.5
  }
]
```

---

### GET /analytics/stock/critico

Repuestos con `stock_actual <= stock_minimo`, ordenados por diferencia ascendente.

**Respuesta:**
```json
[
  {
    "id": 5,
    "nombre": "Filtro de aceite",
    "categoria": "Filtros",
    "stock_actual": 1,
    "stock_minimo": 5,
    "diferencia": -4
  }
]
```

---

### GET /analytics/stock/mas-usados

Top N repuestos más usados en órdenes en los últimos X días.

**Query params:**
| Parámetro | Tipo | Default | Rango |
|-----------|------|---------|-------|
| `top` | int | 10 | 1-100 |
| `dias` | int | 30 | 1-365 |

**Respuesta:**
```json
[
  { "id": 3, "nombre": "Pantalla OLED", "categoria": "Pantallas", "total_usado": 12 }
]
```

---

### GET /analytics/caja/resumen-diario

Resumen de ingresos del día (o de la fecha indicada).

**Query params:**
| Parámetro | Tipo | Default |
|-----------|------|---------|
| `fecha` | date | hoy |

**Respuesta:**
```json
{
  "fecha": "2025-06-06",
  "total_ingresos": 47500.00,
  "cantidad_cobros": 6,
  "desglose": [
    { "medio_pago": "EFECTIVO", "total": 15000.00, "cantidad": 2 },
    { "medio_pago": "TARJETA", "total": 22500.00, "cantidad": 3 },
    { "medio_pago": "MERCADOPAGO", "total": 10000.00, "cantidad": 1 }
  ]
}
```

---

### GET /analytics/caja/evolucion-mensual

Evolución de ingresos mes a mes.

**Query params:**
| Parámetro | Tipo | Default | Rango |
|-----------|------|---------|-------|
| `meses` | int | 6 | 1-24 |

**Respuesta:**
```json
[
  { "mes": "2025-01", "total_ingresos": 35000.00, "cantidad_cobros": 10 },
  { "mes": "2025-02", "total_ingresos": 41500.00, "cantidad_cobros": 12 }
]
```

---

### POST /analytics/asistente/consulta

Consulta al asistente IA con datos reales del taller.

**Body:**
```json
{ "pregunta": "¿Cuántas órdenes hay pendientes?" }
```

Restricciones: `pregunta` entre 3 y 500 caracteres.

**Respuesta exitosa:**
```json
{
  "respuesta": "Hay 5 órdenes pendientes de asignación. El técnico disponible con mejor rendimiento este mes es Carlos Gómez.",
  "contexto_utilizado": {
    "ordenes_pendientes": 5,
    "ordenes_en_proceso": 3,
    ...
  }
}
```

**Respuesta cuando IA no disponible:**
```json
{
  "respuesta": "Lo siento, el asistente no está disponible en este momento. Intentá de nuevo en unos segundos.",
  "contexto_utilizado": { ... }
}
```

---

## Cómo Funciona el Asistente IA Internamente

```
1. Router recibe POST /analytics/asistente/consulta
   │
2. obtener_contexto_taller(db):
   │  Ejecuta múltiples queries para recopilar:
   │  - Órdenes por estado (resumen_ordenes)
   │  - Cobros del día y evolución mensual
   │  - Repuestos con stock crítico
   │  - Rendimiento de técnicos del mes
   │  - Órdenes sin movimiento > 5 días
   │  Todo se empaqueta en un dict de contexto
   │
3. consultar_asistente(pregunta, contexto):
   │  - Construye el prompt con el SYSTEM_PROMPT + contexto en texto + pregunta
   │  - Llama a Groq API (o Claude API como alternativa)
   │  - Retorna el texto de respuesta
   │
4. Si la API de IA falla:
   │  - El router captura la excepción
   │  - Retorna mensaje amigable (no 500 crudo)
   │
5. Router retorna ConsultaResponse con respuesta + contexto usado
```

---

## System Prompt del Asistente

```
Sos el asistente inteligente de TallerSoft, un sistema de gestión para talleres
de servicio técnico. Tu rol es ayudar al dueño y empleados del taller a entender
el estado de su negocio respondiendo preguntas sobre órdenes de trabajo, stock
de repuestos, ingresos y rendimiento del equipo técnico.

Reglas estrictas:
- Respondé siempre en español rioplatense, de forma clara y concisa.
- Usá exclusivamente los datos del CONTEXTO que se te proveen.
- No inventes ni estimes información.
- Si los datos del contexto no son suficientes para responder, decílo con
  claridad y sugerí qué informe revisar.
- Podés hacer cálculos simples con los datos disponibles.
- Nunca menciones detalles técnicos de implementación al usuario.
- Usá un tono profesional pero cercano.
```

**¿Por qué este diseño?** El prompt tiene instrucciones explícitas de no inventar datos para prevenir alucinaciones. El contexto se construye con datos reales de la BD, y el modelo opera sobre esos datos en lugar de su conocimiento de entrenamiento.

---

## Variables de Entorno Requeridas

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `DATABASE_URL` | URL de conexión a PostgreSQL | `postgresql://analytics_readonly:pass@db:5432/tallersoft` |
| `GROQ_API_KEY` | API key de Groq para el asistente | `gsk_...` |
| `ANTHROPIC_API_KEY` | API key de Claude (alternativa) | `sk-ant-...` |

---

## Configuración de CORS

El Analytics Service acepta requests solo desde el Gateway y el entorno de desarrollo:

```python
allow_origins=[
    "http://localhost:8080",   # Gateway local
    "http://gateway:8080",     # Gateway en Docker
    "http://localhost:4200",   # Frontend en desarrollo
    "http://localhost",        # Frontend en Docker
]
```

En producción, reemplazar con el dominio real del Gateway.

---

## Cómo Correr Localmente

```bash
cd analytics
python -m venv .venv
source .venv/bin/activate      # Linux/Mac
# .venv\Scripts\activate       # Windows
pip install -r requirements.txt
uvicorn app.main:app --port 8082 --reload
# Docs en: http://localhost:8082/docs
```

## Cómo Correr con Docker

```bash
docker compose up analytics -d
```

## Cómo Correr los Tests

```bash
cd analytics
source .venv/bin/activate
python -m pytest tests/ -v --tb=short
python -m pytest tests/ -v --cov=app --cov-report=html  # Con cobertura
```
