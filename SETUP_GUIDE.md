# TallerSoft Setup Guide

Complete step-by-step instructions for setting up the TallerSoft development environment and running all services.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Clone and Configure](#clone-and-configure)
3. [Option A: Docker Compose (Recommended)](#option-a-docker-compose-recommended)
4. [Option B: Local Development Setup](#option-b-local-development-setup)
5. [Accessing Services](#accessing-services)
6. [Troubleshooting](#troubleshooting)

---

## Prerequisites

Before you begin, ensure you have the following installed:

### For Docker Compose Setup
- Docker Desktop (version 20.10 or higher)
- Docker Compose (version 1.29 or higher)
- Git

### For Local Development Setup
All prerequisites from Docker Compose, plus:
- Java 21 (OpenJDK recommended)
- Maven 3.9+
- Node.js 20+
- Angular CLI 17+
- Python 3.11+
- PostgreSQL 16+ client tools

## Clone and Configure

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/tallersoft.git
cd tallersoft
```

### 2. Create environment file

```bash
cp .env.example .env
```

### 3. Edit `.env` file with your values

```bash
# Linux/macOS
nano .env

# Windows
notepad .env
```

**Important production values to change:**

```env
DB_PASSWORD=your_secure_database_password_here
JWT_SECRET=your_256bit_secure_secret_key_here
ANTHROPIC_API_KEY=sk-ant-your_api_key_here
MP_ACCESS_TOKEN=APP_USR-your_mercadopago_token
MP_WEBHOOK_SECRET=your_webhook_secret_here
```

---

## Option A: Docker Compose (Recommended)

### Complete Stack Setup (All Services)

```bash
# Start all services in the background
docker compose up -d

# View logs
docker compose logs -f

# View logs for a specific service
docker compose logs -f backend
docker compose logs -f analytics
```

### Database Setup

The database will be initialized automatically with:
- PostgreSQL 16 with all tables (usuarios, clientes, equipos, ordenes_trabajo, repuestos, cobros)
- read-only `analytics_reader` user for the analytics service

To manually verify:

```bash
docker compose exec db psql -U postgres -d tallersoft -c "\dt"
```

### Stop All Services

```bash
# Stop all services (keep data)
docker compose down

# Stop and remove all data
docker compose down -v
```

---

## Option B: Local Development Setup

### 1. Start PostgreSQL Only

```bash
# Start only the database
docker compose -f docker-compose.dev.yml up -d

# Verify database is running
docker compose -f docker-compose.dev.yml logs db
```

### 2. Initialize Database Schema

```bash
cd scripts
bash setup-db.sh

# Or manually using psql
psql -h localhost -p 5432 -U postgres -d tallersoft -f init-db.sql
psql -h localhost -p 5432 -U postgres -d tallersoft -f create-analytics-user.sql
```

### 3. Start Spring Boot Backend

```bash
cd backend

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/tallersoft-backend-1.0.0.jar
```

Backend starts on: **http://localhost:8081**

### 4. Start Spring Cloud Gateway

```bash
cd gateway

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/tallersoft-gateway-1.0.0.jar
```

Gateway starts on: **http://localhost:8080**

### 5. Start Python Analytics Service

```bash
cd analytics

# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # macOS/Linux
# or
venv\Scripts\activate  # Windows

# Install dependencies
pip install -r requirements.txt

# Run FastAPI
uvicorn app.main:app --reload --port 8082
```

Analytics starts on: **http://localhost:8082**

### 6. Start Angular Frontend

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
ng serve --open
```

Frontend starts on: **http://localhost:4200**

---

## Accessing Services

Once all services are running, you can access them at:

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:4200 | Angular application (dev) / http://localhost:80 (production) |
| **API Gateway** | http://localhost:8080 | Public API entry point |
| **Backend** | http://localhost:8081 | Core Service (internal) |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | REST API documentation |
| **Analytics** | http://localhost:8082 | Analytics Service (internal) |
| **Analytics Docs** | http://localhost:8082/docs | FastAPI documentation |
| **PostgreSQL** | localhost:5432 | Database connection |

### Sample Requests

#### 1. Health Check

```bash
curl http://localhost:8080/auth/login
# Should return 200 or auth error (not 404)
```

#### 2. Login (Get JWT Token)

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tallersoft.local","password":"password"}'
```

#### 3. Protected Endpoint (with Token)

```bash
curl -X GET http://localhost:8080/api/clientes \
  -H "Authorization: Bearer <your_jwt_token_here>"
```

#### 4. Analytics Endpoint

```bash
curl http://localhost:8080/analytics/ordenes/resumen
```

---

## Troubleshooting

### Docker Issues

#### Database container not starting

```bash
# Check logs
docker compose logs db

# Solution: Ensure port 5432 is not in use
lsof -i :5432  # Find process using port
kill -9 <PID>  # Kill process if needed
```

#### Services can't communicate

```bash
# Check network
docker network ls
docker network inspect tallersoft-network

# Restart all services
docker compose restart
```

### Backend Issues

#### Cannot connect to database

```
Error: Connection refused at 127.0.0.1:5432
```

**Solution:** Ensure `.env` file has correct `DB_HOST`. For local development:
```env
DB_HOST=localhost
```

For Docker:
```env
DB_HOST=db
```

#### Port already in use

```bash
# Check what's using the port
lsof -i :8081

# If it's another instance of the app, restart it
pkill -f "java.*tallersoft"
```

### Frontend Issues

#### Node modules not installing

```bash
# Clear npm cache
npm cache clean --force

# Delete and reinstall
rm -rf node_modules package-lock.json
npm install
```

#### ng command not found

```bash
# Install Angular CLI globally
npm install -g @angular/cli

# Or use npx (no global install needed)
npx ng serve --open
```

### Python Issues

#### Module not found errors

```bash
# Ensure virtual environment is activated
source venv/bin/activate

# Reinstall requirements
pip install -r requirements.txt
```

#### Port 8082 already in use

```bash
# Kill existing process
lsof -i :8082
kill -9 <PID>

# Or run on different port
uvicorn app.main:app --port 8083
```

---

## Development Workflow

### Making Code Changes

1. **Backend** (Spring Boot):
   - Maven hot-reload: Changes auto-compile in `mvn spring-boot:run`
   - Or restart manually: `Ctrl+C` then re-run

2. **Frontend** (Angular):
   - `ng serve` automatically reloads on file changes (hot reload)
   - Check browser console for errors

3. **Analytics** (Python):
   - `--reload` flag enables auto-reload on file changes
   - Check terminal for errors

### Testing API Changes

Use Postman or curl:

```bash
# Import collection from repository
# Or create manual requests
```

### Debugging

- **Spring Boot**: Add breakpoints in IDE, use `DEBUG` log level
- **Angular**: Chrome DevTools (F12)
- **Python**: Use `print()` or pdb debugger

---

## Next Steps

1. Read [CONVENTIONS.md](./CONVENTIONS.md) for code standards
2. Check the [README.md](./README.md) for architecture overview
3. Review the [Execution Plan](./docs/EXECUTION_PLAN.md) for feature roadmap

---

## Support

For issues or questions:

1. Check troubleshooting section above
2. Review service logs: `docker compose logs <service_name>`
3. Check Spring Boot actuator: http://localhost:8081/actuator/health
4. Check Analytics health: http://localhost:8082/health

---

*Last Updated: May 15, 2026*
