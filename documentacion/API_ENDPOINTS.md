# Referencia de API — TallerSoft

Todos los endpoints se acceden a través del API Gateway en `http://localhost:8080`.

**Formato de error estándar:**
```json
{ "status": 401, "error": "TOKEN_EXPIRADO", "message": "Tu sesión ha expirado..." }
```

---

## Auth

### POST /auth/login
Autentica un usuario y retorna un JWT.

**Autenticación:** No requerida.

**Request body:**
```json
{ "email": "admin@tallersoft.com", "password": "admin123" }
```

**Respuestas:**

`200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "email": "admin@tallersoft.com",
  "rol": "ADMIN"
}
```

`401 Unauthorized`
```json
{ "status": 401, "error": "UNAUTHORIZED", "message": "Email o contraseña incorrectos" }
```

`400 Bad Request` — body vacío o campos faltantes.

---

### POST /auth/register
Registra un nuevo usuario en el sistema.

**Autenticación:** No requerida (usado en el setup inicial; en producción proteger con ADMIN).

**Request body:**
```json
{
  "nombre": "Carlos Gómez",
  "email": "carlos@tallersoft.com",
  "password": "password123",
  "rol": "TECNICO"
}
```
Valores válidos de `rol`: `ADMIN`, `TECNICO`, `RECEPCION`.

**Respuestas:**

`201 Created`
```json
{ "id": 2, "nombre": "Carlos Gómez", "email": "carlos@tallersoft.com", "rol": "TECNICO", "activo": true }
```

`400 Bad Request` — email duplicado o rol inválido.

---

## Clientes

### GET /api/clientes
Lista clientes activos.

**Autenticación:** JWT. Roles: ADMIN, TECNICO, RECEPCION.

**Query params:**
| Param | Tipo | Descripción |
|-------|------|-------------|
| `nombre` | string | Filtro parcial (LIKE) |

**200 OK**
```json
[
  { "id": 1, "nombre": "Juan Pérez", "telefono": "351-4001234", "email": "juan@mail.com", "direccion": "Córdoba 123", "activo": true }
]
```

---

### GET /api/clientes/{id}
Obtiene un cliente por ID.

**Autenticación:** JWT. Todos los roles.

`200 OK` — objeto cliente (ver arriba).
`404 Not Found` — cliente no existe.

---

### POST /api/clientes
Crea un nuevo cliente.

**Autenticación:** JWT. Roles: ADMIN, RECEPCION.

**Request body:**
```json
{ "nombre": "Ana Rodríguez", "telefono": "351-4005678", "email": "ana@mail.com", "direccion": "Av. Colón 456" }
```

`201 Created` — cliente creado.
`400 Bad Request` — nombre vacío.

---

### PUT /api/clientes/{id}
Actualiza un cliente existente.

**Request body:** igual que POST.

`200 OK` — cliente actualizado.
`404 Not Found`.

---

### DELETE /api/clientes/{id}
Baja lógica: establece `activo = false`.

**Autenticación:** JWT. Roles: ADMIN.

`204 No Content` — cliente dado de baja.
`404 Not Found`.

---

## Equipos

### GET /api/equipos/cliente/{clienteId}
Lista los equipos de un cliente.

**Autenticación:** JWT. Todos los roles.

**200 OK**
```json
[
  { "id": 1, "clienteId": 1, "tipo": "Celular", "marca": "Samsung", "modelo": "Galaxy S21", "numeroSerie": null }
]
```

---

### POST /api/equipos
Crea un equipo asociado a un cliente.

**Request body:**
```json
{ "clienteId": 1, "tipo": "Celular", "marca": "Apple", "modelo": "iPhone 14", "numeroSerie": "XYZABC123" }
```

`201 Created`.
`404 Not Found` — si el cliente no existe.

---

### PUT /api/equipos/{id}
Actualiza un equipo.

**Request body:** igual que POST.
`200 OK` / `404 Not Found`.

---

## Órdenes de Trabajo

### GET /api/ordenes
Lista órdenes con filtros opcionales.

**Autenticación:** JWT. Todos los roles.

**Query params:**
| Param | Tipo | Descripción |
|-------|------|-------------|
| `estado` | string | PENDIENTE / EN_PROCESO / LISTO / ENTREGADO |
| `tecnicoId` | long | Filtrar por técnico |
| `desde` | date | Desde fecha (YYYY-MM-DD) |
| `hasta` | date | Hasta fecha (YYYY-MM-DD) |

**200 OK** — array de órdenes.

---

### GET /api/ordenes/{id}
Obtiene el detalle completo de una orden (incluyendo repuestos).

**200 OK**
```json
{
  "id": 3,
  "equipoId": 3,
  "clienteId": 3,
  "clienteNombre": "Pedro Martínez",
  "tecnicoId": 2,
  "tecnicoNombre": "Carlos Gómez",
  "fallaReportada": "Batería no carga",
  "diagnostico": "Batería degradada al 40%",
  "estado": "LISTO",
  "prioridad": "NORMAL",
  "presupuesto": 8500.00,
  "createdAt": "2025-06-01T10:00:00",
  "updatedAt": "2025-06-03T14:30:00",
  "repuestos": [
    { "id": 1, "repuestoId": 2, "nombreRepuesto": "Batería iPhone 13", "cantidad": 1, "precioUnit": 8500.00, "total": 8500.00 }
  ]
}
```

---

### POST /api/ordenes
Crea una nueva orden de trabajo.

**Autenticación:** JWT. Roles: ADMIN, RECEPCION.

**Request body:**
```json
{
  "equipoId": 1,
  "clienteId": 1,
  "tecnicoId": 2,
  "fallaReportada": "Pantalla rota tras caída",
  "prioridad": "ALTA"
}
```

**Reglas de negocio:**
- El cliente debe estar activo (`activo = true`).
- El equipo debe pertenecer al cliente.

`201 Created` — orden con estado PENDIENTE.
`404 Not Found` — equipo, cliente o técnico no encontrado.
`422 Unprocessable Entity` — cliente inactivo.

---

### PUT /api/ordenes/{id}/estado
Cambia el estado de una orden.

**Autenticación:** JWT. Todos los roles.

**Request body:**
```json
{ "nuevoEstado": "LISTO" }
```

**Reglas de negocio:**
- Solo se permiten transiciones válidas: PENDIENTE→EN_PROCESO→LISTO→ENTREGADO.
- Para pasar a LISTO: debe existir diagnóstico.
- Para ENTREGADO: solo desde LISTO (normalmente vía cobro).

`200 OK` — orden actualizada.
`400 Bad Request` — diagnóstico faltante (para LISTO).
`409 Conflict` — transición de estado inválida.

---

### PUT /api/ordenes/{id}
Actualiza los datos de una orden (falla, diagnóstico, técnico, prioridad).

**Request body:**
```json
{
  "diagnostico": "Pantalla OLED dañada, requiere reemplazo",
  "tecnicoId": 2,
  "prioridad": "ALTA"
}
```

`200 OK`.

---

### POST /api/ordenes/{id}/repuestos
Agrega un repuesto a la orden y descuenta el stock.

**Autenticación:** JWT. Roles: ADMIN, TECNICO.

**Request body:**
```json
{ "repuestoId": 1, "cantidad": 1 }
```

`200 OK` — orden con presupuesto recalculado.
`409 Conflict` — stock insuficiente.
`404 Not Found` — orden o repuesto no encontrado.

---

## Stock (Repuestos)

### GET /api/repuestos
Lista todos los repuestos del inventario.

`200 OK`
```json
[
  { "id": 1, "nombre": "Pantalla OLED Samsung S21", "categoria": "Pantallas", "precio": 45000.00, "stockActual": 3, "stockMinimo": 5, "critico": true }
]
```
El campo `critico: true` indica `stockActual <= stockMinimo`.

---

### POST /api/repuestos
Crea un nuevo repuesto en el inventario.

**Autenticación:** JWT. Roles: ADMIN, RECEPCION.

**Request body:**
```json
{ "nombre": "Batería Samsung A52", "categoria": "Baterías", "precio": 7500.00, "stockActual": 5, "stockMinimo": 3 }
```

`201 Created`.

---

### PUT /api/repuestos/{id}
Actualiza un repuesto (precio, stock, nombre).

**Request body:** igual que POST.
`200 OK` / `404 Not Found`.

---

## Caja / Cobros

### POST /api/cobros
Registra el cobro de una orden.

**Autenticación:** JWT. Roles: ADMIN, RECEPCION.

**Request body (EFECTIVO):**
```json
{ "ordenId": 3, "monto": 8500.00, "medioPago": "EFECTIVO", "montoRecibido": 10000.00 }
```

**Request body (TARJETA):**
```json
{ "ordenId": 3, "monto": 8500.00, "medioPago": "TARJETA" }
```

**Request body (MERCADOPAGO):**
```json
{ "ordenId": 3, "monto": 8500.00, "medioPago": "MERCADOPAGO" }
```

**200 OK (EFECTIVO/TARJETA)**
```json
{ "id": 5, "ordenId": 3, "monto": 8500.00, "medioPago": "EFECTIVO", "estadoPago": "APROBADO", "vuelto": 1500.00 }
```

**200 OK (MERCADOPAGO)**
```json
{ "id": 6, "ordenId": 3, "monto": 8500.00, "medioPago": "MERCADOPAGO", "estadoPago": "PENDIENTE", "mpQrImageUrl": "https://..." }
```

`400 Bad Request` — orden no en estado LISTO, monto insuficiente.
`400 Bad Request` — la orden ya tiene un cobro aprobado.

---

### GET /api/cobros/caja-diaria
Resumen de cobros aprobados del día.

**Query params:**
| Param | Tipo | Default |
|-------|------|---------|
| `fecha` | date | hoy |

`200 OK`
```json
{
  "fecha": "2025-06-06",
  "totalDia": 47500.00,
  "cantidadOrdenes": 6,
  "totalEfectivo": 15000.00,
  "totalTarjeta": 22500.00,
  "totalMercadoPago": 10000.00,
  "cobrosDelDia": [...]
}
```

---

### POST /api/cobros/{id}/confirmar
Confirma manualmente un cobro TARJETA pendiente.

`200 OK` — cobro APROBADO, orden ENTREGADA.
`400 Bad Request` — ya está APROBADO o es MERCADOPAGO.

---

### POST /api/pagos/webhook
Webhook de MercadoPago. Endpoint público (sin JWT).

**Headers requeridos:**
- `x-signature`: `ts=<timestamp>,v1=<hmac_sha256>`
- `x-request-id`: ID del request

**Request body:**
```json
{ "type": "payment", "data": { "id": "12345" } }
```

`200 OK` — procesado o ya procesado (idempotente).
`400 Bad Request` — firma HMAC inválida.

---

## Analytics

Todos los endpoints del Analytics Service. Se acceden como `/analytics/**` a través del Gateway.

### GET /analytics/ordenes/resumen
Ver [MICROSERVICIO_ANALYTICS.md](MICROSERVICIO_ANALYTICS.md) para respuesta completa.

### GET /analytics/ordenes/por-periodo
Parámetros: `agrupacion` (semana|mes), `meses_atras` (1-24).

### GET /analytics/ordenes/tecnicos/rendimiento
Parámetro: `mes_actual` (bool).

### GET /analytics/stock/critico
Sin parámetros.

### GET /analytics/stock/mas-usados
Parámetros: `top` (1-100), `dias` (1-365).

### GET /analytics/caja/resumen-diario
Parámetro: `fecha` (date, default hoy).

### GET /analytics/caja/evolucion-mensual
Parámetro: `meses` (1-24).

### POST /analytics/asistente/consulta
Body: `{ "pregunta": "..." }` (3-500 caracteres).

Ver [MICROSERVICIO_ANALYTICS.md](MICROSERVICIO_ANALYTICS.md) para ejemplos de respuesta.
