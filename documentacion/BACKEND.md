# Backend — TallerSoft Core Service

## Stack y Versiones

| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| Java | 21 | Lenguaje del backend |
| Spring Boot | 3.x | Framework principal |
| Spring Security | 6.x | Autenticación y autorización |
| Spring Data JPA / Hibernate | 6.x | Persistencia |
| MapStruct | 1.5.x | Mapeo Entity ↔ DTO |
| JJWT (io.jsonwebtoken) | 0.12.x | Generación y validación de JWT |
| MercadoPago SDK | 2.x | Pagos online |
| iText | 7.x | Generación de PDFs |
| PostgreSQL Driver | 42.x | Conector JDBC |
| Lombok | 1.18.x | Reducción de boilerplate |
| JUnit 5 | 5.x | Tests unitarios |
| Mockito | 5.x | Mocks para tests |

---

## Estructura de Paquetes

```
com.tallersoft/
├── TallerSoftApplication.java     — Punto de entrada Spring Boot
├── config/
│   └── SecurityConfig.java        — Configuración de Spring Security
├── controller/
│   ├── AuthController.java        — Login y registro
│   ├── ClienteController.java     — CRUD de clientes
│   ├── EquipoController.java      — CRUD de equipos
│   ├── OrdenTrabajoController.java — Órdenes de trabajo
│   ├── RepuestoController.java    — CRUD de repuestos/stock
│   ├── CobrosController.java      — Registro y consulta de cobros
│   └── PagoController.java        — Webhook de MercadoPago
├── service/
│   ├── AuthService.java           — Login y registro de usuarios
│   ├── UsuarioService.java        — CRUD de usuarios
│   ├── ClienteService.java        — Lógica de clientes
│   ├── EquipoService.java         — Lógica de equipos
│   ├── OrdenTrabajoService.java   — Lógica principal de órdenes
│   ├── RepuestoService.java       — Gestión de stock
│   ├── CobrosService.java         — Cobros y caja
│   ├── MercadoPagoService.java    — Integración MP (Checkout Pro + POS QR)
│   └── PresupuestoPdfService.java — Generación de PDF con iText
├── repository/
│   ├── UsuarioRepository.java
│   ├── ClienteRepository.java
│   ├── EquipoRepository.java
│   ├── OrdenTrabajoRepository.java
│   ├── OrdenRepuestoRepository.java
│   ├── RepuestoRepository.java
│   └── CobrosRepository.java
├── model/
│   ├── Usuario.java               — Implementa UserDetails (Spring Security)
│   ├── Cliente.java
│   ├── Equipo.java
│   ├── OrdenTrabajo.java          — Entidad principal con @PrePersist/@PreUpdate
│   ├── OrdenRepuesto.java         — Ítem de repuesto en una orden
│   ├── Repuesto.java
│   ├── Cobro.java
│   ├── Rol.java                   — Enum: ADMIN, TECNICO, RECEPCION
│   ├── EstadoOrden.java           — Enum: PENDIENTE, EN_PROCESO, LISTO, ENTREGADO
│   ├── EstadoPago.java            — Enum: PENDIENTE, APROBADO, RECHAZADO
│   ├── MedioPago.java             — Enum: EFECTIVO, TARJETA, MERCADOPAGO
│   └── Prioridad.java             — Enum: BAJA, NORMAL, ALTA
├── dto/
│   ├── AuthRequest.java / AuthResponse.java
│   ├── UsuarioRequest.java / UsuarioResponse.java / UsuarioUpdateRequest.java
│   ├── ClienteRequest.java / ClienteResponse.java
│   ├── EquipoRequest.java / EquipoResponse.java
│   ├── OrdenTrabajoRequest.java / OrdenTrabajoResponse.java
│   ├── AgregarRepuestoRequest.java
│   ├── CambiarEstadoRequest.java
│   ├── DiagnosticoRequest.java
│   ├── RepuestoRequest.java / RepuestoResponse.java
│   ├── CobrarOrdenRequest.java / CobroResponse.java
│   ├── CajaDiariaResponse.java
│   └── MercadoPagoPreferenceResponse.java
├── mapper/
│   ├── UsuarioMapper.java
│   ├── ClienteMapper.java
│   ├── EquipoMapper.java
│   ├── OrdenTrabajoMapper.java
│   ├── OrdenRepuestoMapper.java
│   ├── RepuestoMapper.java
│   └── CobroMapper.java
├── security/
│   ├── JwtUtil.java               — Generación y validación de JWT
│   ├── JwtAuthenticationFilter.java — Filtro que extrae el JWT de cada request
│   ├── UserDetailsServiceImpl.java — Carga usuario desde BD para Spring Security
│   ├── MercadoPagoWebhookValidator.java — Valida firma HMAC-SHA256 del webhook
│   └── CurrentUser.java           — Anotación para inyectar el usuario autenticado
└── exception/
    ├── GlobalExceptionHandler.java — @RestControllerAdvice central
    ├── ErrorResponse.java
    ├── EntityNotFoundException.java     (→ 404)
    ├── InvalidCredentialsException.java (→ 401)
    ├── InvalidStateTransitionException.java (→ 409)
    ├── InsufficientStockException.java  (→ 409)
    ├── MissingDiagnosticException.java  (→ 400)
    └── CobrosException.java             (→ 400)
```

---

## Convenciones de Código

### Nomenclatura de clases
- **Entities:** `NombreEntidad.java` (sin sufijo)
- **Repositories:** `NombreEntidadRepository.java` (extienden `JpaRepository`)
- **Services:** `NombreServicioService.java`
- **Controllers:** `NombreControllerController.java`
- **DTOs:** `NombreRequest.java` / `NombreResponse.java`
- **Mappers:** `NombreMapper.java` (interfaces de MapStruct)
- **Excepciones:** `NombreException.java` (extienden `RuntimeException`)

### Uso de DTOs
Los controllers siempre reciben y retornan DTOs, nunca entidades JPA. Los mappers de MapStruct convierten entre los dos mundos sin código manual.

### Manejo de excepciones
El `GlobalExceptionHandler` centraliza todos los errores. Cada excepción de negocio tiene su propio tipo y código HTTP específico. Nunca retornar `500` por errores de negocio esperados.

---

## Autenticación JWT

### Flujo de Login
```
POST /auth/login
Body: { "email": "...", "password": "..." }

1. AuthService.login() busca el usuario por email
2. Verifica la contraseña con BCryptPasswordEncoder.matches()
3. Verifica que el usuario está activo (activo = true)
4. Genera el token con JwtUtil.generateToken(userId, email, rol)
5. Retorna AuthResponse { token, userId, email, rol }
```

### Claims del JWT
| Claim | Tipo | Descripción |
|-------|------|-------------|
| `sub` | String | Email del usuario |
| `userId` | Long | ID del usuario en la BD |
| `email` | String | Email del usuario |
| `rol` | String | Rol: ADMIN, TECNICO o RECEPCION |
| `iat` | Timestamp | Fecha de emisión |
| `exp` | Timestamp | Fecha de expiración (default: 24h) |

### Validación en cada Request
El `JwtAuthenticationFilter` intercepta cada HTTP request:
1. Extrae el header `Authorization: Bearer <token>`
2. Valida la firma y expiración del token con `JwtUtil.validateToken()`
3. Si válido, crea un `UsernamePasswordAuthenticationToken` y lo establece en el `SecurityContextHolder`
4. Si inválido o expirado, retorna 401 con mensaje `TOKEN_EXPIRADO`

---

## Roles y Permisos

| Endpoint / Acción | ADMIN | TECNICO | RECEPCION |
|-------------------|-------|---------|-----------|
| Gestión de usuarios (`/api/usuarios/**`) | ✅ | ❌ | ❌ |
| Crear/editar clientes | ✅ | ❌ | ✅ |
| Ver clientes | ✅ | ✅ | ✅ |
| Crear órdenes | ✅ | ❌ | ✅ |
| Ver órdenes | ✅ | ✅ (solo las suyas) | ✅ |
| Cambiar estado de orden | ✅ | ✅ | ✅ |
| Agregar diagnóstico | ✅ | ✅ | ❌ |
| Agregar repuestos a orden | ✅ | ✅ | ❌ |
| Gestión de stock | ✅ | ❌ | ✅ |
| Registrar cobros | ✅ | ❌ | ✅ |
| Ver caja diaria | ✅ | ❌ | ✅ |
| Analytics / asistente IA | ✅ | ✅ | ✅ |

---

## Módulos con sus Endpoints

### Auth
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/auth/login` | Login, retorna JWT |
| POST | `/auth/register` | Registro de usuario (público) |

### Clientes
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/clientes` | Listar (filtro opcional por nombre) |
| GET | `/api/clientes/{id}` | Obtener por ID |
| POST | `/api/clientes` | Crear nuevo cliente |
| PUT | `/api/clientes/{id}` | Editar cliente |
| DELETE | `/api/clientes/{id}` | Baja lógica (activo=false) |

### Órdenes de Trabajo
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/ordenes` | Listar (filtros: estado, tecnicoId, desde, hasta) |
| GET | `/api/ordenes/activas` | Órdenes con estado != ENTREGADO |
| GET | `/api/ordenes/{id}` | Detalle de orden |
| POST | `/api/ordenes` | Crear nueva orden |
| PUT | `/api/ordenes/{id}` | Actualizar orden |
| PUT | `/api/ordenes/{id}/estado` | Cambiar estado |
| POST | `/api/ordenes/{id}/repuestos` | Agregar repuesto (descuenta stock) |

### Repuestos / Stock
| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/repuestos` | Listar todos |
| GET | `/api/repuestos/{id}` | Obtener por ID |
| POST | `/api/repuestos` | Crear |
| PUT | `/api/repuestos/{id}` | Editar |

### Cobros / Caja
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/cobros` | Registrar cobro (EFECTIVO/TARJETA/MERCADOPAGO) |
| GET | `/api/cobros/caja-diaria` | Resumen del día |
| POST | `/api/cobros/{id}/confirmar` | Confirmar pago manual (TARJETA) |
| POST | `/api/pagos/webhook` | Webhook de MercadoPago (público, sin JWT) |

---

## Configuración de Spring Security

### Rutas Públicas (sin JWT)
```java
"/auth/**"           — Login y registro
"/api/pagos/webhook" — Webhook de MercadoPago
"/v3/api-docs/**"    — Swagger OpenAPI
"/swagger-ui/**"     — Swagger UI
```

Todas las demás rutas requieren JWT válido.

---

## Integración con MercadoPago

### Modalidades Implementadas

**1. QR de Punto de Venta (POS) — Recomendado para caja presencial**
- Se carga el monto vía `PUT /instore/orders/qr/seller/collectors/{userId}/pos/{posId}/qrs`
- El QR estático del POS se reutiliza; solo cambia el monto cargado
- Compatible con todas las billeteras (EMVCo / Pago QR BCRA)
- El `qrImageUrl` es la URL pública de la imagen QR del POS

**2. Checkout Pro**
- Se crea una `Preference` via SDK → MP retorna un `initPoint` (link)
- Desde ese link se genera un QR con ZXing
- Solo compatible con la app de MercadoPago

### Validación del Webhook
La firma `x-signature` del webhook se valida con HMAC-SHA256:
```
Mensaje = "id:<paymentId>;request-id:<requestId>;ts:<timestamp>;"
HMAC = HmacSHA256(mensaje, MP_WEBHOOK_SECRET)
```
Si `HMAC != v1` del header → retorna 400 (firma inválida).

**Idempotencia:** Si el cobro ya está en estado `APROBADO`, el webhook retorna 200 sin reprocesar.

---

## Integración con iText (PDF)

`PresupuestoPdfService` genera PDFs de presupuesto con:
- Datos del cliente y equipo
- Detalle de repuestos con precios
- Total y fecha
- Logo del taller (si existe)

El PDF se retorna como `application/pdf` en el response.

---

## Operaciones Transaccionales

| Operación | `@Transactional` | Motivo |
|-----------|-----------------|--------|
| `crearOrden()` | Sí | Asocia cliente, equipo y técnico en una sola operación atómica |
| `cambiarEstado()` | Sí | Lee y escribe la orden en una transacción |
| `agregarRepuesto()` | Sí | Descuenta stock Y crea OrdenRepuesto Y actualiza presupuesto. Si falla alguno, rollback total |
| `registrarCobro()` | Sí | Crea cobro Y actualiza estado de orden en una sola transacción |
| `procesarPagoAprobado()` | Sí | Actualiza cobro Y orden atómicamente al recibir webhook |
| `listar*()` | `readOnly=true` | Optimización: no abre transacción de escritura |

---

## Cómo Correr Localmente

```bash
# 1. Levantar PostgreSQL (necesario)
docker compose up db -d

# 2. Configurar variables de entorno
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=tallersoft
export DB_USER=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=clave_secreta_minimo_256_bits
export MP_ACCESS_TOKEN=tu_token_de_mercadopago
export WEBHOOK_BASE_URL=http://localhost:8080

# 3. Correr la aplicación
cd backend
mvn spring-boot:run
```

## Cómo Correr con Docker

```bash
docker compose up backend -d
```

## Cómo Correr los Tests

```bash
cd backend
mvn test
# Con reporte de cobertura:
mvn test jacoco:report
```
