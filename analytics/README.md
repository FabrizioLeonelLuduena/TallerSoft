# TallerSoft Analytics Service

Microservicio de analítica en Python + FastAPI para TallerSoft.
Provee KPIs, reportes y un asistente IA conversacional sobre los datos del taller.
Solo tiene permisos de lectura sobre la base de datos.

Puerto: **8082** (nunca se expone directamente; el tráfico pasa por el API Gateway en 8080).

---

## Stack

| Componente | Tecnología |
|---|---|
| Framework | FastAPI 0.111 |
| Servidor ASGI | Uvicorn 0.30 |
| ORM / BD | SQLAlchemy 2.0 + psycopg2 |
| Análisis | Pandas 2.2 |
| Asistente IA | Groq API — Llama 3.3 70B |
| Validación | Pydantic 2.7 |

---

## Instalación y ejecución local

```bash
cd analytics

# Crear entorno virtual
python -m venv .venv
source .venv/bin/activate      # Linux / Mac
# .venv\Scripts\activate       # Windows

# Instalar dependencias
pip install -r requirements.txt

# Configurar variables de entorno
cp .env.example .env
# Editar .env con los valores reales

# Iniciar el servicio
uvicorn app.main:app --host 0.0.0.0 --port 8082 --reload
```

Swagger disponible en: `http://localhost:8082/docs`

---

## Variables de entorno

Copiar `.env.example` como `.env` y completar los valores:

| Variable | Descripción |
|---|---|
| `DB_HOST` | Host de PostgreSQL |
| `DB_PORT` | Puerto (default 5432) |
| `DB_NAME` | Nombre de la base de datos |
| `ANALYTICS_USER` | Usuario readonly de PostgreSQL |
| `ANALYTICS_PASSWORD` | Contraseña del usuario readonly |
| `GROQ_API_KEY` | API Key de Groq (gratuito en console.groq.com) |
| `GROQ_MODEL` | Modelo a usar (default: llama-3.3-70b-versatile) |
| `GROQ_MAX_TOKENS` | Tokens máximos de respuesta (default: 1024) |
| `ALLOWED_ORIGINS` | Orígenes CORS permitidos (separados por coma) |

---

## Crear usuario readonly en PostgreSQL

```sql
CREATE USER analytics_reader WITH PASSWORD 'password_seguro';
GRANT CONNECT ON DATABASE tallersoft TO analytics_reader;
GRANT USAGE ON SCHEMA public TO analytics_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_reader;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
  GRANT SELECT ON TABLES TO analytics_reader;
```

---

## Endpoints

Todos los endpoints requieren JWT válido (validado por el Gateway).

### Órdenes

| Método | URL | Descripción |
|---|---|---|
| GET | `/analytics/ordenes/resumen` | Totales por estado |
| GET | `/analytics/ordenes/por-periodo` | Órdenes agrupadas por semana o mes |
| GET | `/analytics/ordenes/tecnicos/rendimiento` | Órdenes cerradas y tiempo promedio por técnico |

**Parámetros `/por-periodo`:**
- `agrupacion`: `semana` o `mes` (default: `mes`)
- `meses_atras`: 1–24 (default: 6)

**Ejemplo respuesta `/resumen`:**
```json
{
  "pendientes": 12,
  "en_proceso": 5,
  "listas": 3,
  "entregadas": 87,
  "total": 107
}
```

### Stock

| Método | URL | Descripción |
|---|---|---|
| GET | `/analytics/stock/critico` | Repuestos con stock ≤ mínimo |
| GET | `/analytics/stock/mas-usados` | Top N repuestos más usados |

**Parámetros `/mas-usados`:**
- `top`: 1–100 (default: 10)
- `dias`: 1–365 (default: 30)

**Ejemplo respuesta `/critico`:**
```json
[
  {
    "id": 3,
    "nombre": "Filtro de aceite",
    "categoria": "Filtros",
    "stock_actual": 1,
    "stock_minimo": 5,
    "diferencia": -4
  }
]
```

### Caja

| Método | URL | Descripción |
|---|---|---|
| GET | `/analytics/caja/resumen-diario` | Ingresos del día con desglose por medio de pago |
| GET | `/analytics/caja/evolucion-mensual` | Ingresos mes a mes |

**Parámetros `/resumen-diario`:**
- `fecha`: fecha ISO `YYYY-MM-DD` (default: hoy)

**Parámetros `/evolucion-mensual`:**
- `meses`: 1–24 (default: 6)

**Ejemplo respuesta `/resumen-diario`:**
```json
{
  "fecha": "2025-06-02",
  "total_ingresos": 45000.00,
  "cantidad_cobros": 8,
  "desglose": [
    { "medio_pago": "EFECTIVO", "total": 20000.0, "cantidad": 4 },
    { "medio_pago": "TARJETA",  "total": 25000.0, "cantidad": 4 }
  ]
}
```

### Asistente IA

| Método | URL | Descripción |
|---|---|---|
| POST | `/analytics/asistente/consulta` | Consulta en lenguaje natural al asistente |

**Body:**
```json
{ "pregunta": "¿Cuántas órdenes tenemos pendientes hoy?" }
```

**Respuesta:**
```json
{
  "respuesta": "Actualmente tenés 12 órdenes pendientes...",
  "contexto_utilizado": { ... }
}
```

Validación: `pregunta` debe tener entre 3 y 500 caracteres.

---

## Tests

```bash
cd analytics
source .venv/bin/activate

# Todos los tests
pytest tests/ -v

# Por módulo
pytest tests/test_ordenes.py -v
pytest tests/test_stock.py -v
pytest tests/test_caja.py -v
pytest tests/test_asistente.py -v

# Con cobertura
pytest tests/ --cov=app --cov-report=term-missing
```

Los tests usan SQLite en memoria y mockean la API de Groq, por lo que no necesitan conexión a PostgreSQL ni API keys reales.

---

## Docker

```bash
# Construir imagen
docker build -t tallersoft-analytics .

# Levantar con docker compose (desde la raíz del proyecto)
docker compose up --build analytics
```

Health check: `GET http://localhost:8082/health`
