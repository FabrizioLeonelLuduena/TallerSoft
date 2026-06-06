# Deploy — TallerSoft

## Requisitos del Servidor de Producción

| Recurso | Mínimo | Recomendado |
|---------|--------|-------------|
| CPU | 2 vCPUs | 4 vCPUs |
| RAM | 4 GB | 8 GB |
| Disco | 20 GB SSD | 50 GB SSD |
| SO | Ubuntu 22.04 LTS | Ubuntu 22.04 LTS |
| Docker | 24+ | 25+ |
| Docker Compose | v2 | v2 |

---

## Variables de Entorno de Producción

Crear un archivo `.env` en la raíz del repositorio (nunca commitearlo):

```env
# Base de datos
DB_NAME=tallersoft
DB_USER=tallersoft_user
DB_PASSWORD=contraseña_muy_segura_bd

# JWT (generar con: openssl rand -base64 32)
JWT_SECRET=clave_aleatoria_minimo_256_bits_produccion
JWT_EXPIRATION_MS=86400000

# MercadoPago (producción, NO sandbox)
MP_ACCESS_TOKEN=APP_USR-tu-token-produccion
MP_WEBHOOK_SECRET=secreto-de-webhooks-mp
MP_SANDBOX=false
MP_USER_ID=tu-user-id-mp
MP_POS_EXTERNAL_ID=tallersoftcaja01
MP_QR_IMAGE_URL=https://api.mercadopago.com/instore/qr/seller/...

# Anthropic Claude (o Groq para analytics)
ANTHROPIC_API_KEY=sk-ant-tu-clave-de-produccion
GROQ_API_KEY=gsk_tu-clave-groq-produccion
CLAUDE_MODEL=claude-sonnet-4-20250514
CLAUDE_MAX_TOKENS=1024

# URLs
WEBHOOK_BASE_URL=https://tudominio.com
BACKEND_PORT=8081

# Analytics
ANALYTICS_DATABASE_URL=postgresql://analytics_readonly:pass_readonly@db:5432/tallersoft
```

---

## Checklist Pre-Deploy

- [ ] Todas las variables de entorno de producción configuradas en `.env`
- [ ] `JWT_SECRET` cambiado a una clave de mínimo 256 bits:
  ```bash
  openssl rand -base64 32
  ```
- [ ] `DB_PASSWORD` es una contraseña segura (no la del desarrollo)
- [ ] `ANTHROPIC_API_KEY` o `GROQ_API_KEY` válida y con créditos suficientes
- [ ] `MP_ACCESS_TOKEN` en modo producción (no sandbox): `MP_SANDBOX=false`
- [ ] El archivo `.env` está en `.gitignore` y no fue commiteado (`git log --all -- .env`)
- [ ] El dominio de producción está configurado en el CORS del Analytics Service (`app/main.py`)
- [ ] El `WEBHOOK_BASE_URL` apunta al dominio de producción (no localhost)
- [ ] Los logs del backend no imprimen JWT ni credenciales (verificar `application.yml`)
- [ ] El script de BD (`init-db.sql`) fue ejecutado en la instancia de producción
- [ ] El usuario `analytics_readonly` fue creado con permisos de solo lectura

---

## Deploy con Docker Compose — Paso a Paso

### Primer deploy

```bash
# 1. Clonar el repositorio en el servidor
git clone https://github.com/usuario/tallersoft.git
cd tallersoft

# 2. Crear el archivo .env con las variables de producción
cp .env.example .env
nano .env   # Completar todas las variables

# 3. Construir y levantar todos los servicios
docker compose up --build -d

# 4. Verificar que todos los contenedores están corriendo
docker compose ps

# 5. Ver logs del primer arranque
docker compose logs -f backend
```

### Deploy de una nueva versión

```bash
# 1. Obtener los cambios
git pull origin main

# 2. Reconstruir solo los servicios modificados
docker compose up --build -d backend analytics

# 3. Verificar que levantan bien
docker compose ps
docker compose logs -f backend analytics
```

---

## Rollback a una Versión Anterior

```bash
# Opción 1: Volver al commit anterior en git
git log --oneline -10   # Identificar el commit anterior
git checkout <commit_hash>
docker compose up --build -d

# Opción 2: Si usás tags
git checkout v1.2.0
docker compose up --build -d

# Opción 3: Rollback de Docker image (si guardás imágenes)
docker compose down
docker compose up -d --no-build   # Usar imagen anterior sin rebuild
```

---

## Ver Logs de Cada Servicio

```bash
# Logs en tiempo real
docker compose logs -f backend        # Core Service (Java)
docker compose logs -f analytics      # Analytics Service (Python)
docker compose logs -f gateway        # API Gateway
docker compose logs -f db             # PostgreSQL
docker compose logs -f frontend       # Nginx (frontend)

# Últimas 100 líneas
docker compose logs --tail=100 backend
```

---

## Backup de la Base de Datos

### Backup completo

```bash
docker compose exec db pg_dump \
  -U postgres \
  -Fc \
  tallersoft > backup_$(date +%Y%m%d_%H%M%S).dump
```

### Backup solo datos (sin DDL)

```bash
docker compose exec db pg_dump \
  -U postgres \
  --data-only \
  tallersoft > data_$(date +%Y%m%d).sql
```

### Restaurar desde backup

```bash
# Parar servicios (excepto DB)
docker compose stop backend analytics gateway frontend

# Restaurar
docker compose exec -T db pg_restore \
  -U postgres \
  -d tallersoft \
  --clean < backup_20250606_120000.dump

# Reiniciar servicios
docker compose start backend analytics gateway frontend
```

**Recomendación:** Configurar cron para hacer backup diario:
```bash
0 3 * * * /ruta/a/proyecto && docker compose exec db pg_dump -U postgres -Fc tallersoft > /backups/tallersoft_$(date +\%Y\%m\%d).dump
```

---

## Deploy en Railway

1. Conectar el repositorio de GitHub a Railway
2. Crear 5 servicios en Railway: `db`, `backend`, `analytics`, `gateway`, `frontend`
3. Para cada servicio, configurar:
   - **Build command:** se detecta automáticamente desde Dockerfile
   - **Variables de entorno:** copiar del `.env` de producción
4. Railway genera URLs automáticamente para cada servicio
5. Actualizar `WEBHOOK_BASE_URL` con la URL del gateway generada por Railway

---

## Deploy en Render

1. Crear servicios web en Render para `backend`, `analytics`, `gateway`
2. Para la base de datos, usar el servicio de PostgreSQL de Render
3. El frontend puede desplegarse como "Static Site" en Render (dist de Angular)
4. Configurar las variables de entorno en el panel de Render

---

## Health Checks por Servicio

```bash
# API Gateway (único punto de entrada)
curl http://localhost:8080/actuator/health
# Esperado: { "status": "UP" }

# Core Service (interno)
curl http://localhost:8081/actuator/health
# Esperado: { "status": "UP" }

# Analytics Service (interno)
curl http://localhost:8082/health
# Esperado: { "status": "healthy" }

# Base de datos
docker compose exec db pg_isready -U postgres
# Esperado: /var/run/postgresql:5432 - accepting connections

# Frontend
curl -I http://localhost:80
# Esperado: HTTP/1.1 200 OK

# Verificar login end-to-end
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tallersoft.com","password":"admin123"}'
# Esperado: { "token": "eyJ...", ... }
```

### Verificar el deploy completo

```bash
# 1. Login y obtener token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tallersoft.com","password":"admin123"}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 2. Listar órdenes (autenticado)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/ordenes

# 3. Consultar asistente IA
curl -X POST http://localhost:8080/analytics/asistente/consulta \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"pregunta":"¿Cuántas órdenes hay?"}'
```
