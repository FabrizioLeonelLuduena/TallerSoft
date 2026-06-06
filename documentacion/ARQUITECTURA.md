# Arquitectura del Sistema — TallerSoft

## Descripción General

TallerSoft es un ERP web para talleres de servicio técnico, compuesto por tres servicios independientes coordinados por un API Gateway.

---

## Diagrama de Arquitectura

```
                         ┌─────────────────────────────────────────┐
                         │            CLIENTE (Browser)             │
                         │         Angular 18 PWA (puerto 80)       │
                         └───────────────────┬─────────────────────┘
                                             │ HTTP/HTTPS
                                             ▼
                         ┌─────────────────────────────────────────┐
                         │          API GATEWAY (puerto 8080)       │
                         │       Spring Cloud Gateway 2024.0.1      │
                         │   Único punto de entrada al sistema      │
                         └──────┬──────────────────────┬───────────┘
                                │                      │
                  /api/**, /auth/**          /analytics/**
                                │                      │
               ┌────────────────▼──┐        ┌──────────▼──────────┐
               │   CORE SERVICE    │        │  ANALYTICS SERVICE  │
               │  (puerto 8081)    │        │   (puerto 8082)     │
               │  Java 21 +        │        │  Python 3.11 +      │
               │  Spring Boot 3    │        │  FastAPI            │
               │  Spring Security  │        │  Pandas             │
               │  JPA/Hibernate    │        │  SQLAlchemy         │
               │  JWT              │        │  Groq API           │
               └────────┬──────────┘        └──────────┬──────────┘
                        │ R/W                           │ R (solo lectura)
                        └──────────────┬────────────────┘
                                       ▼
                        ┌──────────────────────────────┐
                        │      PostgreSQL 16            │
                        │      (puerto 5432)            │
                        │  Base de datos compartida     │
                        └──────────────────────────────┘
```

---

## Descripción de Cada Servicio

### Frontend — Angular 18 PWA (puerto 80)
- **Responsabilidad:** Interfaz de usuario, SPA (Single Page Application) con soporte offline.
- **Tecnología:** Angular 18.2, Angular Material 18.2, CDK Drag and Drop (Kanban), gráficos CSS personalizados.
- **Comunicación:** Solo habla con el API Gateway en puerto 8080. Nunca llama directamente al Core (8081) ni al Analytics (8082).
- **Autenticación:** Almacena el JWT en `sessionStorage` (nunca `localStorage`).

### API Gateway — Spring Cloud Gateway (puerto 8080)
- **Responsabilidad:** Único punto de entrada. Enruta peticiones al Core Service o al Analytics Service según la ruta.
- **Tecnología:** Spring Boot 3, Spring Cloud Gateway.
- **Reglas de enrutamiento:**
  - `/analytics/**` → Analytics Service (8082)
  - Todo lo demás (`/api/**`, `/auth/**`) → Core Service (8081)

### Core Service — Spring Boot 3 (puerto 8081)
- **Responsabilidad:** Toda la lógica de negocio principal: usuarios, clientes, equipos, órdenes de trabajo, stock, cobros, pagos.
- **Tecnología:** Java 21, Spring Boot 3, Spring Security, JWT, JPA/Hibernate, MapStruct, iText.
- **Base de datos:** Acceso de lectura y escritura a PostgreSQL.

### Analytics Service — FastAPI (puerto 8082)
- **Responsabilidad:** KPIs, reportes, alertas y asistente IA. Solo lectura sobre la BD.
- **Tecnología:** Python 3.11, FastAPI, Pandas, SQLAlchemy, Groq API (llama-3.3-70b-versatile).
- **Restricción crítica:** Solo tiene permisos `SELECT` sobre la base de datos. Nunca escribe datos.

### Base de Datos — PostgreSQL 16 (puerto 5432)
- **Responsabilidad:** Persistencia de todos los datos del sistema.
- **Usuarios:** `tallersoft` (lectura/escritura para Core Service) y `analytics_readonly` (solo lectura para Analytics Service).

---

## Reglas de Arquitectura

| Regla | Descripción |
|-------|-------------|
| ✅ Gateway primero | El frontend siempre llama al Gateway (8080), nunca directamente a los servicios internos. |
| ✅ Analytics solo lectura | El Analytics Service solo tiene permisos `SELECT` sobre PostgreSQL. |
| ✅ JWT en sessionStorage | El frontend almacena tokens solo en `sessionStorage`, nunca en `localStorage`. |
| ❌ No llamar Core directo | El frontend nunca hace peticiones al Core (8081) directamente. |
| ❌ No escritura desde Analytics | El Analytics Service nunca ejecuta INSERT/UPDATE/DELETE. |
| ❌ No CORS permisivo | El Analytics Service solo acepta requests del Gateway. |

---

## Flujo de una Request Típica

Ejemplo: el usuario consulta las órdenes de trabajo desde el Kanban.

```
1. Usuario hace clic en "Kanban" en el navegador
   │
2. Angular llama GET http://localhost:8080/api/ordenes/activas
   │  (con header Authorization: Bearer <jwt>)
   │
3. API Gateway recibe la petición en puerto 8080
   │  - Verifica que la ruta no sea /analytics/**
   │  - Reenvía la petición al Core Service en 8081
   │
4. Core Service (8081) recibe GET /api/ordenes/activas
   │  - JwtAuthenticationFilter valida el token JWT
   │  - OrdenTrabajoController llama a OrdenTrabajoService
   │  - OrdenTrabajoService consulta la BD vía JPA
   │
5. PostgreSQL retorna las órdenes con estado != ENTREGADO
   │
6. Core Service serializa la respuesta como JSON
   │
7. API Gateway reenvía la respuesta al cliente
   │
8. Angular recibe la lista de órdenes y renderiza el Kanban
```

---

## Flujo del Asistente IA

```
1. Usuario escribe pregunta en el chat del frontend
   │
2. Angular POST http://localhost:8080/analytics/asistente/consulta
   │  Body: { "pregunta": "¿Cuántas órdenes hay pendientes?" }
   │
3. API Gateway enruta a Analytics Service (8082)
   │  (detecta prefijo /analytics/)
   │
4. Analytics Service recibe la pregunta
   │  - obtener_contexto_taller(db): consulta métricas clave de la BD
   │    (órdenes por estado, cobros del día, stock crítico, etc.)
   │  - Construye el prompt con el contexto real del taller
   │
5. Analytics llama a Groq API (llama-3.3-70b-versatile)
   │  con el SYSTEM_PROMPT + contexto + pregunta del usuario
   │
6. Claude responde en español con datos precisos del taller
   │
7. Analytics retorna { "respuesta": "...", "contexto_utilizado": {...} }
   │
8. Frontend muestra la respuesta en el chat
```

---

## Flujo del Webhook de MercadoPago

```
1. Cliente realiza pago en la app de MercadoPago
   │
2. MercadoPago llama POST https://tudominio.com/api/pagos/webhook
   │  (con header x-signature y x-request-id)
   │
3. API Gateway reenvía al Core Service (8081) en POST /api/pagos/webhook
   │  (ruta pública, no requiere JWT)
   │
4. PagoController valida la firma HMAC-SHA256 del webhook
   │  - Si la firma es inválida → retorna 400 Bad Request
   │
5. Core Service llama a MercadoPagoService.consultarPago(paymentId)
   │  para obtener el status real del pago
   │
6. CobrosService.procesarPagoAprobadoMercadoPago():
   │  - Si status = "approved": actualiza Cobro a APROBADO, Orden a ENTREGADO
   │  - Si status = "rejected": actualiza Cobro a RECHAZADO
   │  - Si ya estaba APROBADO: retorna 200 sin reprocesar (idempotencia)
   │
7. Core retorna 200 OK a MercadoPago (siempre para evitar reintentos)
```

---

## Decisiones de Arquitectura

### ¿Por qué un microservicio Python separado para analytics?

El Core Service en Java maneja la lógica transaccional crítica del negocio. Añadir operaciones de análisis pesadas (GROUP BY sobre millones de filas, integración con LLMs, generación de reportes con Pandas) dentro del Core afectaría su rendimiento y tiempo de respuesta. Python con Pandas/SQLAlchemy es más expresivo para análisis de datos. Además, el Analytics Service tiene permisos de solo lectura, lo que protege la integridad de los datos.

### ¿Por qué API Gateway?

Sin Gateway, el frontend necesitaría conocer las URLs internas de cada microservicio, lo que acopla el frontend a la infraestructura. El Gateway permite cambiar la topología interna (escalar, mover servicios) sin modificar el frontend. También centraliza el enrutamiento y permite añadir rate limiting o autenticación a nivel de gateway en el futuro.

### ¿Por qué JWT stateless?

JWT permite autenticación sin estado en el servidor, eliminando la necesidad de una sesión centralizada o un store de tokens. Esto es fundamental para escalar el Core Service horizontalmente (múltiples instancias) sin compartir estado de sesión. El token incluye `userId`, `email` y `rol`, suficiente para tomar decisiones de autorización sin consultar la BD en cada request.

---

## Consideraciones de Escalabilidad y Limitaciones

| Aspecto | Estado Actual | Evolución Posible |
|---------|--------------|-------------------|
| Core Service | Una instancia | Escala horizontal (sin estado) |
| Analytics Service | Una instancia | Escala horizontal (solo lectura) |
| Base de datos | Una instancia PostgreSQL | PostgreSQL HA, read replicas |
| JWT | Sin refresh token | Implementar refresh token |
| Analytics Claude | API call por request | Cache de respuestas frecuentes |
| Rate limiting | No implementado | Añadir en el Gateway |

**Limitaciones conocidas v1.0:**
- Sin refresh token: si el JWT expira durante la sesión, el usuario debe volver a loguearse.
- Sin WebSockets: el estado del Kanban no se actualiza en tiempo real entre múltiples usuarios.
- Analytics Service sin autenticación propia: confía en que el Gateway es el único que lo llama.
