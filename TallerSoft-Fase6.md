# Plan de Desarrollo — Microservicio de Analítica
## TallerSoft · Sprint 4 & 5

> **Contexto:** Los módulos del Core Service (Auth, Clientes, Órdenes, Stock, Caja) están completos.
> Este plan cubre la implementación íntegra del `analytics/` service en Python + FastAPI y su integración con el frontend Angular.

---

## Índice

1. [Visión general y restricciones](#1-visión-general-y-restricciones)
2. [Estructura de archivos objetivo](#2-estructura-de-archivos-objetivo)
3. [Fase 1 — Infraestructura base](#fase-1--infraestructura-base)
4. [Fase 2 — Endpoints de analítica](#fase-2--endpoints-de-analítica)
5. [Fase 3 — Asistente IA](#fase-3--asistente-ia)
6. [Fase 4 — Integración Gateway](#fase-4--integración-gateway)
7. [Fase 5 — Frontend Angular (dashboard + chat)](#fase-5--frontend-angular-dashboard--chat)
8. [Fase 6 — Testing y documentación](#fase-6--testing-y-documentación)
9. [Fase 7 — Docker y deploy](#fase-7--docker-y-deploy)
10. [Checklist final](#checklist-final)

---

## 1. Visión general y restricciones

### Reglas de arquitectura que NO se deben romper

- El `analytics` service **solo lee** la base de datos. Nunca ejecutar `INSERT`, `UPDATE` ni `DELETE`.
- El usuario de PostgreSQL configurado en este servicio debe tener **permisos exclusivos de `SELECT`**.
- Todo acceso desde el frontend pasa por el **API Gateway** (puerto `8080`). El Analytics Service en `8082` **nunca es llamado directamente**.
- La comunicación entre servicios es **REST/JSON**. No usar WebSockets ni GraphQL.
- Cada variable sensible va en el `.env` del servicio. No hardcodear credenciales.

### Stack del servicio

| Componente | Tecnología | Versión |
|---|---|---|
| Framework HTTP | FastAPI | 0.111.0 |
| Servidor ASGI | Uvicorn | 0.30.0 |
| ORM / conexión BD | SQLAlchemy | 2.0.30 |
| Análisis de datos | Pandas | 2.2.2 |
| Driver PostgreSQL | psycopg2-binary | 2.9.9 |
| IA conversacional | Groq SDK (Llama 3.3 70B) | latest |
| Validación | Pydantic | 2.7.1 |
| Variables de entorno | python-dotenv | 1.0.1 |

---

## 2. Estructura de archivos objetivo

Al finalizar el desarrollo, la carpeta `analytics/` debe tener esta estructura:

```
analytics/
├── app/
│   ├── __init__.py
│   ├── main.py                        # Entry point FastAPI, middlewares, routers
│   ├── config.py                      # Carga y valida variables de entorno
│   ├── db/
│   │   ├── __init__.py
│   │   └── database.py                # Engine SQLAlchemy (solo lectura) + get_db
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── ordenes.py                 # GET /analytics/ordenes/*
│   │   ├── stock.py                   # GET /analytics/stock/*
│   │   ├── caja.py                    # GET /analytics/caja/*
│   │   └── asistente.py               # POST /analytics/asistente/consulta
│   ├── services/
│   │   ├── __init__.py
│   │   ├── analytics_service.py       # Queries Pandas por dominio + obtener_contexto_taller
│   │   └── groq_service.py            # Integración Groq API (Llama 3.3 70B)
│   └── schemas/
│       ├── __init__.py
│       ├── ordenes_schema.py          # Pydantic models respuestas órdenes
│       ├── stock_schema.py            # Pydantic models respuestas stock
│       ├── caja_schema.py             # Pydantic models respuestas caja
│       └── asistente_schema.py        # ConsultaRequest, ConsultaResponse
├── tests/
│   ├── __init__.py
│   ├── conftest.py                    # Fixtures: BD en memoria, cliente test FastAPI
│   ├── test_ordenes.py
│   ├── test_stock.py
│   ├── test_caja.py
│   └── test_asistente.py
├── .env                               # Variables de entorno (no commitear)
├── .env.example                       # Plantilla de variables (sí commitear)
├── requirements.txt
├── Dockerfile
└── README.md
```

---

## Fase 1 — Infraestructura base

### Tarea 1.1 — Variables de entorno y configuración

**Archivo:** `analytics/.env.example`

Crear el archivo con las siguientes variables. El archivo `.env` real (con valores reales) **no se commitea**.

```env
# Base de datos — usuario con permisos solo de SELECT
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tallersoft
DB_USER=analytics_readonly
DB_PASSWORD=password_seguro

# Groq (Llama 3.3 70B — tier gratuito, sin tarjeta de crédito)
# Obtener API key en: https://console.groq.com
GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxx
GROQ_MODEL=llama-3.3-70b-versatile
GROQ_MAX_TOKENS=1024

# Servicio
ANALYTICS_PORT=8082
ALLOWED_ORIGINS=http://localhost:8080
```

**Archivo:** `analytics/app/config.py`

Crear una clase `Settings` usando Pydantic `BaseSettings` que cargue y valide todas las variables de entorno al iniciar. Si falta alguna variable crítica (`GROQ_API_KEY`, `DB_*`), el servicio debe fallar al arrancar con un mensaje claro.

```python
# app/config.py
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    db_host: str
    db_port: int = 5432
    db_name: str
    db_user: str
    db_password: str
    groq_api_key: str
    groq_model: str = "llama-3.3-70b-versatile"
    groq_max_tokens: int = 1024
    allowed_origins: str = "http://localhost:8080"

    class Config:
        env_file = ".env"

settings = Settings()
```

> **Nota para Claude Code:** Agregar `pydantic-settings` al `requirements.txt`.

---

### Tarea 1.2 — Conexión a la base de datos (solo lectura)

**Archivo:** `analytics/app/db/database.py`

Implementar la conexión con SQLAlchemy usando el patrón de sesión con `yield`. La sesión debe configurarse en modo solo lectura a nivel de aplicación para tener una segunda capa de seguridad además de los permisos del usuario de BD.

```python
# app/db/database.py
from sqlalchemy import create_engine, event
from sqlalchemy.orm import sessionmaker
from app.config import settings

DATABASE_URL = (
    f"postgresql://{settings.db_user}:{settings.db_password}"
    f"@{settings.db_host}:{settings.db_port}/{settings.db_name}"
)

engine = create_engine(
    DATABASE_URL,
    pool_size=5,
    max_overflow=10,
    pool_pre_ping=True,          # Detecta conexiones muertas
)

# Segunda capa de protección: si SQLAlchemy intenta escribir, el evento lo bloquea
@event.listens_for(engine, "before_cursor_execute")
def prevent_writes(conn, cursor, statement, parameters, context, executemany):
    stmt_upper = statement.strip().upper()
    if any(stmt_upper.startswith(w) for w in ["INSERT", "UPDATE", "DELETE", "DROP", "ALTER"]):
        raise PermissionError(f"El Analytics Service no puede ejecutar: {stmt_upper[:30]}...")

SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
```

---

### Tarea 1.3 — Entry point y middlewares

**Archivo:** `analytics/app/main.py`

```python
# app/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import ordenes, stock, caja, asistente
from app.config import settings

app = FastAPI(
    title="TallerSoft Analytics API",
    version="1.0.0",
    docs_url="/docs",            # Swagger UI en /docs
    redoc_url="/redoc",
)

# CORS: solo aceptar requests del API Gateway
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins.split(","),
    allow_methods=["GET", "POST"],
    allow_headers=["Authorization", "Content-Type"],
)

# Registrar routers
app.include_router(ordenes.router, prefix="/analytics/ordenes", tags=["Órdenes"])
app.include_router(stock.router,   prefix="/analytics/stock",   tags=["Stock"])
app.include_router(caja.router,    prefix="/analytics/caja",    tags=["Caja"])
app.include_router(asistente.router, prefix="/analytics/asistente", tags=["Asistente IA"])

@app.get("/health", tags=["Health"])
def health_check():
    return {"status": "ok", "service": "analytics"}
```

---

### Tarea 1.4 — `requirements.txt`

```
fastapi==0.111.0
uvicorn==0.30.0
sqlalchemy==2.0.30
pandas==2.2.2
psycopg2-binary==2.9.9
groq==0.9.0
python-dotenv==1.0.1
pydantic==2.7.1
pydantic-settings==2.3.4
pytest==8.2.2
httpx==0.27.0
pytest-asyncio==0.23.7
```

---

## Fase 2 — Endpoints de analítica

### Tarea 2.1 — Schemas Pydantic

Crear un schema por dominio con los modelos de respuesta. Esto garantiza que el contrato con el frontend esté tipado y documentado en Swagger.

**Archivo:** `analytics/app/schemas/ordenes_schema.py`

```python
from pydantic import BaseModel
from typing import List

class ResumenOrdenes(BaseModel):
    pendientes: int
    en_proceso: int
    listas: int
    entregadas: int
    total: int

class OrdenesPorPeriodo(BaseModel):
    periodo: str          # "2025-W22" o "2025-06"
    cantidad: int

class RendimientoTecnico(BaseModel):
    tecnico_id: int
    nombre: str
    ordenes_cerradas: int
    tiempo_promedio_dias: float
```

**Archivo:** `analytics/app/schemas/stock_schema.py`

```python
from pydantic import BaseModel

class RepuestoCritico(BaseModel):
    id: int
    nombre: str
    categoria: str | None
    stock_actual: int
    stock_minimo: int
    deficit: int           # stock_minimo - stock_actual

class RepuestoMasUsado(BaseModel):
    id: int
    nombre: str
    total_usado: int
    ordenes_count: int
```

**Archivo:** `analytics/app/schemas/caja_schema.py`

```python
from pydantic import BaseModel
from typing import List
from datetime import date

class DesgloseMedioPago(BaseModel):
    medio_pago: str
    total: float
    cantidad: int

class ResumenDiario(BaseModel):
    fecha: date
    total_ingresos: float
    cantidad_cobros: int
    desglose: List[DesgloseMedioPago]

class EvolucionMensual(BaseModel):
    mes: str              # "2025-06"
    total_ingresos: float
    cantidad_cobros: int
```

**Archivo:** `analytics/app/schemas/asistente_schema.py`

```python
from pydantic import BaseModel, Field

class ConsultaRequest(BaseModel):
    pregunta: str = Field(..., min_length=3, max_length=500)

class ConsultaResponse(BaseModel):
    respuesta: str
    contexto_utilizado: dict   # Para debug (puede filtrarse en producción)
```

---

### Tarea 2.2 — Servicio de analítica con Pandas

**Archivo:** `analytics/app/services/analytics_service.py`

Este es el archivo más importante del servicio. Implementar cada función usando `pandas.read_sql()` para leer datos y procesarlos en memoria. Cada función recibe una sesión de SQLAlchemy.

```python
# app/services/analytics_service.py
import pandas as pd
from sqlalchemy.orm import Session
from datetime import date, timedelta
from typing import Literal

# ─── ÓRDENES ────────────────────────────────────────────────────────────────

def resumen_ordenes(db: Session) -> dict:
    """Totales de órdenes agrupados por estado."""
    sql = "SELECT estado, COUNT(*) as cantidad FROM ordenes_trabajo GROUP BY estado"
    df = pd.read_sql(sql, db.bind)
    conteos = df.set_index("estado")["cantidad"].to_dict()
    return {
        "pendientes":  int(conteos.get("PENDIENTE", 0)),
        "en_proceso":  int(conteos.get("EN_PROCESO", 0)),
        "listas":      int(conteos.get("LISTO", 0)),
        "entregadas":  int(conteos.get("ENTREGADO", 0)),
        "total":       int(df["cantidad"].sum()),
    }

def ordenes_por_periodo(
    db: Session,
    agrupacion: Literal["semana", "mes"] = "mes",
    meses_atras: int = 6
) -> list[dict]:
    """
    Devuelve la cantidad de órdenes creadas agrupadas por semana o mes.
    Por defecto trae los últimos 6 meses.
    """
    desde = date.today() - timedelta(days=meses_atras * 30)
    sql = f"""
        SELECT created_at, id
        FROM ordenes_trabajo
        WHERE created_at >= '{desde}'
    """
    df = pd.read_sql(sql, db.bind, parse_dates=["created_at"])
    if df.empty:
        return []

    freq = "W" if agrupacion == "semana" else "ME"
    df["periodo"] = df["created_at"].dt.to_period(freq).astype(str)
    resultado = df.groupby("periodo").size().reset_index(name="cantidad")
    return resultado.to_dict(orient="records")

def rendimiento_tecnicos(db: Session, mes_actual: bool = True) -> list[dict]:
    """Órdenes cerradas (ENTREGADO) por técnico, con tiempo promedio en días."""
    filtro_fecha = ""
    if mes_actual:
        hoy = date.today()
        filtro_fecha = f"AND ot.updated_at >= '{hoy.replace(day=1)}'"

    sql = f"""
        SELECT
            u.id AS tecnico_id,
            u.nombre,
            COUNT(ot.id) AS ordenes_cerradas,
            ROUND(AVG(EXTRACT(EPOCH FROM (ot.updated_at - ot.created_at)) / 86400), 2) AS tiempo_promedio_dias
        FROM ordenes_trabajo ot
        JOIN usuarios u ON u.id = ot.tecnico_id
        WHERE ot.estado = 'ENTREGADO'
        {filtro_fecha}
        GROUP BY u.id, u.nombre
        ORDER BY ordenes_cerradas DESC
    """
    df = pd.read_sql(sql, db.bind)
    return df.to_dict(orient="records")


# ─── STOCK ──────────────────────────────────────────────────────────────────

def stock_critico(db: Session) -> list[dict]:
    """Repuestos donde stock_actual <= stock_minimo."""
    sql = """
        SELECT id, nombre, categoria, stock_actual, stock_minimo,
               (stock_minimo - stock_actual) AS deficit
        FROM repuestos
        WHERE stock_actual <= stock_minimo
        ORDER BY deficit DESC
    """
    df = pd.read_sql(sql, db.bind)
    return df.to_dict(orient="records")

def repuestos_mas_usados(db: Session, top: int = 10) -> list[dict]:
    """Top N repuestos más usados en órdenes."""
    sql = f"""
        SELECT
            r.id,
            r.nombre,
            SUM(ore.cantidad) AS total_usado,
            COUNT(DISTINCT ore.orden_id) AS ordenes_count
        FROM orden_repuestos ore
        JOIN repuestos r ON r.id = ore.repuesto_id
        GROUP BY r.id, r.nombre
        ORDER BY total_usado DESC
        LIMIT {top}
    """
    df = pd.read_sql(sql, db.bind)
    return df.to_dict(orient="records")


# ─── CAJA ────────────────────────────────────────────────────────────────────

def resumen_caja_diario(db: Session, fecha: date | None = None) -> dict:
    """Resumen de cobros aprobados para un día (hoy por defecto)."""
    if fecha is None:
        fecha = date.today()

    sql = f"""
        SELECT medio_pago, monto
        FROM cobros
        WHERE estado_pago = 'APROBADO'
          AND DATE(created_at) = '{fecha}'
    """
    df = pd.read_sql(sql, db.bind)
    if df.empty:
        return {"fecha": str(fecha), "total_ingresos": 0.0, "cantidad_cobros": 0, "desglose": []}

    desglose = (
        df.groupby("medio_pago")
          .agg(total=("monto", "sum"), cantidad=("monto", "count"))
          .reset_index()
          .rename(columns={"medio_pago": "medio_pago"})
          .to_dict(orient="records")
    )
    return {
        "fecha":           str(fecha),
        "total_ingresos":  float(df["monto"].sum()),
        "cantidad_cobros": int(len(df)),
        "desglose":        desglose,
    }

def evolucion_mensual_caja(db: Session, meses: int = 6) -> list[dict]:
    """Ingresos mes a mes para los últimos N meses."""
    desde = date.today() - timedelta(days=meses * 30)
    sql = f"""
        SELECT monto, created_at
        FROM cobros
        WHERE estado_pago = 'APROBADO'
          AND created_at >= '{desde}'
    """
    df = pd.read_sql(sql, db.bind, parse_dates=["created_at"])
    if df.empty:
        return []

    df["mes"] = df["created_at"].dt.to_period("M").astype(str)
    resultado = (
        df.groupby("mes")
          .agg(total_ingresos=("monto", "sum"), cantidad_cobros=("monto", "count"))
          .reset_index()
    )
    return resultado.to_dict(orient="records")


# ─── CONTEXTO PARA EL ASISTENTE ──────────────────────────────────────────────

def obtener_contexto_taller(db: Session) -> dict:
    """
    Agrega un snapshot del estado actual del taller para enriquecer
    el prompt que se envía al asistente IA.
    """
    resumen = resumen_ordenes(db)
    criticos = stock_critico(db)
    caja_hoy = resumen_caja_diario(db)
    tecnicos = rendimiento_tecnicos(db, mes_actual=True)

    top_tecnico = tecnicos[0]["nombre"] if tecnicos else "N/A"
    nombres_criticos = [r["nombre"] for r in criticos[:5]]  # máximo 5 para no saturar el prompt

    return {
        "ordenes_pendientes":   resumen["pendientes"],
        "ordenes_en_proceso":   resumen["en_proceso"],
        "ordenes_listas":       resumen["listas"],
        "ordenes_entregadas":   resumen["entregadas"],
        "repuestos_criticos":   nombres_criticos,
        "ingresos_hoy":         caja_hoy["total_ingresos"],
        "top_tecnico":          top_tecnico,
        "tecnicos_rendimiento": tecnicos[:3],  # top 3 para el contexto
    }
```

---

### Tarea 2.3 — Routers

**Archivo:** `analytics/app/routers/ordenes.py`

```python
from fastapi import APIRouter, Depends, Query
from typing import Literal
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services import analytics_service as svc

router = APIRouter()

@router.get("/resumen")
def resumen_ordenes(db: Session = Depends(get_db)):
    """Totales de órdenes por estado."""
    return svc.resumen_ordenes(db)

@router.get("/por-periodo")
def ordenes_por_periodo(
    agrupacion: Literal["semana", "mes"] = Query("mes"),
    meses_atras: int = Query(6, ge=1, le=24),
    db: Session = Depends(get_db)
):
    """Órdenes agrupadas por semana o mes en los últimos N meses."""
    return svc.ordenes_por_periodo(db, agrupacion, meses_atras)

@router.get("/tecnicos/rendimiento")
def rendimiento_tecnicos(
    mes_actual: bool = Query(True),
    db: Session = Depends(get_db)
):
    """Rendimiento de técnicos (órdenes cerradas + tiempo promedio)."""
    return svc.rendimiento_tecnicos(db, mes_actual)
```

**Archivo:** `analytics/app/routers/stock.py`

```python
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services import analytics_service as svc

router = APIRouter()

@router.get("/critico")
def stock_critico(db: Session = Depends(get_db)):
    """Repuestos con stock por debajo del mínimo."""
    return svc.stock_critico(db)

@router.get("/mas-usados")
def repuestos_mas_usados(
    top: int = Query(10, ge=1, le=50),
    db: Session = Depends(get_db)
):
    """Top N repuestos más utilizados en órdenes."""
    return svc.repuestos_mas_usados(db, top)
```

**Archivo:** `analytics/app/routers/caja.py`

```python
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from datetime import date
from app.db.database import get_db
from app.services import analytics_service as svc

router = APIRouter()

@router.get("/resumen-diario")
def resumen_caja_diario(
    fecha: date = Query(default=None),
    db: Session = Depends(get_db)
):
    """Resumen de ingresos del día (o de la fecha indicada)."""
    return svc.resumen_caja_diario(db, fecha)

@router.get("/evolucion-mensual")
def evolucion_mensual(
    meses: int = Query(6, ge=1, le=24),
    db: Session = Depends(get_db)
):
    """Evolución de ingresos mes a mes."""
    return svc.evolucion_mensual_caja(db, meses)
```

---

### Tarea 2.4 — Router del asistente IA

**Archivo:** `analytics/app/routers/asistente.py`

```python
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services.groq_service import consultar_asistente
from app.services.analytics_service import obtener_contexto_taller
from app.schemas.asistente_schema import ConsultaRequest, ConsultaResponse

router = APIRouter()

@router.post("/consulta", response_model=ConsultaResponse)
def chat_asistente(request: ConsultaRequest, db: Session = Depends(get_db)):
    """
    Recibe una pregunta en lenguaje natural, enriquece el prompt con datos
    reales del taller y devuelve la respuesta del asistente IA.
    """
    try:
        contexto = obtener_contexto_taller(db)
        respuesta = consultar_asistente(request.pregunta, contexto)
        return ConsultaResponse(respuesta=respuesta, contexto_utilizado=contexto)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error al consultar el asistente: {str(e)}")
```

---

## Fase 3 — Asistente IA

### ¿Por qué Groq + Llama 3.3 70B?

Groq es la mejor opción gratuita y open-source para este caso de uso por tres razones concretas:

- **Tier gratuito real y sin vencimiento:** el tier gratuito no requiere tarjeta de crédito y no tiene límite de tiempo en su uso. Para un taller PYME con uso moderado del chat, los límites son más que suficientes.
- **Velocidad excepcional:** Groq corre exclusivamente modelos open-source a 300–1.000 tokens por segundo usando su hardware LPU propio, lo que da una experiencia de chat casi instantánea.
- **Modelo de calidad:** Llama 3.3 70B se entrega a más de 300 tokens/seg de forma gratuita con un endpoint compatible con OpenAI, lo que facilita la integración.

**Límites del tier gratuito relevantes para TallerSoft:**

| Modelo | Requests/día | Tokens/minuto |
|---|---|---|
| `llama-3.3-70b-versatile` | 1.000 req/día | 12.000 TPM |
| `llama-3.1-8b-instant` (fallback) | 14.400 req/día | 6.000 TPM |

> Para un taller con uso normal del chat (10–50 consultas/día), el modelo 70B es más que suficiente. Si se necesita más volumen, Llama 3.1 8B ofrece 14.400 requests por día con un presupuesto diario de 500.000 tokens, ideal como fallback.

**Obtener la API Key:** registrarse en [console.groq.com](https://console.groq.com) → API Keys → Create API Key. Sin tarjeta de crédito.

---

### Tarea 3.1 — Servicio Groq

**Archivo:** `analytics/app/services/groq_service.py`

```python
# app/services/groq_service.py
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
        f"  - {t['nombre']}: {t['ordenes_cerradas']} órdenes cerradas, promedio {t.get('tiempo_promedio_dias', 0)} días"
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
        model=settings.groq_model,                # llama-3.3-70b-versatile
        max_tokens=settings.groq_max_tokens,      # 1024
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {
                "role": "user",
                "content": f"{contexto_texto}\n\nPREGUNTA: {pregunta}"
            }
        ]
    )

    return chat.choices[0].message.content
```

> **Nota para Claude Code:** La API de Groq usa el mismo formato de respuesta que OpenAI (`choices[0].message.content`), por lo que si en el futuro se quiere cambiar de proveedor (OpenAI, Together.ai, etc.) el cambio es mínimo.

---

## Fase 4 — Integración Gateway

### Tarea 4.1 — Rutas en Spring Cloud Gateway

En el archivo `gateway/src/main/resources/application.yml`, agregar las rutas del Analytics Service. El Gateway ya valida el JWT antes de reenviar el request.

```yaml
# gateway/src/main/resources/application.yml  (agregar dentro de spring.cloud.gateway.routes)

- id: analytics-service
  uri: http://localhost:8082
  predicates:
    - Path=/analytics/**
  filters:
    - StripPrefix=0   # No remover el prefijo; el servicio ya espera /analytics/*
```

> **Importante:** El endpoint `/analytics/asistente/consulta` es `POST` y requiere JWT. No agregarlo a la lista de rutas públicas del Gateway.

### Tarea 4.2 — Verificar CORS del Analytics Service

El `main.py` ya configura CORS para aceptar solo el origen del Gateway. Verificar que `ALLOWED_ORIGINS` en el `.env` apunte al origen correcto (`http://localhost:8080` en dev, dominio de producción en prod).

---

## Fase 5 — Frontend Angular (dashboard + chat)

### Tarea 5.1 — Servicio Angular para Analytics

**Archivo:** `frontend/src/app/core/services/analytics.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private base = `${environment.apiUrl}/analytics`;

  constructor(private http: HttpClient) {}

  getResumenOrdenes(): Observable<any> {
    return this.http.get(`${this.base}/ordenes/resumen`);
  }

  getOrdenesPorPeriodo(agrupacion: 'semana' | 'mes' = 'mes', mesesAtras = 6): Observable<any[]> {
    const params = new HttpParams()
      .set('agrupacion', agrupacion)
      .set('meses_atras', mesesAtras);
    return this.http.get<any[]>(`${this.base}/ordenes/por-periodo`, { params });
  }

  getRendimientoTecnicos(mesActual = true): Observable<any[]> {
    const params = new HttpParams().set('mes_actual', mesActual);
    return this.http.get<any[]>(`${this.base}/ordenes/tecnicos/rendimiento`, { params });
  }

  getStockCritico(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/stock/critico`);
  }

  getRepuestosMasUsados(top = 10): Observable<any[]> {
    const params = new HttpParams().set('top', top);
    return this.http.get<any[]>(`${this.base}/stock/mas-usados`, { params });
  }

  getResumenCajaDiario(fecha?: string): Observable<any> {
    let params = new HttpParams();
    if (fecha) params = params.set('fecha', fecha);
    return this.http.get(`${this.base}/caja/resumen-diario`, { params });
  }

  getEvolucionMensual(meses = 6): Observable<any[]> {
    const params = new HttpParams().set('meses', meses);
    return this.http.get<any[]>(`${this.base}/caja/evolucion-mensual`, { params });
  }

  consultarAsistente(pregunta: string): Observable<{ respuesta: string }> {
    return this.http.post<{ respuesta: string }>(`${this.base}/asistente/consulta`, { pregunta });
  }
}
```

---

### Tarea 5.2 — Dashboard con KPIs

**Archivo:** `frontend/src/app/modules/dashboard/dashboard.component.ts`

El componente debe consumir varios endpoints en paralelo al iniciar y renderizar los KPIs. Usar `forkJoin` para hacer todas las llamadas simultáneas.

```typescript
import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { AnalyticsService } from '@core/services/analytics.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  resumenOrdenes: any;
  stockCritico: any[] = [];
  cajaDiaria: any;
  evolucionMensual: any[] = [];
  rendimientoTecnicos: any[] = [];
  loading = true;
  error = false;

  // Configuración ApexCharts para evolución mensual
  chartOptions: any;

  constructor(private analytics: AnalyticsService) {}

  ngOnInit(): void {
    forkJoin({
      ordenes:   this.analytics.getResumenOrdenes(),
      stock:     this.analytics.getStockCritico(),
      caja:      this.analytics.getResumenCajaDiario(),
      evolucion: this.analytics.getEvolucionMensual(6),
      tecnicos:  this.analytics.getRendimientoTecnicos(),
    }).subscribe({
      next: (data) => {
        this.resumenOrdenes    = data.ordenes;
        this.stockCritico      = data.stock;
        this.cajaDiaria        = data.caja;
        this.evolucionMensual  = data.evolucion;
        this.rendimientoTecnicos = data.tecnicos;
        this.buildChartOptions(data.evolucion);
        this.loading = false;
      },
      error: () => {
        this.error   = true;
        this.loading = false;
      }
    });
  }

  private buildChartOptions(evolucion: any[]): void {
    this.chartOptions = {
      series: [{ name: 'Ingresos', data: evolucion.map(e => e.total_ingresos) }],
      chart: { type: 'bar', height: 300 },
      xaxis: { categories: evolucion.map(e => e.mes) },
      yaxis: { labels: { formatter: (v: number) => `$${v.toLocaleString()}` } },
      colors: ['#1976D2'],
    };
  }
}
```

**Archivo:** `frontend/src/app/modules/dashboard/dashboard.component.html`

El template debe mostrar:
- 4 tarjetas KPI: Órdenes Pendientes, En Proceso, Listas, Ingresos del Día
- Gráfico de barras de evolución mensual (ApexCharts `<apx-chart>`)
- Tabla de stock crítico con badge rojo cuando `deficit > 0`
- Tabla de rendimiento de técnicos

---

### Tarea 5.3 — Módulo Asistente IA (chat)

**Archivo:** `frontend/src/app/modules/asistente/asistente.component.ts`

```typescript
import { Component } from '@angular/core';
import { AnalyticsService } from '@core/services/analytics.service';

interface Mensaje {
  rol: 'usuario' | 'asistente';
  texto: string;
  timestamp: Date;
}

@Component({
  selector: 'app-asistente',
  templateUrl: './asistente.component.html',
})
export class AsistenteComponent {
  mensajes: Mensaje[] = [];
  preguntaActual = '';
  cargando = false;

  constructor(private analytics: AnalyticsService) {
    // Mensaje de bienvenida
    this.mensajes.push({
      rol: 'asistente',
      texto: '¡Hola! Soy el asistente de TallerSoft. Podés preguntarme sobre órdenes, stock, ingresos o el rendimiento de tu equipo.',
      timestamp: new Date(),
    });
  }

  enviar(): void {
    const pregunta = this.preguntaActual.trim();
    if (!pregunta || this.cargando) return;

    this.mensajes.push({ rol: 'usuario', texto: pregunta, timestamp: new Date() });
    this.preguntaActual = '';
    this.cargando = true;

    this.analytics.consultarAsistente(pregunta).subscribe({
      next: (res) => {
        this.mensajes.push({ rol: 'asistente', texto: res.respuesta, timestamp: new Date() });
        this.cargando = false;
      },
      error: () => {
        this.mensajes.push({
          rol: 'asistente',
          texto: 'Ocurrió un error al consultar el asistente. Intentá de nuevo en unos segundos.',
          timestamp: new Date(),
        });
        this.cargando = false;
      }
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.enviar();
    }
  }
}
```

**Template requerido** (`asistente.component.html`):
- Lista de mensajes con burbujas diferenciadas por `rol`
- Indicador de carga (spinner o "escribiendo…") cuando `cargando = true`
- Input de texto con botón Enviar (deshabilitar ambos cuando `cargando = true`)
- Auto-scroll al último mensaje cuando llega la respuesta

---

## Fase 6 — Testing y documentación

### Tarea 6.1 — Tests del Analytics Service

**Archivo:** `analytics/tests/conftest.py`

```python
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.main import app
from app.db.database import get_db

# BD SQLite en memoria para tests (sin el listener de solo lectura)
SQLALCHEMY_DATABASE_URL = "sqlite://"

engine_test = create_engine(SQLALCHEMY_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(bind=engine_test)

def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()

app.dependency_overrides[get_db] = override_get_db

@pytest.fixture
def client():
    return TestClient(app)
```

**Archivo:** `analytics/tests/test_ordenes.py`

```python
def test_resumen_ordenes_devuelve_estructura_correcta(client):
    response = client.get("/analytics/ordenes/resumen")
    assert response.status_code == 200
    data = response.json()
    assert "pendientes" in data
    assert "en_proceso" in data
    assert "listas" in data
    assert "entregadas" in data
    assert "total" in data

def test_ordenes_por_periodo_acepta_parametros(client):
    response = client.get("/analytics/ordenes/por-periodo?agrupacion=semana&meses_atras=3")
    assert response.status_code == 200
    assert isinstance(response.json(), list)

def test_ordenes_por_periodo_parametro_invalido(client):
    response = client.get("/analytics/ordenes/por-periodo?agrupacion=anio")
    assert response.status_code == 422   # Pydantic validation error
```

**Archivo:** `analytics/tests/test_asistente.py`

```python
from unittest.mock import patch

def test_consulta_asistente_estructura_response(client):
    with patch("app.routers.asistente.consultar_asistente", return_value="Respuesta de prueba"):
        with patch("app.routers.asistente.obtener_contexto_taller", return_value={}):
            response = client.post("/analytics/asistente/consulta", json={"pregunta": "¿Cuántas órdenes hay?"})
    assert response.status_code == 200
    assert "respuesta" in response.json()

def test_consulta_asistente_pregunta_vacia(client):
    response = client.post("/analytics/asistente/consulta", json={"pregunta": ""})
    assert response.status_code == 422

def test_consulta_asistente_pregunta_muy_larga(client):
    response = client.post("/analytics/asistente/consulta", json={"pregunta": "a" * 501})
    assert response.status_code == 422

def test_groq_error_devuelve_500(client):
    from groq import APIError
    with patch("app.routers.asistente.consultar_asistente", side_effect=Exception("Groq unavailable")):
        with patch("app.routers.asistente.obtener_contexto_taller", return_value={}):
            response = client.post("/analytics/asistente/consulta", json={"pregunta": "¿Cuántas órdenes hay?"})
    assert response.status_code == 500
```

**Comandos para ejecutar tests:**

```bash
cd analytics
pytest tests/ -v                    # Todos los tests con verbose
pytest tests/test_ordenes.py -v     # Solo tests de órdenes
pytest tests/ --cov=app             # Con cobertura de código
```

---

### Tarea 6.2 — Colección Postman

Agregar los siguientes requests a la colección existente `tallersoft.postman_collection.json`:

| Nombre | Método | URL |
|---|---|---|
| Analytics - Resumen órdenes | GET | `{{gateway}}/analytics/ordenes/resumen` |
| Analytics - Órdenes por mes | GET | `{{gateway}}/analytics/ordenes/por-periodo?agrupacion=mes&meses_atras=6` |
| Analytics - Rendimiento técnicos | GET | `{{gateway}}/analytics/ordenes/tecnicos/rendimiento` |
| Analytics - Stock crítico | GET | `{{gateway}}/analytics/stock/critico` |
| Analytics - Repuestos más usados | GET | `{{gateway}}/analytics/stock/mas-usados?top=10` |
| Analytics - Resumen caja diario | GET | `{{gateway}}/analytics/caja/resumen-diario` |
| Analytics - Evolución mensual caja | GET | `{{gateway}}/analytics/caja/evolucion-mensual?meses=6` |
| Analytics - Consultar asistente | POST | `{{gateway}}/analytics/asistente/consulta` |

Todos los requests deben incluir el header `Authorization: Bearer {{jwt_token}}`.

---

### Tarea 6.3 — README del Analytics Service

Crear `analytics/README.md` con:
- Descripción y propósito del servicio
- Instrucciones para crear el usuario de BD con solo lectura
- Instrucciones de instalación y ejecución local
- Descripción de cada endpoint con ejemplos de respuesta
- Instrucciones para correr los tests

---

## Fase 7 — Docker y deploy

### Tarea 7.1 — Dockerfile del Analytics Service

**Archivo:** `analytics/Dockerfile`

```dockerfile
FROM python:3.11-slim

WORKDIR /app

# Copiar dependencias primero (cache de capas Docker)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copiar código fuente
COPY app/ ./app/

EXPOSE 8082

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8082"]
```

---

### Tarea 7.2 — Verificar docker-compose.yml

El `docker-compose.yml` raíz ya tiene el servicio `analytics` definido. Verificar que estén configuradas todas las variables:

```yaml
analytics:
  build: ./analytics
  environment:
    DB_HOST: db
    DB_PORT: 5432
    DB_NAME: ${DB_NAME}
    DB_USER: ${DB_USER}
    DB_PASSWORD: ${DB_PASSWORD}
    GROQ_API_KEY: ${GROQ_API_KEY}
    GROQ_MODEL: ${GROQ_MODEL}
    GROQ_MAX_TOKENS: ${GROQ_MAX_TOKENS}
    ALLOWED_ORIGINS: http://gateway:8080
  depends_on:
    - db
  ports:
    - "8082:8082"
  restart: unless-stopped
```

---

### Tarea 7.3 — Usuario de BD con permisos solo lectura

Ejecutar en PostgreSQL para crear el usuario readonly que usará el Analytics Service:

```sql
-- Crear usuario de solo lectura para el Analytics Service
CREATE USER analytics_readonly WITH PASSWORD 'password_seguro';

-- Dar acceso a la base de datos
GRANT CONNECT ON DATABASE tallersoft TO analytics_readonly;

-- Dar permisos de SELECT sobre todas las tablas del schema public
GRANT USAGE ON SCHEMA public TO analytics_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_readonly;

-- Para tablas que se creen en el futuro
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT ON TABLES TO analytics_readonly;
```

---

## Checklist final

Antes de dar el sprint por terminado, verificar que todos estos puntos estén completos:

### Infraestructura
- [ ] `analytics/.env.example` creado con todas las variables necesarias
- [ ] `analytics/app/config.py` valida variables al iniciar
- [ ] `analytics/app/db/database.py` previene escrituras a nivel de aplicación
- [ ] Usuario `analytics_readonly` creado en PostgreSQL con solo `SELECT`

### Endpoints
- [ ] `GET /analytics/ordenes/resumen` responde con estructura correcta
- [ ] `GET /analytics/ordenes/por-periodo` respeta parámetros `agrupacion` y `meses_atras`
- [ ] `GET /analytics/ordenes/tecnicos/rendimiento` devuelve datos por técnico
- [ ] `GET /analytics/stock/critico` devuelve solo repuestos bajo mínimo
- [ ] `GET /analytics/stock/mas-usados` respeta parámetro `top`
- [ ] `GET /analytics/caja/resumen-diario` funciona con y sin parámetro `fecha`
- [ ] `GET /analytics/caja/evolucion-mensual` respeta parámetro `meses`
- [ ] `POST /analytics/asistente/consulta` devuelve respuesta del asistente IA

### Asistente IA
- [ ] `groq_service.py` usa las variables de entorno (no hardcoded)
- [ ] El system prompt respeta las reglas definidas en el README
- [ ] El contexto incluye: órdenes, stock crítico, caja del día, rendimiento de técnicos
- [ ] Errores de la API de Groq se capturan y devuelven un 500 descriptivo

### Gateway
- [ ] La ruta `/analytics/**` está configurada en Spring Cloud Gateway
- [ ] Los requests sin JWT son rechazados por el Gateway (401)

### Frontend
- [ ] `AnalyticsService` en Angular consume todos los endpoints
- [ ] Dashboard muestra los 4 KPIs principales
- [ ] Gráfico de evolución mensual renderiza con ApexCharts
- [ ] Módulo Asistente permite enviar preguntas y muestra respuestas
- [ ] El chat maneja el estado de carga y errores

### Testing
- [ ] Tests de todos los routers cubren caso exitoso y validaciones
- [ ] Tests del asistente usan mock de la API de Groq
- [ ] `pytest tests/` pasa sin errores

### Docker
- [ ] `analytics/Dockerfile` construye la imagen correctamente
- [ ] `docker compose up --build` levanta todos los servicios sin errores
- [ ] El servicio responde en `http://localhost:8082/health`
- [ ] Swagger disponible en `http://localhost:8082/docs`

---

*Plan generado para TallerSoft — Trabajo Final Integrador — UTN FRC 2026*