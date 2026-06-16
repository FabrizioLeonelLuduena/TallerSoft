<div align="center">

# TallerSoft

**ERP inteligente para talleres de servicio técnico**

[![CI](https://github.com/412237-Luduena/TFI-TechSoft/actions/workflows/ci.yml/badge.svg)](https://github.com/412237-Luduena/TFI-TechSoft/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-18-DD0031?logo=angular&logoColor=white)](https://angular.io/)
[![Python](https://img.shields.io/badge/Python-3.11-3776AB?logo=python&logoColor=white)](https://www.python.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)

Plataforma web para la gestión integral de talleres técnicos con arquitectura de microservicios, analítica avanzada y asistente IA integrado.

</div>

---

## Descripción

**TallerSoft** es un sistema ERP liviano orientado a PYMEs del rubro de servicios técnicos (PCs, celulares, electrónica, electrodomésticos). Digitaliza y centraliza todos los procesos operativos del taller en una única plataforma moderna, segura y escalable.

| | Funcionalidad |
|---|---|
| 🗂️ | Gestión completa de clientes, equipos e historial de reparaciones |
| 🔧 | Órdenes de trabajo con flujo Kanban drag & drop y sincronización en tiempo real (WebSocket + STOMP) |
| 📦 | Control de stock con alertas de inventario crítico y lock pesimista anti-race condition |
| 💳 | Caja y facturación con soporte para Efectivo, Tarjeta y MercadoPago (QR POS + Checkout Pro) |
| 📊 | Dashboard analítico con 4 tabs de KPIs: órdenes, caja, técnicos y stock |
| 🤖 | Asistente IA conversacional conectado a datos reales del negocio (Groq + llama-3.3-70b) |
| 🔔 | Sistema de notificaciones y alertas operativas persistentes por usuario |
| 📄 | Generación de PDFs de presupuesto con iText |

---

## Arquitectura

El sistema está compuesto por tres capas que se comunican a través de un API Gateway como único punto de entrada:

```
┌──────────────────────────────────────────┐
│       FRONTEND  (Angular 18 PWA)         │
│     Angular Material 18 · CDK · STOMP    │
└───────────────────┬──────────────────────┘
                    │ HTTP/REST  +  WebSocket
┌───────────────────▼──────────────────────┐
│      API GATEWAY  (Spring Cloud)         │
│  Enruta · Valida JWT · Puerto 8080       │
│  /api/** /auth/** → Core (8081)          │
│  /analytics/**   → Analytics (8082)      │
│  /ws/**          → Core WebSocket (8081) │
└──────────┬───────────────┬───────────────┘
           │               │
┌──────────▼──────┐  ┌─────▼──────────────┐
│  CORE SERVICE   │  │  ANALYTICS SERVICE │
│  Spring Boot 3  │  │  FastAPI · Python  │
│  Puerto 8081    │  │  Puerto 8082       │
│  WebSocket STOMP│  │  Solo SELECT       │
└──────────┬──────┘  └────┬───────────────┘
           └──────┬────────┘
        ┌─────────▼──────────┐
        │    PostgreSQL 16   │
        │    Puerto 5432     │
        │  tallersoft (R/W)  │
        │  analytics_readonly│
        └────────────────────┘
```

**Reglas de arquitectura:**
- El Gateway (`:8080`) es el **único punto de entrada** desde el frontend. Nunca llamar directo a `:8081` o `:8082`.
- El Analytics Service tiene permisos de **solo lectura** sobre la base de datos.
- Los tokens JWT se almacenan en `sessionStorage`, nunca en `localStorage`.
- Los WebSockets autentican por **query param** (`?token=<jwt>`) ya que los WebSockets no soportan headers custom en el handshake.

---

## Stack Tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Frontend | Angular (Standalone Components) | 18.2 |
| UI Components | Angular Material + CDK | 18.2 |
| WebSocket cliente | @stomp/rx-stomp + sockjs-client | 2.x / 1.6 |
| Backend principal | Java + Spring Boot | 21 / 3.x |
| Seguridad | Spring Security + JWT (HS256) | 6.x |
| ORM | Spring Data JPA + Hibernate | 6.x |
| Mapeo | MapStruct | 1.5.x |
| WebSocket servidor | Spring WebSocket + STOMP | — |
| API Gateway | Spring Cloud Gateway | 2024.0.1 |
| Microservicio analítica | Python + FastAPI | 3.11 / 0.109+ |
| Análisis de datos | Pandas + SQLAlchemy | 2.x |
| Base de datos | PostgreSQL | 16 |
| Asistente IA | Groq API (llama-3.3-70b-versatile) | — |
| Pagos | MercadoPago SDK (QR POS + Checkout Pro) | 2.x |
| Exportación PDF | iText | 7.x |
| Contenedores | Docker + Docker Compose | latest |
| CI/CD | GitHub Actions | — |
| Imágenes Docker | GitHub Container Registry (GHCR) | — |
| Testing backend | JUnit 5 + Mockito | 5.x |
| Testing analytics | pytest + pytest-mock | 7+ |
| Testing frontend | Jasmine + Karma | — |

---

## Estructura del Proyecto

```
tallersoft/
├── frontend/                  # Angular 18 PWA
│   └── src/app/
│       ├── core/              # Auth, guards, interceptores, servicios globales
│       ├── modules/           # Kanban, clientes, stock, caja, asistente, usuarios
│       └── shared/            # Componentes compartidos (top-bar, chat flotante)
├── backend/                   # Core Service — Spring Boot 3
│   └── src/
│       ├── main/java/com/tallersoft/
│       │   ├── controller/    # REST controllers
│       │   ├── service/       # Lógica de negocio
│       │   ├── model/         # Entidades JPA + enums
│       │   ├── dto/           # Request/Response DTOs
│       │   ├── mapper/        # MapStruct mappers
│       │   ├── repository/    # Spring Data JPA repositories
│       │   ├── security/      # JWT, filtros, webhook validator
│       │   └── exception/     # GlobalExceptionHandler + excepciones de negocio
│       └── test/              # Suite JUnit 5 + Mockito + tests de integración
├── gateway/                   # Spring Cloud Gateway
│   └── filter/                # CorrelationIdFilter, JwtValidationFilter
├── analytics/                 # Microservicio Python — FastAPI
│   ├── app/
│   │   ├── routers/           # ordenes, stock, caja, asistente, clientes, alertas
│   │   ├── services/
│   │   │   ├── groq_service.py       # Asistente IA (Groq)
│   │   │   ├── alertas_service.py    # Alertas operativas
│   │   │   └── analytics_service.py  # Análisis con Pandas
│   │   └── db/                # Sesión SQLAlchemy, modelos ORM
│   └── tests/                 # Suite pytest (SQLite in-memory)
├── documentacion/             # Documentación técnica completa
│   ├── ARQUITECTURA.md
│   ├── BACKEND.md
│   ├── FRONTEND.md
│   ├── MICROSERVICIO_ANALYTICS.md
│   ├── BASE_DE_DATOS.md
│   ├── API_ENDPOINTS.md
│   ├── CICD.md
│   ├── DEPLOY.md
│   └── TESTING.md
├── scripts/
│   ├── init-db.sql            # DDL y usuarios de PostgreSQL
│   ├── seed-data.sql          # Datos iniciales
│   ├── migrate-alertas-leidas.sql
│   └── run_tests.sh           # Corre los tres suites de tests
├── .github/workflows/
│   ├── ci.yml                 # CI: JUnit + pytest + Karma en paralelo
│   └── cd.yml                 # CD: Build y push de imágenes a GHCR
├── docker-compose.yml
├── docker-compose.dev.yml
└── .env.example
```

---

## Módulos del Sistema

### Autenticación y Roles

JWT (HS256) con claims `userId`, `email`, `rol`. Expiración de 24 h. Tres roles disponibles:

| Rol | Acceso |
|---|---|
| `ADMIN` | Total: usuarios, dashboard, reportes, configuración |
| `TECNICO` | Sus propias órdenes, diagnósticos y repuestos |
| `RECEPCION` | Clientes, equipos, órdenes nuevas, cobros. Solo puede cancelar órdenes. |

### Órdenes de Trabajo

Flujo de estados unidireccional con tablero Kanban drag & drop (CDK):

```
PENDIENTE  →  EN_PROCESO  →  LISTO  →  ENTREGADO
    └────────────────────────────────→  CANCELADO
```

- Para pasar a `LISTO` se requiere diagnóstico cargado.
- Al registrar un cobro aprobado, la orden pasa automáticamente a `ENTREGADO`.
- La cancelación devuelve automáticamente el stock de repuestos al inventario.
- Todos los usuarios conectados ven los cambios de estado **en tiempo real** sin recargar la página (WebSocket + STOMP → topic `/topic/kanban`).

### Caja y Pagos

| Medio | Flujo |
|---|---|
| EFECTIVO | Aprobado inmediatamente, calcula vuelto |
| TARJETA | Aprobado al confirmar posnet |
| MERCADOPAGO | QR POS estático (EMVCo/BCRA) o Checkout Pro. Queda PENDIENTE hasta recibir webhook. |

El webhook de MercadoPago es público pero valida firma HMAC-SHA256. El procesamiento es **idempotente**.

### Asistente IA

Chat en lenguaje natural que enriquece cada pregunta con contexto real de la base de datos (órdenes, stock, caja, rendimiento de técnicos) antes de consultar el modelo. Implementado con la API de **Groq** (`llama-3.3-70b-versatile`). El modelo opera solo sobre los datos del contexto — nunca inventa información.

### Dashboard Analítico

4 tabs con KPIs en tiempo real servidos por el microservicio Python:

| Tab | Contenido |
|---|---|
| Órdenes | Distribución por estado, evolución mensual, tendencia de 6 meses |
| Caja | Ingresos del día/semana/mes, evolución mensual, desglose por medio de pago |
| Técnicos | Órdenes cerradas por técnico, tiempo promedio, % clientes recurrentes |
| Stock | Repuestos con stock crítico, top más usados, alertas de inventario |

### Notificaciones y Alertas

- Alertas operativas calculadas por el Analytics Service (órdenes sin movimiento, alta prioridad parada, stock crítico).
- Cada usuario puede marcar alertas como leídas; el estado persiste en la tabla `alertas_leidas`.

---

## CI/CD

El pipeline está dividido en dos workflows de GitHub Actions:

| Workflow | Cuándo | Qué hace |
|---|---|---|
| `ci.yml` — CI Tests | Push/PR a `develop` o `main` | Corre JUnit, pytest y Karma en paralelo |
| `cd.yml` — CD Docker | Push a `main` | Build y push de 4 imágenes a GHCR |

### Jobs del CI (paralelos)

```
push
 ├── test-backend  (JUnit 5 + PostgreSQL 16) ─┐
 ├── test-analytics (pytest + SQLite)         ─┼─→ all-tests-passed
 └── test-frontend  (Karma + ChromeHeadless)  ─┘
```

### Imágenes publicadas (CD)

```
ghcr.io/412237-Luduena/tallersoft-backend:latest   (+ :<sha>)
ghcr.io/412237-Luduena/tallersoft-analytics:latest (+ :<sha>)
ghcr.io/412237-Luduena/tallersoft-gateway:latest   (+ :<sha>)
ghcr.io/412237-Luduena/tallersoft-frontend:latest  (+ :<sha>)
```

---

## Inicio Rápido

### Prerrequisitos

- Docker 24+ y Docker Compose v2
- Java 21, Maven 3.9+
- Node.js 20+, Angular CLI
- Python 3.11+

### Levantar con Docker Compose (entorno completo)

```bash
# 1. Clonar
git clone https://github.com/412237-Luduena/TFI-TechSoft.git
cd TFI-TechSoft

# 2. Configurar variables de entorno
cp .env.example .env
# Editar .env con tus credenciales (JWT_SECRET, GROQ_API_KEY, MP_ACCESS_TOKEN)

# 3. Levantar todo
docker compose up --build -d

# 4. Verificar que los servicios están activos
docker compose ps
```

### Desarrollo local

```bash
# Solo la base de datos con Docker
docker compose -f docker-compose.dev.yml up -d

# Backend
cd backend && mvn spring-boot:run

# Gateway
cd gateway && mvn spring-boot:run

# Analytics
cd analytics && uvicorn app.main:app --reload --port 8082

# Frontend
cd frontend && ng serve
```

### URLs en desarrollo

| Servicio | URL |
|---|---|
| Frontend | http://localhost:4200 |
| API Gateway | http://localhost:8080 |
| Swagger Analytics | http://localhost:8082/docs |
| Swagger Backend | http://localhost:8080/swagger-ui.html |

---

## Testing

```bash
# Correr los tres suites de una vez
bash scripts/run_tests.sh
```

| Suite | Herramienta | Comando |
|---|---|---|
| Backend unitarios | JUnit 5 + Mockito | `cd backend && mvn test` |
| Backend integración | Spring Boot Test + MockMvc | `cd backend && mvn verify` |
| Analytics | pytest | `cd analytics && python -m pytest tests/ -v` |
| Frontend | Jasmine + Karma | `cd frontend && ng test --watch=false` |

Ver la estrategia completa, casos de prueba manuales y deuda técnica en [documentacion/TESTING.md](documentacion/TESTING.md).

---

## Documentación

| Documento | Descripción |
|---|---|
| [ARQUITECTURA.md](documentacion/ARQUITECTURA.md) | Diagramas, flujos de request, WebSocket, decisiones de diseño |
| [BACKEND.md](documentacion/BACKEND.md) | Core Service: JWT, módulos, endpoints, WebSocket, MercadoPago |
| [FRONTEND.md](documentacion/FRONTEND.md) | Angular: guards, interceptores, Kanban en tiempo real, asistente IA |
| [MICROSERVICIO_ANALYTICS.md](documentacion/MICROSERVICIO_ANALYTICS.md) | FastAPI: endpoints de analítica, asistente IA y alertas |
| [BASE_DE_DATOS.md](documentacion/BASE_DE_DATOS.md) | Esquema, índices, DDL completo, seed de datos |
| [API_ENDPOINTS.md](documentacion/API_ENDPOINTS.md) | Referencia completa de la API (estilo Postman Docs) |
| [CICD.md](documentacion/CICD.md) | Pipeline CI/CD: workflows, jobs, variables de entorno |
| [DEPLOY.md](documentacion/DEPLOY.md) | Variables de entorno, Docker, checklist, backups |
| [TESTING.md](documentacion/TESTING.md) | Estrategia, cómo correr tests, deuda técnica |

---

## Autor

**Fabrizio Ludueña**
Trabajo Final Integrador — Tecnicatura Universitaria en Programación — UTN FRC — 2026

[![GitHub](https://img.shields.io/badge/GitHub-412237--Luduena-181717?logo=github)](https://github.com/412237-Luduena)

---

*Proyecto desarrollado con fines académicos.*
