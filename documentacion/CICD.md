# CI/CD — TallerSoft

## Descripción General

TallerSoft implementa un pipeline de Integración Continua y Entrega Continua (CI/CD) usando **GitHub Actions**. El pipeline está dividido en dos workflows independientes:

| Workflow | Archivo | Cuándo se dispara | Qué hace |
|----------|---------|-------------------|----------|
| **CI — Tests** | `.github/workflows/ci.yml` | Push o PR a `develop` / `main` | Corre los tres suites de tests en paralelo |
| **CD — Build y Push Docker** | `.github/workflows/cd.yml` | Push a `main` (merge exitoso) | Construye y publica imágenes Docker a GHCR |

---

## Workflow de CI (`ci.yml`)

### Objetivo

Garantizar que ningún commit que llegue a las ramas principales rompa los tests de ninguno de los tres servicios. Los tres jobs corren **en paralelo** para minimizar el tiempo de espera.

### Jobs

```
push / PR
    │
    ├── test-backend  (JUnit 5)    ─┐
    ├── test-analytics (pytest)    ─┤──→ all-tests-passed (gate)
    └── test-frontend (Karma)      ─┘
```

#### `test-backend` — JUnit 5

- **Entorno:** `ubuntu-latest` con Java 21 (distribución Temurin)
- **Servicio auxiliar:** PostgreSQL 16 levantado como service container de GitHub Actions (con healthcheck `pg_isready`)
- **Caché:** dependencias Maven cacheadas por `actions/setup-java`
- **Variables de entorno:** credenciales dummy (JWT, MP, Groq) suficientes para que los tests unitarios funcionen sin servicios externos reales
- **Artefacto generado:** reportes Surefire en `backend/target/surefire-reports/` (subidos siempre, incluso si los tests fallan)
- **Comando:** `mvn test --batch-mode`

#### `test-analytics` — pytest

- **Entorno:** `ubuntu-latest` con Python 3.11
- **Caché:** dependencias pip cacheadas por hash de `requirements.txt`
- **Base de datos:** los tests usan SQLite in-memory (configurado en `analytics/tests/conftest.py`) — no necesitan el PostgreSQL de CI
- **Variable `GROQ_API_KEY`:** valor dummy; el servicio Groq está mockeado en todos los tests con `unittest.mock.patch`
- **Comando:** `python -m pytest tests/ -v --tb=short`

#### `test-frontend` — Karma + Jasmine

- **Entorno:** `ubuntu-latest` con Node.js 20
- **Caché:** dependencias npm cacheadas por hash de `package-lock.json`
- **Instalación reproducible:** `npm ci` (usa exactamente las versiones del lockfile)
- **Browser:** ChromeHeadless (sin interfaz gráfica, adecuado para CI)
- **Artefacto generado:** reporte de cobertura en `frontend/coverage/` (subido siempre)
- **Comando:** `npx ng test --watch=false --browsers=ChromeHeadless --code-coverage`

#### `all-tests-passed` — Gate

Job final que solo tiene éxito si los tres anteriores resultaron en `success`. Actúa como punto de control para requerimientos de branch protection: configurar la rama `main` para que solo acepte merges cuando este job pase.

---

## Workflow de CD (`cd.yml`)

### Objetivo

Cuando un commit llega a `main` (vía merge de PR), construir las cuatro imágenes Docker y publicarlas en **GitHub Container Registry (GHCR)** con dos tags:

- `:latest` — apunta siempre a la última versión de `main`
- `:<sha>` — tag inmutable con el hash del commit (permite rollback exacto)

### Imágenes publicadas

| Imagen | Contexto de build |
|--------|------------------|
| `ghcr.io/412237-Luduena/tallersoft-backend:latest` | `./backend` |
| `ghcr.io/412237-Luduena/tallersoft-analytics:latest` | `./analytics` |
| `ghcr.io/412237-Luduena/tallersoft-gateway:latest` | `./gateway` |
| `ghcr.io/412237-Luduena/tallersoft-frontend:latest` | `./frontend` |

### Autenticación con GHCR

Usa el token `GITHUB_TOKEN` que GitHub inyecta automáticamente en cada ejecución. No requiere configurar secrets adicionales para publicar en el registry del mismo repositorio. El job necesita el permiso `packages: write` declarado explícitamente.

### Caché de capas Docker

Se usa `cache-from: type=gha` / `cache-to: type=gha,mode=max`. GitHub Actions cachea las capas de Docker entre ejecuciones del mismo workflow, lo que acelera significativamente los builds cuando solo cambiaron pocas capas (por ejemplo, solo el código de la aplicación y no las dependencias).

### Resumen de ejecución

Al finalizar, el workflow escribe en el **Job Summary** de GitHub Actions la lista de imágenes publicadas con su SHA, visible directamente en la pestaña "Actions" del repositorio.

---

## Diagrama de Flujo Completo

```
Developer → git push origin develop
                │
                ▼
        GitHub Actions dispara ci.yml
                │
        ┌───────┴───────────────────┐
        │                           │
  test-backend              test-analytics
  (JUnit + PostgreSQL)      (pytest + SQLite)
        │                           │
        └───────────┬───────────────┘
                    │
             test-frontend
             (Karma + ChromeHeadless)
                    │
             all-tests-passed
             ✓ (o ✗ si alguno falló)
                    │
         [merge PR a main aprobado]
                    │
                    ▼
        GitHub Actions dispara cd.yml
                    │
          build-and-push (Docker Buildx)
          ├── backend   → ghcr.io/.../tallersoft-backend:<sha>
          ├── analytics → ghcr.io/.../tallersoft-analytics:<sha>
          ├── gateway   → ghcr.io/.../tallersoft-gateway:<sha>
          └── frontend  → ghcr.io/.../tallersoft-frontend:<sha>
```

---

## Configuración de Branch Protection (recomendada)

Para obligar que los tests pasen antes de mergear a `main`:

1. Ir a **Settings → Branches → Add branch protection rule**
2. Branch name pattern: `main`
3. Habilitar **Require status checks to pass before merging**
4. Agregar el check: `Todos los tests pasaron` (job `all-tests-passed`)
5. Habilitar **Require branches to be up to date before merging**

Con esto, ningún PR puede mergearse a `main` si el pipeline de CI está rojo.

---

## Variables de Entorno Requeridas por CI

Los tests no usan servicios externos reales — todas las variables sensibles tienen valores dummy en el workflow. La siguiente tabla explica para qué sirve cada una en el contexto de CI:

| Variable | Job | Valor en CI | Por qué |
|----------|-----|-------------|---------|
| `JWT_SECRET` | backend | Cadena fija de 64+ chars | Los tests de JWT necesitan un secreto para firmar tokens de prueba |
| `MP_ACCESS_TOKEN` | backend | `TEST-dummy-token` | Los tests de MercadoPago están mockeados; el valor evita que falle la inicialización |
| `MP_WEBHOOK_SECRET` | backend | `dummy-webhook-secret` | El `MercadoPagoWebhookValidator` lo requiere en construcción |
| `MP_SANDBOX` | backend | `true` | Evita la validación en `@PostConstruct` que exige el secreto en producción |
| `GROQ_API_KEY` | analytics | `dummy-key-for-tests` | El cliente Groq se inicializa al importar el módulo; el valor evita errores de configuración |
| `ANTHROPIC_API_KEY` | backend | `dummy-key` | Requerido por `application.yml` aunque no se usa en tests unitarios |

---

## Cómo Ver los Resultados

1. Ir al repositorio en GitHub
2. Pestaña **Actions**
3. Seleccionar el workflow (`CI — Tests` o `CD — Build y Push Docker`)
4. Ver el log de cada job, los artefactos generados y el resumen

El badge en el `README.md` raíz muestra el estado del último run de CI en `main`:

```
[![CI](https://github.com/412237-Luduena/TFI-TechSoft/actions/workflows/ci.yml/badge.svg)](...)
```

---

## Extensiones Futuras

| Mejora | Descripción |
|--------|-------------|
| Deploy automático | Agregar un job en `cd.yml` que haga `docker-compose pull && docker-compose up -d` vía SSH al servidor de producción |
| Notificaciones | Integrar con Slack o email para avisar cuando el pipeline falla en `main` |
| Análisis de código estático | Agregar SpotBugs (Java), flake8/ruff (Python) y ESLint (Angular) como steps opcionales |
| Matriz de versiones | Testear el backend contra Java 21 y 22 en paralelo |
| Caché de imágenes Docker por servicio | Separar el job CD en cuatro jobs paralelos independientes para reducir el tiempo total de build |
