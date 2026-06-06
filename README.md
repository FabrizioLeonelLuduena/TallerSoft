<div align="center">

# TallerSoft

**ERP inteligente para talleres de servicio técnico**

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
| 🔧 | Órdenes de trabajo con flujo Kanban (PENDIENTE → EN_PROCESO → LISTO → ENTREGADO) |
| 📦 | Control de stock con alertas de inventario crítico |
| 💳 | Caja y facturación con soporte para Efectivo, Tarjeta y MercadoPago |
| 📊 | Dashboard analítico con KPIs y métricas de rendimiento del equipo |
| 🤖 | Asistente IA conversacional conectado a datos reales del negocio |

---

## Arquitectura

El sistema está compuesto por tres capas que se comunican a través de un API Gateway como único punto de entrada:

```
┌──────────────────────────────────────────┐
│           FRONTEND  (Angular 18)         │
│         Angular Material 18 · PWA       │
└───────────────────┬──────────────────────┘
                    │ HTTP/REST
┌───────────────────▼──────────────────────┐
│      API GATEWAY  (Spring Cloud)         │
│  Enruta · Valida JWT · Puerto 8080       │
└──────────┬──────────────┬────────────────┘
           │              │
┌──────────▼──────┐  ┌────▼───────────────┐
│  CORE SERVICE   │  │  ANALYTICS SERVICE │
│  Spring Boot 3  │  │  FastAPI · Python  │
│  Puerto 8081    │  │  Puerto 8082       │
│                 │  │  Solo SELECT       │
└──────────┬──────┘  └────┬───────────────┘
           └──────┬────────┘
        ┌─────────▼──────────┐
        │    PostgreSQL 16   │
        │    Puerto 5432     │
        └────────────────────┘
```

**Reglas de arquitectura:**
- El Gateway (`:8080`) es el **único punto de entrada** desde el frontend. Nunca llamar directo a `:8081` o `:8082`.
- El Analytics Service tiene permisos de **solo lectura** sobre la base de datos.
- Los tokens JWT se almacenan en `sessionStorage`, nunca en `localStorage`.

---

## Stack Tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Frontend | Angular | 18.2 |
| UI Components | Angular Material + CDK | 18.2 |
| Backend principal | Java + Spring Boot | 21 / 3.2.5 |
| Seguridad | Spring Security + JWT (HS256) | 3.2.5 |
| ORM | Spring Data JPA + Hibernate | 3.2.5 |
| API Gateway | Spring Cloud Gateway | 2024.0.1 |
| Microservicio analítica | Python + FastAPI | 3.11 / 0.111 |
| Análisis de datos | Pandas + SQLAlchemy | 2.x |
| Base de datos | PostgreSQL | 16 |
| Asistente IA | Groq API (llama-3.3-70b-versatile) | 0.9.0 |
| Pagos | MercadoPago API | latest |
| Exportación PDF | iText | 7.x |
| Contenedores | Docker + Docker Compose | latest |
| Testing backend | JUnit 5 + Mockito | - |
| Testing analytics | pytest + httpx | 8.x |
| Testing frontend | Jasmine + Karma | - |

---

## Estructura del Proyecto

```
tallersoft/
├── frontend/                  # Angular 18 — PWA
├── backend/                   # Core Service — Spring Boot 3
│   └── src/test/              # Suite JUnit 5 + Mockito
├── gateway/                   # Spring Cloud Gateway
├── analytics/                 # Microservicio Python — FastAPI
│   ├── app/
│   │   ├── services/
│   │   │   ├── groq_service.py        # Asistente IA (activo)
│   │   │   ├── alertas_service.py     # Alertas operativas
│   │   │   └── analytics_service.py   # Análisis con Pandas
│   │   └── routers/
│   └── tests/                 # Suite pytest
├── documentacion/             # Documentación técnica completa
│   ├── ARQUITECTURA.md
│   ├── BACKEND.md
│   ├── FRONTEND.md
│   ├── MICROSERVICIO_ANALYTICS.md
│   ├── BASE_DE_DATOS.md
│   ├── API_ENDPOINTS.md
│   ├── DEPLOY.md
│   └── TESTING.md
├── scripts/
│   └── run_tests.sh           # Corre los tres suites de tests
├── docker-compose.yml
├── docker-compose.dev.yml
└── .env.example
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
git clone https://github.com/412237-Luduena/tallersoft.git
cd tallersoft

# 2. Configurar variables de entorno
cp .env.example .env
# Editar .env con tus credenciales

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

---

## Módulos del Sistema

### Autenticación y Roles

JWT (HS256) con claims `userId`, `email`, `rol`. Tres roles disponibles:

| Rol | Acceso |
|---|---|
| `ADMIN` | Total: usuarios, dashboard, reportes, configuración |
| `TECNICO` | Sus propias órdenes, diagnósticos y repuestos |
| `RECEPCION` | Clientes, equipos, órdenes nuevas, cobros |

### Órdenes de Trabajo

Flujo de estados unidireccional con tablero Kanban drag & drop (CDK):

```
PENDIENTE  →  EN_PROCESO  →  LISTO  →  ENTREGADO
```

- Para pasar a `LISTO` se requiere diagnóstico cargado.
- Al registrar un cobro aprobado, la orden pasa automáticamente a `ENTREGADO`.

### Caja y Pagos

| Medio | Flujo |
|---|---|
| EFECTIVO | Aprobado inmediatamente, calcula vuelto |
| TARJETA | Aprobado al confirmar posnet |
| MERCADOPAGO | Genera QR/link, queda PENDIENTE hasta recibir webhook |

El webhook de MercadoPago es público pero valida firma HMAC-SHA256. El procesamiento es **idempotente**.

### Asistente IA

Chat en lenguaje natural que enriquece cada pregunta con contexto real de la base de datos (órdenes, stock, caja, rendimiento de técnicos) antes de consultar el modelo. Implementado con la API de **Groq** (`llama-3.3-70b-versatile`).

### Dashboard Analítico

KPIs en tiempo real servidos por el microservicio Python:

- Órdenes por estado y por período
- Ingresos del día / semana / mes con evolución histórica
- Rendimiento de técnicos (órdenes cerradas, tiempo promedio)
- Repuestos con stock crítico
- Alertas operativas (órdenes sin movimiento, prioridad alta paradas)

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
| Analytics | pytest + httpx | `cd analytics && python -m pytest tests/ -v` |
| Frontend | Jasmine + Karma | `cd frontend && ng test --watch=false` |

Ver la estrategia completa, casos de prueba manuales y deuda técnica en [documentacion/TESTING.md](documentacion/TESTING.md).

---

## Documentación

| Documento | Descripción |
|---|---|
| [ARQUITECTURA.md](documentacion/ARQUITECTURA.md) | Diagramas, flujos de request, decisiones de diseño |
| [BACKEND.md](documentacion/BACKEND.md) | Core Service: JWT, módulos, endpoints, seguridad |
| [FRONTEND.md](documentacion/FRONTEND.md) | Angular: guards, interceptores, Kanban, asistente IA |
| [MICROSERVICIO_ANALYTICS.md](documentacion/MICROSERVICIO_ANALYTICS.md) | FastAPI: endpoints de analítica y asistente IA |
| [BASE_DE_DATOS.md](documentacion/BASE_DE_DATOS.md) | Esquema, índices, DDL completo, seed de datos |
| [API_ENDPOINTS.md](documentacion/API_ENDPOINTS.md) | Referencia completa de la API (estilo Postman Docs) |
| [DEPLOY.md](documentacion/DEPLOY.md) | Variables de entorno, Docker, checklist, backups |
| [TESTING.md](documentacion/TESTING.md) | Estrategia, cómo correr tests, deuda técnica |

---

## Autor

**Fabrizio Ludueña**
Trabajo Final Integrador — Tecnicatura Universitaria en Programación — UTN FRC — 2026

[![GitHub](https://img.shields.io/badge/GitHub-412237--Luduena-181717?logo=github)](https://github.com/412237-Luduena)

---

*Proyecto desarrollado con fines académicos.*
