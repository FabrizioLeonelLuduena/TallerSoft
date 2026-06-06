# Documentación — TallerSoft

Sistema ERP web para talleres de servicio técnico.  
Trabajo Final Integrador · Tecnicatura Universitaria en Programación · UTN FRC · 2026

---

## Contenido de esta Carpeta

| Archivo | Descripción |
|---------|-------------|
| [ARQUITECTURA.md](ARQUITECTURA.md) | Diagrama de arquitectura, flujos de request, decisiones de diseño |
| [BACKEND.md](BACKEND.md) | Core Service Java/Spring Boot: estructura, JWT, módulos, endpoints |
| [FRONTEND.md](FRONTEND.md) | Angular PWA: módulos, autenticación, Kanban, asistente IA |
| [MICROSERVICIO_ANALYTICS.md](MICROSERVICIO_ANALYTICS.md) | FastAPI Python: endpoints de análisis y asistente IA |
| [BASE_DE_DATOS.md](BASE_DE_DATOS.md) | Esquema PostgreSQL: tablas, índices, scripts SQL, seed de datos |
| [API_ENDPOINTS.md](API_ENDPOINTS.md) | Referencia completa de todos los endpoints (estilo Postman Docs) |
| [DEPLOY.md](DEPLOY.md) | Variables de entorno, checklist pre-deploy, Docker Compose, backups |
| [TESTING.md](TESTING.md) | Estrategia de testing, cómo correr tests, casos de prueba manuales |

---

## Guía de Lectura por Rol

### 🧑‍💻 Nuevo Developer
1. [ARQUITECTURA.md](ARQUITECTURA.md) — entender el sistema completo
2. [BACKEND.md](BACKEND.md) o [FRONTEND.md](FRONTEND.md) según tu área
3. [BASE_DE_DATOS.md](BASE_DE_DATOS.md) — esquema de datos
4. [TESTING.md](TESTING.md) — cómo correr tests y agregar nuevos

### 🗄️ DBA / Infraestructura
1. [BASE_DE_DATOS.md](BASE_DE_DATOS.md) — esquema, índices, permisos
2. [DEPLOY.md](DEPLOY.md) — variables de entorno, Docker, backups
3. [ARQUITECTURA.md](ARQUITECTURA.md) — topología de servicios

### 🧪 QA / Tester
1. [TESTING.md](TESTING.md) — estrategia, casos de prueba manuales
2. [API_ENDPOINTS.md](API_ENDPOINTS.md) — referencia de endpoints para armar colección Postman

### ⚙️ DevOps
1. [DEPLOY.md](DEPLOY.md) — checklist pre-deploy, Docker Compose, health checks
2. [ARQUITECTURA.md](ARQUITECTURA.md) — puertos, servicios, topología

### 📋 Product Owner / Cliente
1. [ARQUITECTURA.md](ARQUITECTURA.md) — sección "Decisiones de Arquitectura" y descripción de módulos

---

## Setup Rápido (Desarrollo Local)

```bash
# 1. Clonar e instalar
git clone https://github.com/usuario/tallersoft.git
cd tallersoft

# 2. Copiar variables de entorno
cp .env.example .env
# Completar .env con tus valores

# 3. Levantar todo con Docker Compose
docker compose up --build -d

# 4. La app estará disponible en:
# Frontend:   http://localhost:80
# Gateway:    http://localhost:8080
# API Docs:   http://localhost:8082/docs (Analytics)
```

Para más detalles ver [DEPLOY.md](DEPLOY.md).
