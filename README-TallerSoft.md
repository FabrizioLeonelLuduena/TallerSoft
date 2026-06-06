# TallerSoft 🔧

> Sistema de Gestión Integral para Talleres de Servicio Técnico con Asistente IA integrado.

Plataforma web ERP liviana orientada a PYMEs del rubro de servicios técnicos (electrónica, computadoras, celulares, electrodomésticos). Digitaliza y centraliza los procesos operativos del taller —órdenes de trabajo, stock de repuestos, clientes, técnicos, caja y facturación— incorporando un asistente conversacional con IA capaz de consultar datos en tiempo real.

---

## Índice

- [Arquitectura general](#arquitectura-general)
- [Stack tecnológico](#stack-tecnológico)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Requisitos previos](#requisitos-previos)
- [Configuración del entorno](#configuración-del-entorno)
- [Variables de entorno](#variables-de-entorno)
- [Base de datos](#base-de-datos)
- [Módulos del sistema](#módulos-del-sistema)
- [Microservicio de Analítica (Python)](#microservicio-de-analítica-python)
- [Asistente IA](#asistente-ia)
- [Pasarela de pagos](#pasarela-de-pagos)
- [Autenticación y roles](#autenticación-y-roles)
- [Endpoints principales](#endpoints-principales)
- [Ejecución del proyecto](#ejecución-del-proyecto)
- [Testing](#testing)
- [Deploy](#deploy)
- [Planificación de sprints](#planificación-de-sprints)

---

## Arquitectura general

El sistema está compuesto por tres capas principales que se comunican entre sí:

```
┌─────────────────────────────────────────────┐
│             FRONTEND (Angular)              │
│         Angular 18 PWA                      │
└────────────────┬────────────────────────────┘
                 │ HTTP/REST (JSON)
┌────────────────▼────────────────────────────┐
│         API GATEWAY (Spring Cloud)          │
│  Enruta, autentica y valida JWT             │
│  Puerto: 8080                               │
└──────────┬──────────────┬───────────────────┘
           │              │
┌──────────▼──────┐  ┌────▼──────────────────┐
│  CORE SERVICE   │  │  ANALYTICS SERVICE     │
│  Spring Boot 3  │  │  Python 3.11 + FastAPI │
│  Puerto: 8081   │  │  Puerto: 8082          │
│                 │  │  Solo lectura sobre BD │
└──────────┬──────┘  └────┬──────────────────┘
           │              │
┌──────────▼──────────────▼───────────────────┐
│           PostgreSQL 16                     │
│           Puerto: 5432                      │
└─────────────────────────────────────────────┘
```

### Reglas de arquitectura

- El **API Gateway** es el único punto de entrada desde el frontend. Nunca llamar directamente al Core Service ni al Analytics Service.
- El **Analytics Service** tiene permisos de **solo lectura** sobre la base de datos. Nunca escribe datos.
- La comunicación entre servicios es **REST/JSON**. No usar RPC ni GraphQL.
- El **frontend** nunca almacena tokens en `localStorage`. Usar `sessionStorage` o cookies `httpOnly`.
- Cada microservicio tiene su propio archivo `.env` y no comparte configuración directamente.

---

## Stack tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Frontend | Angular | 18.2 |
| UI Components | Angular Material + CDK | 18.2 |
| Backend principal | Java + Spring Boot | 21 / 3.2.5 |
| Seguridad | Spring Security + JWT | 3.2.5 |
| ORM | Spring Data JPA + Hibernate | 3.2.5 |
| API Gateway | Spring Cloud Gateway | 2024.0.1 |
| Microservicio analítica | Python + FastAPI | 3.11 / latest |
| Análisis de datos | Pandas + SQLAlchemy | latest |
| Base de datos | PostgreSQL | 16 |
| Asistente IA | Groq API + llama-3.3-70b-versatile | 0.9.0 |
| Pagos | MercadoPago API | latest |
| Exportación PDF | iText | 7.x |
| Build backend | Maven | 3.9+ |
| Contenedores | Docker + Docker Compose | latest |
| Testing backend | JUnit 5 + Mockito | - |
| Testing analytics | pytest + httpx | 8.x |
| Testing frontend | Jasmine + Karma | - |
| Testing API | Postman | - |

---

## Estructura del repositorio

```
tallersoft/
├── frontend/                        # Aplicación Angular
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/                # Guards, interceptors, servicios singleton
│   │   │   │   ├── auth/            # AuthService, JwtInterceptor, AuthGuard
│   │   │   │   └── services/        # Servicios HTTP globales
│   │   │   ├── shared/              # Componentes reutilizables (botones, tablas, modales)
│   │   │   ├── modules/
│   │   │   │   ├── auth/            # Login, registro
│   │   │   │   ├── dashboard/       # Panel principal con KPIs
│   │   │   │   ├── ordenes/         # Órdenes de trabajo (Kanban + listado)
│   │   │   │   ├── clientes/        # ABM de clientes y equipos
│   │   │   │   ├── stock/           # Gestión de repuestos
│   │   │   │   ├── caja/            # Cobros, presupuestos y caja diaria
│   │   │   │   └── asistente/       # Chat con el asistente IA
│   │   │   └── app.routes.ts
│   │   ├── assets/
│   │   └── environments/
│   │       ├── environment.ts       # Desarrollo
│   │       └── environment.prod.ts  # Producción
│   ├── angular.json
│   └── package.json
│
├── backend/                         # Spring Boot — Core Service
│   ├── src/main/java/com/tallersoft/
│   │   ├── config/                  # SecurityConfig, JwtConfig, CorsConfig
│   │   ├── controller/              # REST Controllers por módulo
│   │   ├── service/                 # Lógica de negocio
│   │   ├── repository/              # Interfaces JPA
│   │   ├── model/                   # Entidades JPA
│   │   ├── dto/                     # Data Transfer Objects (request y response)
│   │   ├── mapper/                  # MapStruct mappers (entidad ↔ DTO)
│   │   ├── security/                # JwtUtil, JwtFilter, UserDetailsServiceImpl
│   │   ├── exception/               # GlobalExceptionHandler, excepciones custom
│   │   └── TallerSoftApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml          # Config principal
│   │   └── application-prod.yml     # Config producción
│   └── pom.xml
│
├── gateway/                         # Spring Cloud Gateway
│   ├── src/main/java/com/tallersoft/gateway/
│   │   └── GatewayApplication.java
│   ├── src/main/resources/
│   │   └── application.yml          # Rutas y filtros del gateway
│   └── pom.xml
│
├── analytics/                       # Microservicio Python
│   ├── app/
│   │   ├── main.py                  # Entry point FastAPI
│   │   ├── config.py                # Settings (pydantic-settings)
│   │   ├── routers/                 # Endpoints por dominio
│   │   │   ├── ordenes.py
│   │   │   ├── stock.py
│   │   │   ├── caja.py
│   │   │   └── asistente.py         # Endpoint del chat IA
│   │   ├── services/
│   │   │   ├── analytics_service.py # Lógica de análisis con Pandas
│   │   │   ├── alertas_service.py   # Alertas operativas (stock crítico, prioridad)
│   │   │   ├── groq_service.py      # Integración con Groq API (implementación activa)
│   │   │   └── claude_service.py    # Integración con Anthropic API (referencia alternativa)
│   │   ├── db/
│   │   │   └── database.py          # Conexión SQLAlchemy (solo lectura)
│   │   └── schemas/                 # Pydantic models
│   ├── tests/                       # Suite de tests pytest
│   │   ├── conftest.py              # SQLite in-memory + TestClient
│   │   ├── test_ordenes.py
│   │   ├── test_stock.py
│   │   ├── test_caja.py
│   │   ├── test_asistente.py
│   │   └── test_claude_service.py
│   ├── requirements.txt
│   └── Dockerfile
│
├── documentacion/                   # Documentación técnica completa (Sprint 6)
│   ├── README.md                    # Índice y guía de lectura por rol
│   ├── ARQUITECTURA.md              # Diagramas, flujos, decisiones de diseño
│   ├── BACKEND.md                   # Core Service: JWT, módulos, endpoints
│   ├── FRONTEND.md                  # Angular PWA: módulos, guards, Kanban
│   ├── MICROSERVICIO_ANALYTICS.md   # FastAPI Python: endpoints y asistente IA
│   ├── BASE_DE_DATOS.md             # Esquema, índices, DDL completo, seed
│   ├── API_ENDPOINTS.md             # Referencia completa de endpoints (estilo Postman)
│   ├── DEPLOY.md                    # Variables de entorno, Docker, checklist
│   ├── TESTING.md                   # Estrategia, cómo correr tests, deuda técnica
│   └── *.sql                        # Scripts SQL (init, seed, migraciones)
│
├── scripts/
│   └── run_tests.sh                 # Corre los tres suites de tests (mvn + pytest + ng)
│
├── docker-compose.yml               # Orquestación completa
├── docker-compose.dev.yml           # Solo servicios de infraestructura (BD)
├── .env.example                     # Variables de entorno de ejemplo
└── README.md
```

---

## Requisitos previos

Instalar las siguientes herramientas antes de iniciar:

```bash
# Java 21
java -version   # debe mostrar openjdk 21

# Maven 3.9+
mvn -version

# Node.js 20+ y Angular CLI
node -version
npm install -g @angular/cli
ng version

# Python 3.11+
python --version

# Docker y Docker Compose
docker -version
docker compose version

# PostgreSQL 16 (opcional si se usa Docker)
psql --version
```

---

## Configuración del entorno

### 1. Clonar el repositorio

```bash
git clone https://github.com/TU_USUARIO/tallersoft.git
cd tallersoft
```

### 2. Copiar variables de entorno

```bash
cp .env.example .env
# Editar .env con tus credenciales reales
```

### 3. Levantar la base de datos con Docker

```bash
# Solo la BD para desarrollo local
docker compose -f docker-compose.dev.yml up -d
```

### 4. Instalar dependencias del frontend

```bash
cd frontend
npm install
```

### 5. Instalar dependencias del microservicio Python

```bash
cd analytics
pip install -r requirements.txt
```

---

## Variables de entorno

Crear un archivo `.env` en la raíz del proyecto basado en `.env.example`:

```env
# ── Base de datos ──────────────────────────────
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tallersoft
DB_USER=postgres
DB_PASSWORD=tu_password_seguro

# ── JWT ────────────────────────────────────────
JWT_SECRET=clave_secreta_minimo_256_bits
JWT_EXPIRATION_MS=86400000

# ── Groq API (asistente IA) ───────────────────
GROQ_API_KEY=gsk_xxxxxxxxxxxx
GROQ_MODEL=llama-3.3-70b-versatile
GROQ_MAX_TOKENS=1024

# ── MercadoPago ────────────────────────────────
MP_ACCESS_TOKEN=APP_USR-xxxxxxxxxxxx
MP_PUBLIC_KEY=APP_USR-xxxxxxxxxxxx
MP_WEBHOOK_SECRET=tu_webhook_secret

# ── Puertos de servicios ───────────────────────
GATEWAY_PORT=8080
BACKEND_PORT=8081
ANALYTICS_PORT=8082
FRONTEND_PORT=4200
```

> ⚠️ Nunca commitear el archivo `.env` al repositorio. Está incluido en `.gitignore`.

---

## Base de datos

### Modelo de datos principal

```sql
-- Usuarios del sistema (empleados del taller)
CREATE TABLE usuarios (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    email       VARCHAR(150) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,          -- BCrypt hash
    rol         VARCHAR(20) NOT NULL,           -- ADMIN | TECNICO | RECEPCION
    activo      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Clientes del taller
CREATE TABLE clientes (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    telefono    VARCHAR(20),
    email       VARCHAR(150),
    direccion   VARCHAR(200),
    activo      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Equipos asociados a un cliente
CREATE TABLE equipos (
    id          BIGSERIAL PRIMARY KEY,
    cliente_id  BIGINT REFERENCES clientes(id),
    tipo        VARCHAR(50) NOT NULL,           -- Celular | PC | Electrodoméstico | etc.
    marca       VARCHAR(50),
    modelo      VARCHAR(100),
    numero_serie VARCHAR(100),
    observaciones TEXT
);

-- Órdenes de trabajo
CREATE TABLE ordenes_trabajo (
    id              BIGSERIAL PRIMARY KEY,
    equipo_id       BIGINT REFERENCES equipos(id),
    cliente_id      BIGINT REFERENCES clientes(id),
    tecnico_id      BIGINT REFERENCES usuarios(id),
    falla_reportada TEXT NOT NULL,
    diagnostico     TEXT,
    estado          VARCHAR(20) DEFAULT 'PENDIENTE', -- PENDIENTE | EN_PROCESO | LISTO | ENTREGADO
    prioridad       VARCHAR(10) DEFAULT 'NORMAL',    -- BAJA | NORMAL | ALTA
    presupuesto     NUMERIC(10,2),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

-- Repuestos utilizados en una orden
CREATE TABLE orden_repuestos (
    id          BIGSERIAL PRIMARY KEY,
    orden_id    BIGINT REFERENCES ordenes_trabajo(id),
    repuesto_id BIGINT REFERENCES repuestos(id),
    cantidad    INTEGER NOT NULL,
    precio_unit NUMERIC(10,2) NOT NULL
);

-- Stock de repuestos
CREATE TABLE repuestos (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    categoria       VARCHAR(80),
    precio          NUMERIC(10,2) NOT NULL,
    stock_actual    INTEGER DEFAULT 0,
    stock_minimo    INTEGER DEFAULT 5,         -- Umbral para alertas
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Registro de cobros
CREATE TABLE cobros (
    id              BIGSERIAL PRIMARY KEY,
    orden_id        BIGINT REFERENCES ordenes_trabajo(id),
    monto           NUMERIC(10,2) NOT NULL,
    medio_pago      VARCHAR(20) NOT NULL,      -- EFECTIVO | TARJETA | MERCADOPAGO
    estado_pago     VARCHAR(20) DEFAULT 'PENDIENTE', -- PENDIENTE | APROBADO | RECHAZADO
    mp_payment_id   VARCHAR(100),              -- ID de pago en MercadoPago (si aplica)
    created_at      TIMESTAMP DEFAULT NOW()
);
```

---

## Módulos del sistema

### Módulo 1: Autenticación y Roles

**Roles disponibles:**

| Rol | Permisos |
|---|---|
| `ADMIN` | Acceso total: usuarios, reportes, configuración, dashboard |
| `TECNICO` | Ver y gestionar sus propias órdenes, registrar diagnósticos y repuestos |
| `RECEPCION` | Crear órdenes, gestionar clientes, registrar cobros y presupuestos |

**Flujo de autenticación:**
1. El usuario envía `email` y `password` al endpoint `/auth/login`.
2. El backend valida credenciales y devuelve un JWT con el rol incluido en los claims.
3. El frontend almacena el token en `sessionStorage` y lo adjunta en el header `Authorization: Bearer {token}` en cada request.
4. El API Gateway valida el JWT antes de reenviar cualquier request al Core Service.

**Reglas importantes para Copilot:**
- Las contraseñas siempre se almacenan con hash BCrypt. Nunca en texto plano.
- El JWT debe incluir en el payload: `userId`, `email`, `rol`, `iat`, `exp`.
- Usar `@PreAuthorize("hasRole('ADMIN')")` en Spring para proteger endpoints por rol.
- El frontend debe implementar un `AuthGuard` que redirija a `/login` si no hay token válido.
- Implementar un `JwtInterceptor` en Angular que adjunte el token automáticamente a todos los requests HTTP.

---

### Módulo 2: Clientes y Equipos

**Entidades:** `Cliente`, `Equipo`

**Funcionalidades:**
- ABM completo de clientes (crear, editar, listar, buscar, desactivar)
- Cada cliente puede tener múltiples equipos asociados
- Búsqueda de clientes por nombre o teléfono con debounce en el frontend
- Vista de historial: al seleccionar un cliente, mostrar todas sus órdenes anteriores

**Reglas importantes para Copilot:**
- La búsqueda de clientes debe usar `ILIKE` en PostgreSQL para ser case-insensitive.
- No eliminar clientes físicamente. Usar baja lógica con campo `activo = false`.
- Al crear una orden, el flujo es: buscar/crear cliente → registrar equipo → crear orden.

---

### Módulo 3: Órdenes de Trabajo

Este es el módulo central del sistema. Representa el ciclo de vida completo de una reparación.

**Estados de una orden:**

```
PENDIENTE → EN_PROCESO → LISTO → ENTREGADO
```

**Funcionalidades:**
- Crear orden asociando cliente, equipo y falla reportada
- Asignar técnico responsable
- Actualizar estado y registrar diagnóstico
- Registrar repuestos utilizados (descuenta stock automáticamente)
- Vista Kanban con columnas por estado (drag & drop)
- Vista de listado con filtros por estado, técnico y fecha

**Reglas importantes para Copilot:**
- Al cambiar el estado a `LISTO`, verificar que exista un diagnóstico cargado. Si no hay diagnóstico, rechazar el cambio con un error descriptivo.
- Al registrar repuestos en una orden, descontar el stock en la tabla `repuestos` dentro de la misma transacción (`@Transactional`). Si no hay stock suficiente, lanzar excepción y hacer rollback.
- El campo `updated_at` debe actualizarse automáticamente en cada modificación usando `@PreUpdate` en JPA.
- En el frontend, el tablero Kanban debe mostrar únicamente las órdenes activas (estado distinto de `ENTREGADO`).

---

### Módulo 4: Stock de Repuestos

**Entidades:** `Repuesto`, `OrdenRepuesto`

**Funcionalidades:**
- ABM de repuestos con nombre, categoría, precio y stock
- Descuento automático de stock al asociar repuestos a una orden
- Alerta visual cuando `stock_actual <= stock_minimo`
- Listado de repuestos con stock crítico en el dashboard

**Reglas importantes para Copilot:**
- Nunca permitir que `stock_actual` quede en valor negativo. Validar antes de descontar.
- La alerta de stock mínimo es visual en el frontend (badge rojo). No envía notificaciones externas en esta versión.
- El descuento de stock debe ocurrir en la misma transacción que el registro del repuesto en la orden.

---

### Módulo 5: Caja y Facturación

**Entidades:** `Cobro`

**Medios de pago soportados:**

| Medio | Flujo |
|---|---|
| `EFECTIVO` | El recepcionista ingresa el monto. El sistema calcula el vuelto. Estado cambia a `APROBADO` inmediatamente. |
| `TARJETA` | El recepcionista confirma manualmente que el posnet aprobó. Estado cambia a `APROBADO` inmediatamente. |
| `MERCADOPAGO` | El sistema genera un QR o link de pago. El estado queda en `PENDIENTE` hasta recibir el webhook de confirmación. |

**Funcionalidades:**
- Generar presupuesto en PDF a partir de una orden (con iText)
- Registrar cobro por cualquiera de los tres medios
- Vista de caja diaria: total ingresado, cantidad de órdenes cobradas, desglose por medio de pago
- Al registrar un cobro aprobado, el estado de la orden pasa automáticamente a `ENTREGADO`

**Reglas importantes para Copilot:**
- El endpoint de webhook de MercadoPago es público (sin JWT) pero debe validar la firma `x-signature` del header para evitar fraudes.
- El PDF del presupuesto debe incluir: datos del taller, datos del cliente, detalle del equipo, descripción de la reparación, repuestos con precios y total.
- Un cobro en estado `APROBADO` no puede modificarse ni eliminarse.

---

## Microservicio de Analítica (Python)

### Descripción

Microservicio independiente desarrollado en Python que se conecta a la misma base de datos PostgreSQL con permisos de **solo lectura**. Expone endpoints REST que el frontend y el asistente IA consumen para obtener datos analíticos procesados.

### Endpoints del Analytics Service

```
GET  /analytics/ordenes/resumen          → Totales por estado
GET  /analytics/ordenes/por-periodo      → Órdenes agrupadas por semana/mes
GET  /analytics/tecnicos/rendimiento     → Órdenes cerradas por técnico
GET  /analytics/stock/critico            → Repuestos bajo stock mínimo
GET  /analytics/stock/mas-usados         → Top repuestos más utilizados
GET  /analytics/caja/resumen-diario      → Ingresos del día por medio de pago
GET  /analytics/caja/evolucion-mensual   → Ingresos mes a mes
POST /analytics/asistente/consulta       → Endpoint del chat IA
```

### Estructura del archivo `main.py`

```python
from fastapi import FastAPI
from app.routers import ordenes, stock, caja, asistente
from app.db.database import engine
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(title="TallerSoft Analytics API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],  # Solo el gateway
    allow_methods=["GET", "POST"],
    allow_headers=["Authorization"],
)

app.include_router(ordenes.router, prefix="/analytics/ordenes")
app.include_router(stock.router, prefix="/analytics/stock")
app.include_router(caja.router, prefix="/analytics/caja")
app.include_router(asistente.router, prefix="/analytics/asistente")
```

### Conexión a la base de datos (solo lectura)

```python
# app/db/database.py
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
import os

DATABASE_URL = (
    f"postgresql://{os.getenv('DB_USER')}:{os.getenv('DB_PASSWORD')}"
    f"@{os.getenv('DB_HOST')}:{os.getenv('DB_PORT')}/{os.getenv('DB_NAME')}"
)

# El usuario de BD debe tener permisos SOLO de SELECT
engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
```

### `requirements.txt`

```
fastapi==0.111.0
uvicorn[standard]==0.30.0
sqlalchemy==2.0.30
psycopg2-binary==2.9.9
pandas==2.2.2
groq==0.9.0
python-dotenv==1.0.1
pydantic==2.7.1
pydantic-settings==2.3.4
pytest==8.2.2
httpx==0.27.0
pytest-asyncio==0.23.7
```

---

## Asistente IA

### Descripción

El asistente es un chat en lenguaje natural que responde preguntas sobre los datos del taller. Funciona enriqueciendo cada mensaje del usuario con contexto real de la base de datos antes de enviarlo a la API de Claude.

### Flujo de una consulta

```
Usuario escribe pregunta en el chat
          ↓
Frontend POST /analytics/asistente/consulta { "pregunta": "..." }
          ↓
analytics/asistente.py recopila contexto:
  - resumen de órdenes (totales por estado)
  - repuestos críticos
  - resumen de caja del día
  - rendimiento de técnicos
          ↓
Se construye el prompt enriquecido con el contexto
          ↓
Se llama a la Groq API (llama-3.3-70b-versatile)
          ↓
Se devuelve la respuesta al frontend
```

### Implementación del servicio (Groq)

El asistente usa la API de **Groq** con el modelo `llama-3.3-70b-versatile` como implementación activa. Existe también `claude_service.py` como referencia alternativa con la API de Anthropic.

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
- Si los datos no son suficientes para responder, decílo con claridad.
- Podés hacer cálculos simples con los datos disponibles (promedios, porcentajes, comparaciones).
- Nunca menciones detalles técnicos de implementación al usuario.
"""

def consultar_asistente(pregunta: str, contexto: dict) -> str:
    contexto_texto = _construir_contexto_texto(contexto)  # formatea el dict en texto estructurado

    chat = _client.chat.completions.create(
        model=settings.groq_model,
        max_tokens=settings.groq_max_tokens,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"{contexto_texto}\n\nPREGUNTA: {pregunta}"},
        ],
    )

    return chat.choices[0].message.content
```

### Endpoint del chat

```python
# app/routers/asistente.py
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.db.database import get_db
from app.services.groq_service import consultar_asistente
from app.services.analytics_service import obtener_contexto_taller
from pydantic import BaseModel

router = APIRouter()

class ConsultaRequest(BaseModel):
    pregunta: str

class ConsultaResponse(BaseModel):
    respuesta: str
    contexto_utilizado: dict

@router.post("/consulta", response_model=ConsultaResponse)
def chat_asistente(request: ConsultaRequest, db: Session = Depends(get_db)):
    try:
        contexto = obtener_contexto_taller(db)
    except Exception:
        raise HTTPException(status_code=500, detail="Error al obtener el contexto del taller")
    try:
        respuesta = consultar_asistente(request.pregunta, contexto)
        return ConsultaResponse(respuesta=respuesta, contexto_utilizado=contexto)
    except Exception:
        mensaje_fallback = "Lo siento, el asistente no está disponible en este momento. Intentá de nuevo en unos segundos."
        return ConsultaResponse(respuesta=mensaje_fallback, contexto_utilizado=contexto)
```

---

## Pasarela de pagos

### MercadoPago — Integración

TallerSoft integra MercadoPago únicamente para el medio de pago `MERCADOPAGO`. Los medios `EFECTIVO` y `TARJETA` se gestionan de forma manual sin integración externa.

### Generar QR / Link de pago (backend Java)

```java
// Agregar dependencia en pom.xml:
// <dependency>
//   <groupId>com.mercadopago</groupId>
//   <artifactId>sdk-java</artifactId>
//   <version>2.1.7</version>
// </dependency>

@Service
public class MercadoPagoService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    public String generarLinkPago(Long ordenId, BigDecimal monto, String descripcion) {
        MercadoPagoConfig.setAccessToken(accessToken);

        PreferenceItemRequest item = PreferenceItemRequest.builder()
            .title(descripcion)
            .quantity(1)
            .unitPrice(monto)
            .build();

        PreferenceRequest request = PreferenceRequest.builder()
            .items(List.of(item))
            .externalReference(String.valueOf(ordenId)) // ID de la orden para identificar el pago
            .notificationUrl("https://TU_DOMINIO/api/pagos/webhook")
            .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(request);

        return preference.getInitPoint(); // Link de pago
        // Para QR usar: preference.getSandboxInitPoint() en desarrollo
    }
}
```

### Recibir webhook de confirmación

```java
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    // Este endpoint NO lleva @PreAuthorize — es público
    // pero valida la firma del webhook de MercadoPago
    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirWebhook(
        @RequestParam String type,
        @RequestParam("data.id") String paymentId,
        @RequestHeader("x-signature") String signature,
        @RequestHeader("x-request-id") String requestId
    ) {
        // 1. Validar firma con HMAC-SHA256 usando MP_WEBHOOK_SECRET
        webhookValidator.validar(signature, requestId, paymentId);

        // 2. Si el tipo es "payment" y el estado es "approved", actualizar cobro
        if ("payment".equals(type)) {
            pagoService.procesarPagoAprobado(paymentId);
            // pagoService internamente:
            // - Busca el cobro por mp_payment_id o external_reference
            // - Cambia estado_pago a APROBADO
            // - Cambia estado de la orden a ENTREGADO
        }

        return ResponseEntity.ok().build();
    }
}
```

---

## Autenticación y roles

### Configuración Spring Security

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/api/pagos/webhook").permitAll() // Webhook público
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### Uso de roles en controllers

```java
// Solo ADMIN puede ver todos los usuarios
@GetMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public List<UsuarioDTO> listarUsuarios() { ... }

// ADMIN y RECEPCION pueden crear clientes
@PostMapping("/clientes")
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPCION')")
public ClienteDTO crearCliente(@RequestBody ClienteRequest request) { ... }

// TECNICO solo puede ver sus propias órdenes
@GetMapping("/ordenes/mis-ordenes")
@PreAuthorize("hasRole('TECNICO')")
public List<OrdenDTO> misOrdenes(Authentication auth) { ... }
```

---

## Endpoints principales

### Auth

```
POST   /auth/login              → Retorna JWT
POST   /auth/register           → Crear usuario (solo ADMIN)
```

### Clientes

```
GET    /api/clientes            → Listar con filtros (?nombre=&telefono=)
GET    /api/clientes/{id}       → Obtener cliente con historial
POST   /api/clientes            → Crear cliente
PUT    /api/clientes/{id}       → Editar cliente
DELETE /api/clientes/{id}       → Baja lógica
```

### Equipos

```
GET    /api/equipos/cliente/{clienteId}   → Equipos de un cliente
POST   /api/equipos                       → Crear equipo
PUT    /api/equipos/{id}                  → Editar equipo
```

### Órdenes de trabajo

```
GET    /api/ordenes             → Listar (?estado=&tecnicoId=&desde=&hasta=)
GET    /api/ordenes/{id}        → Detalle de una orden
POST   /api/ordenes             → Crear orden
PUT    /api/ordenes/{id}/estado → Cambiar estado
PUT    /api/ordenes/{id}        → Editar orden
POST   /api/ordenes/{id}/repuestos → Agregar repuesto (descuenta stock)
```

### Stock

```
GET    /api/repuestos           → Listar (?critico=true para solo alertas)
GET    /api/repuestos/{id}      → Detalle
POST   /api/repuestos           → Crear repuesto
PUT    /api/repuestos/{id}      → Editar repuesto
```

### Caja y pagos

```
POST   /api/cobros              → Registrar cobro (EFECTIVO | TARJETA | MERCADOPAGO)
GET    /api/cobros/caja-diaria  → Resumen del día
POST   /api/cobros/{id}/confirmar → Confirmar pago manual (EFECTIVO o TARJETA)
POST   /api/pagos/webhook       → Webhook MercadoPago (público)
```

### Analítica (Analytics Service — puerto 8082, ruteado por Gateway)

```
GET    /analytics/ordenes/resumen
GET    /analytics/ordenes/por-periodo
GET    /analytics/tecnicos/rendimiento
GET    /analytics/stock/critico
GET    /analytics/stock/mas-usados
GET    /analytics/caja/resumen-diario
GET    /analytics/caja/evolucion-mensual
POST   /analytics/asistente/consulta
```

---

## Ejecución del proyecto

### Desarrollo local completo

```bash
# 1. Levantar solo la BD con Docker
docker compose -f docker-compose.dev.yml up -d

# 2. Iniciar el Core Service (Spring Boot)
cd backend
mvn spring-boot:run

# 3. Iniciar el Gateway (Spring Cloud)
cd gateway
mvn spring-boot:run

# 4. Iniciar el Analytics Service (Python)
cd analytics
uvicorn app.main:app --reload --port 8082

# 5. Iniciar el Frontend (Angular)
cd frontend
ng serve
```

### Con Docker Compose (entorno completo)

```bash
docker compose up --build
```

### URLs en desarrollo

| Servicio | URL |
|---|---|
| Frontend Angular | http://localhost:4200 |
| API Gateway | http://localhost:8080 |
| Core Service | http://localhost:8081 |
| Analytics Service | http://localhost:8082 |
| Swagger Core | http://localhost:8081/swagger-ui.html |
| Swagger Analytics | http://localhost:8082/docs |

---

## Testing

Para la estrategia completa, casos de prueba manuales y deuda técnica ver [documentacion/TESTING.md](documentacion/TESTING.md).

### Correr todos los tests de una vez

```bash
# Script unificado (requiere backend, analytics y frontend instalados)
bash scripts/run_tests.sh
```

### Backend (JUnit 5 + Mockito)

```bash
cd backend
mvn test                                       # Todos los tests
mvn test -Dtest=CajaServiceTest                # Una sola clase
mvn verify                                     # Tests + integración
```

**Suite de tests unitarios:** `AuthServiceTest`, `UsuarioServiceTest`, `ClienteServiceTest`, `OrdenTrabajoServiceTest`, `RepuestoServiceTest`, `CajaServiceTest`, `MercadoPagoServiceTest`, `JwtUtilTest`

**Tests de integración:** `AuthControllerIntegrationTest`, `WebhookControllerIntegrationTest`

**Convenciones:**
- Patrón `// Arrange / Act / Assert` en cada test
- `@ExtendWith(MockitoExtension.class)` en unitarios; `@SpringBootTest + @AutoConfigureMockMvc` en integración
- Nombre de método: `metodo_escenario_resultadoEsperado()`
- Mínimo dos tests por método de servicio: caso exitoso y caso de error

### Analytics (pytest)

```bash
cd analytics
source .venv/bin/activate
python -m pytest tests/ -v --tb=short         # Todos los tests
python -m pytest tests/test_ordenes.py -v     # Un archivo
python -m pytest tests/ --cov=app             # Con cobertura
```

**Suite:** `test_ordenes.py`, `test_stock.py`, `test_caja.py`, `test_asistente.py`, `test_claude_service.py`

`conftest.py` configura SQLite in-memory con `StaticPool` para correr sin PostgreSQL.

### Frontend (Jasmine + Karma)

```bash
cd frontend
ng test                                        # Modo watch
ng test --watch=false --browsers=ChromeHeadless  # Una sola ejecución (CI)
ng test --code-coverage                        # Reporte en coverage/index.html
```

**Suite:** `auth.service.spec.ts`, `ordenes.service.spec.ts`, `jwt.interceptor.spec.ts`, `auth.guard.spec.ts`, `kanban.component.spec.ts`, `cobrar-orden.component.spec.ts`, `repuesto.service.spec.ts`

### API (Postman)

Crear un environment `TallerSoft Local` con `baseUrl = http://localhost:8080` y usar la referencia completa de endpoints en [documentacion/API_ENDPOINTS.md](documentacion/API_ENDPOINTS.md).

---

## Deploy

### Docker Compose — Producción

```yaml
# docker-compose.yml
version: '3.9'

services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  backend:
    build: ./backend
    environment:
      DB_HOST: db
      DB_PORT: 5432
      DB_NAME: ${DB_NAME}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      MP_ACCESS_TOKEN: ${MP_ACCESS_TOKEN}
      MP_WEBHOOK_SECRET: ${MP_WEBHOOK_SECRET}
    depends_on:
      - db
    ports:
      - "8081:8081"

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
    depends_on:
      - db
    ports:
      - "8082:8082"

  gateway:
    build: ./gateway
    environment:
      BACKEND_URL: http://backend:8081
      ANALYTICS_URL: http://analytics:8082
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      - backend
      - analytics
    ports:
      - "8080:8080"

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - gateway

volumes:
  postgres_data:
```

### Deploy en Railway o Render

1. Crear un proyecto nuevo en [Railway](https://railway.app) o [Render](https://render.com)
2. Conectar el repositorio de GitHub
3. Agregar las variables de entorno del `.env` en el panel de la plataforma
4. Configurar el build command por servicio según el `Dockerfile` de cada carpeta
5. Railway detecta Docker Compose automáticamente

---

## Planificación de sprints

| Sprint | Duración | Contenido | Estado |
|---|---|---|---|
| **Sprint 0** | 1 semana | Entorno de desarrollo, configuración de herramientas, spikes técnicos | ✅ Completado |
| **Sprint 1** | 2 semanas | Autenticación + JWT + Roles + Módulo Clientes | ✅ Completado |
| **Sprint 2** | 2 semanas | Módulo Órdenes de Trabajo (Kanban, estados, repuestos) | ✅ Completado |
| **Sprint 3** | 2 semanas | Stock de Repuestos + Caja + Medios de pago (Efectivo, Tarjeta, MercadoPago) | ✅ Completado |
| **Sprint 4** | 2 semanas | Microservicio Python + Dashboard con KPIs + Alertas | ✅ Completado |
| **Sprint 5** | 2 semanas | Asistente IA conversacional (Groq) | ✅ Completado |
| **Sprint 6** | 2 semanas | Testing (JUnit + pytest + Jasmine), bug fixing, documentación técnica completa | ✅ Completado |

**Duración total:** 13 semanas

---

## Convenciones del proyecto

### Nombres de ramas Git

```
main          → rama de producción, solo merge desde develop
develop       → rama de integración
feature/nombre-feature    → nuevas funcionalidades
fix/nombre-bug            → corrección de bugs
```

### Commits

Usar formato [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(ordenes): agregar cambio de estado con validación de diagnóstico
fix(stock): corregir descuento negativo al cerrar orden
docs(readme): actualizar sección de variables de entorno
test(cobros): agregar test unitario para webhook MercadoPago
```

### DTOs

- Crear DTOs separados para request y response. No exponer entidades JPA directamente en los controllers.
- Usar MapStruct para mapear entre entidades y DTOs.
- Nombrar: `ClienteRequest`, `ClienteResponse`, `OrdenRequest`, `OrdenResponse`.

---

*Trabajo Final Integrador — Tecnicatura Universitaria en Programación — UTN FRC — 2026*