# TallerSoft — Prompt para prueba de endpoints con Copilot (Sprint 1 y Sprint 2)

## Contexto para Copilot

Eres un asistente de QA trabajando sobre el proyecto **TallerSoft**, un sistema de gestión integral para talleres de servicio técnico. El backend está construido con **Java 21 + Spring Boot 3**, expuesto a través de un **Spring Cloud Gateway** en `http://localhost:8080`. La base de datos es **PostgreSQL 16**.

El sistema tiene tres roles: `ADMIN`, `TECNICO` y `RECEPCION`. La autenticación usa **JWT (HS256)** enviado en el header `Authorization: Bearer {token}`.

Tu tarea es ejecutar una batería completa de pruebas sobre los endpoints de **Sprint 1** (Autenticación, Usuarios, Clientes, Equipos) y **Sprint 2** (Órdenes de Trabajo, Repuestos). Para cada prueba debes:

1. Hacer la llamada HTTP con `curl` o el cliente HTTP que tengas disponible.
2. Verificar el **status code** esperado.
3. Verificar la **estructura del response JSON**.
4. Reportar PASS ✅ o FAIL ❌ con el detalle del error si corresponde.

---

## Variables de entorno a configurar antes de comenzar

```bash
BASE_URL="http://localhost:8080"
ADMIN_TOKEN=""      # se obtiene en el paso 1
TECNICO_TOKEN=""    # se obtiene en el paso 2
RECEPCION_TOKEN=""  # se obtiene en el paso 3
```

---

## SPRINT 1 — Autenticación, Roles, Clientes y Equipos

---

### US 1.1 / 1.3 — Autenticación (AuthController)

#### TEST-AUTH-01: Login con credenciales válidas (ADMIN)
```
POST {BASE_URL}/auth/login
Content-Type: application/json

{
  "email": "admin@tallersoft.com",
  "password": "Admin1234"
}
```
**Esperado:** `200 OK`
**Response debe contener:**
```json
{
  "token": "<string no vacío>",
  "userId": "<long>",
  "email": "admin@tallersoft.com",
  "rol": "ADMIN"
}
```
**Acción:** Guardar el valor de `token` en `ADMIN_TOKEN`.

---

#### TEST-AUTH-02: Login con credenciales válidas (TECNICO)
```
POST {BASE_URL}/auth/login
Body: { "email": "tecnico@tallersoft.com", "password": "Tecnico1234" }
```
**Esperado:** `200 OK` con `"rol": "TECNICO"`. Guardar token en `TECNICO_TOKEN`.

---

#### TEST-AUTH-03: Login con credenciales válidas (RECEPCION)
```
POST {BASE_URL}/auth/login
Body: { "email": "recepcion@tallersoft.com", "password": "Recepcion1234" }
```
**Esperado:** `200 OK` con `"rol": "RECEPCION"`. Guardar token en `RECEPCION_TOKEN`.

---

#### TEST-AUTH-04: Login con email inexistente
```
POST {BASE_URL}/auth/login
Body: { "email": "noexiste@test.com", "password": "cualquiera" }
```
**Esperado:** `401 Unauthorized`
**Response debe contener:** `{ "status": 401, "error": "...", "message": "...", "timestamp": "..." }`

---

#### TEST-AUTH-05: Login con contraseña incorrecta
```
POST {BASE_URL}/auth/login
Body: { "email": "admin@tallersoft.com", "password": "wrongpassword" }
```
**Esperado:** `401 Unauthorized`

---

#### TEST-AUTH-06: Login con body inválido (email mal formado)
```
POST {BASE_URL}/auth/login
Body: { "email": "no-es-email", "password": "Admin1234" }
```
**Esperado:** `400 Bad Request`

---

#### TEST-AUTH-07: Login con campos vacíos
```
POST {BASE_URL}/auth/login
Body: { "email": "", "password": "" }
```
**Esperado:** `400 Bad Request`

---

#### TEST-AUTH-08: Registro de nuevo usuario (ADMIN)
```
POST {BASE_URL}/auth/register
Authorization: Bearer {ADMIN_TOKEN}
Body:
{
  "nombre": "Juan Técnico",
  "email": "juan.tecnico@tallersoft.com",
  "password": "JuanPass99",
  "rol": "TECNICO"
}
```
**Esperado:** `200 OK`
**Response debe contener:** `{ "id": <long>, "nombre": "Juan Técnico", "email": "...", "rol": "TECNICO", "activo": true }`
**Acción:** Guardar el `id` del usuario creado como `USUARIO_TECNICO_ID`.

---

#### TEST-AUTH-09: Registro con email ya existente
```
POST {BASE_URL}/auth/register
Body: { "nombre": "Otro", "email": "admin@tallersoft.com", "password": "Pass1234", "rol": "ADMIN" }
```
**Esperado:** `4xx` (conflicto o bad request con mensaje de email duplicado)

---

#### TEST-AUTH-10: Registro con password menor a 8 caracteres
```
POST {BASE_URL}/auth/register
Body: { "nombre": "Test", "email": "test@test.com", "password": "1234567", "rol": "RECEPCION" }
```
**Esperado:** `400 Bad Request`

---

#### TEST-AUTH-11: Acceso a endpoint protegido sin token
```
GET {BASE_URL}/api/clientes
(sin header Authorization)
```
**Esperado:** `401 Unauthorized`

---

#### TEST-AUTH-12: Acceso a endpoint protegido con token inválido
```
GET {BASE_URL}/api/clientes
Authorization: Bearer token.invalido.aqui
```
**Esperado:** `401 Unauthorized`

---

### US 1.4 / 1.5 — Clientes (ClienteController)

#### TEST-CLI-01: Crear cliente (RECEPCION)
```
POST {BASE_URL}/api/clientes
Authorization: Bearer {RECEPCION_TOKEN}
Body:
{
  "nombre": "Carlos García",
  "telefono": "351-555-0001",
  "email": "carlos@gmail.com",
  "direccion": "Av. Colón 1234, Córdoba"
}
```
**Esperado:** `200 OK`
**Response debe contener:** `{ "id": <long>, "nombre": "Carlos García", "activo": true, "createdAt": "..." }`
**Acción:** Guardar `id` como `CLIENTE_ID_1`.

---

#### TEST-CLI-02: Crear segundo cliente (ADMIN)
```
POST {BASE_URL}/api/clientes
Authorization: Bearer {ADMIN_TOKEN}
Body: { "nombre": "María López", "telefono": "351-555-0002", "email": "maria@gmail.com", "direccion": "San Martín 456" }
```
**Esperado:** `200 OK`. Guardar `id` como `CLIENTE_ID_2`.

---

#### TEST-CLI-03: Crear cliente con nombre vacío (validación)
```
POST {BASE_URL}/api/clientes
Authorization: Bearer {ADMIN_TOKEN}
Body: { "nombre": "", "telefono": "123" }
```
**Esperado:** `400 Bad Request`

---

#### TEST-CLI-04: Crear cliente con email inválido
```
POST {BASE_URL}/api/clientes
Authorization: Bearer {ADMIN_TOKEN}
Body: { "nombre": "Test", "email": "no-es-email" }
```
**Esperado:** `400 Bad Request`

---

#### TEST-CLI-05: Crear cliente con rol TECNICO (debe fallar — sin permiso)
```
POST {BASE_URL}/api/clientes
Authorization: Bearer {TECNICO_TOKEN}
Body: { "nombre": "Intruso", "telefono": "000" }
```
**Esperado:** `403 Forbidden`

---

#### TEST-CLI-06: Listar todos los clientes activos
```
GET {BASE_URL}/api/clientes
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK`, array con al menos 2 clientes. Todos deben tener `"activo": true`.

---

#### TEST-CLI-07: Buscar cliente por nombre (filtro parcial)
```
GET {BASE_URL}/api/clientes?nombre=Carlos
Authorization: Bearer {RECEPCION_TOKEN}
```
**Esperado:** `200 OK`, array con solo los clientes cuyo nombre contiene "Carlos" (case-insensitive).

---

#### TEST-CLI-08: Obtener cliente por ID existente
```
GET {BASE_URL}/api/clientes/{CLIENTE_ID_1}
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK` con los datos de Carlos García.

---

#### TEST-CLI-09: Obtener cliente con ID inexistente
```
GET {BASE_URL}/api/clientes/99999
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `404 Not Found` con estructura `{ "status": 404, "error": "...", "message": "...", "timestamp": "..." }`.

---

#### TEST-CLI-10: Editar cliente (RECEPCION)
```
PUT {BASE_URL}/api/clientes/{CLIENTE_ID_1}
Authorization: Bearer {RECEPCION_TOKEN}
Body: { "nombre": "Carlos García Actualizado", "telefono": "351-999-9999", "email": "carlos@gmail.com", "direccion": "Nueva dirección 999" }
```
**Esperado:** `200 OK`. El campo `nombre` en la respuesta debe ser `"Carlos García Actualizado"`.

---

#### TEST-CLI-11: Editar cliente con rol TECNICO (debe fallar)
```
PUT {BASE_URL}/api/clientes/{CLIENTE_ID_1}
Authorization: Bearer {TECNICO_TOKEN}
Body: { "nombre": "Hack", "telefono": "000" }
```
**Esperado:** `403 Forbidden`

---

#### TEST-CLI-12: Eliminar cliente — soft delete (solo ADMIN)
```
DELETE {BASE_URL}/api/clientes/{CLIENTE_ID_2}
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK` o `204 No Content`

---

#### TEST-CLI-13: Verificar que el cliente eliminado no aparece en el listado
```
GET {BASE_URL}/api/clientes
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK`. El cliente con `CLIENTE_ID_2` NO debe aparecer (o debe tener `"activo": false`).

---

#### TEST-CLI-14: Verificar que el cliente eliminado sigue en la base (no es delete físico)
```
GET {BASE_URL}/api/clientes/{CLIENTE_ID_2}
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK` con `"activo": false`. **No debe retornar 404** — el registro existe físicamente.

---

#### TEST-CLI-15: Eliminar cliente con rol RECEPCION (debe fallar)
```
DELETE {BASE_URL}/api/clientes/{CLIENTE_ID_1}
Authorization: Bearer {RECEPCION_TOKEN}
```
**Esperado:** `403 Forbidden`

---

### US 1.4 / 1.5 — Equipos (EquipoController)

#### TEST-EQP-01: Crear equipo para cliente existente
```
POST {BASE_URL}/api/equipos
Authorization: Bearer {RECEPCION_TOKEN}
Body:
{
  "clienteId": {CLIENTE_ID_1},
  "tipo": "Notebook",
  "marca": "Lenovo",
  "modelo": "ThinkPad E15",
  "numeroSerie": "SN-LNVO-0001",
  "observaciones": "Pantalla rota"
}
```
**Esperado:** `200 OK`
**Response:** `{ "id": <long>, "clienteId": {CLIENTE_ID_1}, "tipo": "Notebook", ... }`
**Acción:** Guardar `id` como `EQUIPO_ID_1`.

---

#### TEST-EQP-02: Crear segundo equipo para el mismo cliente
```
POST {BASE_URL}/api/equipos
Authorization: Bearer {ADMIN_TOKEN}
Body: { "clienteId": {CLIENTE_ID_1}, "tipo": "Celular", "marca": "Samsung", "modelo": "Galaxy A52", "numeroSerie": "SN-SAMS-0002" }
```
**Esperado:** `200 OK`. Guardar `id` como `EQUIPO_ID_2`.

---

#### TEST-EQP-03: Crear equipo sin clienteId (validación)
```
POST {BASE_URL}/api/equipos
Authorization: Bearer {ADMIN_TOKEN}
Body: { "tipo": "TV" }
```
**Esperado:** `400 Bad Request`

---

#### TEST-EQP-04: Crear equipo sin tipo (validación)
```
POST {BASE_URL}/api/equipos
Authorization: Bearer {ADMIN_TOKEN}
Body: { "clienteId": {CLIENTE_ID_1} }
```
**Esperado:** `400 Bad Request`

---

#### TEST-EQP-05: Listar equipos de un cliente
```
GET {BASE_URL}/api/equipos/cliente/{CLIENTE_ID_1}
Authorization: Bearer {TECNICO_TOKEN}
```
**Esperado:** `200 OK`, array con 2 equipos. Todos deben tener `"clienteId": {CLIENTE_ID_1}`.

---

#### TEST-EQP-06: Listar equipos de un cliente sin equipos registrados
```
GET {BASE_URL}/api/equipos/cliente/99999
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK` con array vacío `[]`.

---

#### TEST-EQP-07: Editar equipo (RECEPCION)
```
PUT {BASE_URL}/api/equipos/{EQUIPO_ID_1}
Authorization: Bearer {RECEPCION_TOKEN}
Body: { "clienteId": {CLIENTE_ID_1}, "tipo": "Notebook", "marca": "Lenovo", "modelo": "ThinkPad E15 Gen2", "numeroSerie": "SN-LNVO-0001-UPD" }
```
**Esperado:** `200 OK`. El campo `modelo` debe ser `"ThinkPad E15 Gen2"`.

---

#### TEST-EQP-08: Editar equipo con rol TECNICO (debe fallar)
```
PUT {BASE_URL}/api/equipos/{EQUIPO_ID_1}
Authorization: Bearer {TECNICO_TOKEN}
Body: { "clienteId": {CLIENTE_ID_1}, "tipo": "Hack" }
```
**Esperado:** `403 Forbidden`

---

## SPRINT 2 — Órdenes de Trabajo y Repuestos

---

### Configuración previa Sprint 2

Antes de comenzar, crear un técnico usuario válido y obtener su `id`:
```
Asumir que USUARIO_TECNICO_ID fue obtenido en TEST-AUTH-08.
```

---

### US 2.4 / 2.5 — Repuestos (RepuestoController)

#### TEST-REP-01: Crear repuesto con stock suficiente (ADMIN)
```
POST {BASE_URL}/api/repuestos
Authorization: Bearer {ADMIN_TOKEN}
Body:
{
  "nombre": "Pantalla LCD 15.6 pulgadas",
  "categoria": "Pantallas",
  "precio": 8500.00,
  "stockActual": 10,
  "stockMinimo": 3
}
```
**Esperado:** `200 OK`
**Response:** `{ "id": <long>, "nombre": "Pantalla LCD 15.6 pulgadas", "critico": false, ... }`
**Acción:** Guardar `id` como `REPUESTO_ID_1`.

---

#### TEST-REP-02: Crear repuesto con stock crítico (RECEPCION)
```
POST {BASE_URL}/api/repuestos
Authorization: Bearer {RECEPCION_TOKEN}
Body: { "nombre": "Batería Li-Ion 3000mAh", "categoria": "Baterías", "precio": 2200.00, "stockActual": 2, "stockMinimo": 5 }
```
**Esperado:** `200 OK`
**Response:** `{ "critico": true }` (stockActual 2 <= stockMinimo 5)
**Acción:** Guardar `id` como `REPUESTO_ID_2`.

---

#### TEST-REP-03: Crear repuesto con precio inválido (0.00)
```
POST {BASE_URL}/api/repuestos
Authorization: Bearer {ADMIN_TOKEN}
Body: { "nombre": "Test", "precio": 0.00, "stockActual": 5, "stockMinimo": 2 }
```
**Esperado:** `400 Bad Request` (precio debe ser mayor a 0.01)

---

#### TEST-REP-04: Crear repuesto con rol TECNICO (debe fallar)
```
POST {BASE_URL}/api/repuestos
Authorization: Bearer {TECNICO_TOKEN}
Body: { "nombre": "Test", "precio": 100.00, "stockActual": 5, "stockMinimo": 1 }
```
**Esperado:** `403 Forbidden`

---

#### TEST-REP-05: Listar todos los repuestos
```
GET {BASE_URL}/api/repuestos
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK`, array con al menos 2 repuestos.

---

#### TEST-REP-06: Listar solo repuestos críticos
```
GET {BASE_URL}/api/repuestos?critico=true
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK`. Todos los repuestos en la respuesta deben tener `"critico": true` (stockActual <= stockMinimo).

---

#### TEST-REP-07: Obtener repuesto por ID
```
GET {BASE_URL}/api/repuestos/{REPUESTO_ID_1}
Authorization: Bearer {TECNICO_TOKEN}
```
**Esperado:** `200 OK` con datos de "Pantalla LCD 15.6 pulgadas".

---

#### TEST-REP-08: Obtener repuesto con ID inexistente
```
GET {BASE_URL}/api/repuestos/99999
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `404 Not Found`

---

#### TEST-REP-09: Editar repuesto (ADMIN)
```
PUT {BASE_URL}/api/repuestos/{REPUESTO_ID_1}
Authorization: Bearer {ADMIN_TOKEN}
Body: { "nombre": "Pantalla LCD 15.6 Full HD", "categoria": "Pantallas", "precio": 9200.00, "stockActual": 10, "stockMinimo": 3 }
```
**Esperado:** `200 OK`. `precio` debe ser `9200.00`.

---

### US 2.4 / 2.5 — Órdenes de Trabajo (OrdenTrabajoController)

#### TEST-OT-01: Crear orden de trabajo (RECEPCION)
```
POST {BASE_URL}/api/ordenes
Authorization: Bearer {RECEPCION_TOKEN}
Body:
{
  "equipoId": {EQUIPO_ID_1},
  "clienteId": {CLIENTE_ID_1},
  "tecnicoId": {USUARIO_TECNICO_ID},
  "fallaReportada": "No enciende, pantalla negra al presionar botón de encendido",
  "prioridad": "ALTA"
}
```
**Esperado:** `200 OK`
**Response debe contener:**
```json
{
  "id": <long>,
  "estado": "PENDIENTE",
  "presupuesto": 0,
  "prioridad": "ALTA",
  "diagnostico": null,
  "repuestos": []
}
```
**Acción:** Guardar `id` como `ORDEN_ID_1`.

---

#### TEST-OT-02: Crear segunda orden sin técnico asignado
```
POST {BASE_URL}/api/ordenes
Authorization: Bearer {RECEPCION_TOKEN}
Body: { "equipoId": {EQUIPO_ID_2}, "clienteId": {CLIENTE_ID_1}, "fallaReportada": "No carga la batería", "prioridad": "NORMAL" }
```
**Esperado:** `200 OK` con `"estado": "PENDIENTE"`, `"tecnicoId": null`.
**Acción:** Guardar `id` como `ORDEN_ID_2`.

---

#### TEST-OT-03: Crear orden sin fallaReportada (validación)
```
POST {BASE_URL}/api/ordenes
Authorization: Bearer {RECEPCION_TOKEN}
Body: { "equipoId": {EQUIPO_ID_1}, "clienteId": {CLIENTE_ID_1} }
```
**Esperado:** `400 Bad Request`

---

#### TEST-OT-04: Crear orden sin equipoId (validación)
```
POST {BASE_URL}/api/ordenes
Authorization: Bearer {RECEPCION_TOKEN}
Body: { "clienteId": {CLIENTE_ID_1}, "fallaReportada": "Falla X" }
```
**Esperado:** `400 Bad Request`

---

#### TEST-OT-05: Listar todas las órdenes (ADMIN)
```
GET {BASE_URL}/api/ordenes
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK`, array con al menos 2 órdenes.

---

#### TEST-OT-06: Listar órdenes activas para Kanban
```
GET {BASE_URL}/api/ordenes/activas
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK`. Ninguna orden debe tener `"estado": "ENTREGADO"`.

---

#### TEST-OT-07: Listar mis órdenes (TECNICO)
```
GET {BASE_URL}/api/ordenes/mis-ordenes
Authorization: Bearer {TECNICO_TOKEN}
```
**Esperado:** `200 OK`. Solo deben aparecer órdenes asignadas al técnico autenticado.

---

#### TEST-OT-08: Listar mis órdenes con rol ADMIN (debe fallar — solo TECNICO)
```
GET {BASE_URL}/api/ordenes/mis-ordenes
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `403 Forbidden`

---

#### TEST-OT-09: Obtener orden por ID
```
GET {BASE_URL}/api/ordenes/{ORDEN_ID_1}
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK` con todos los campos de la orden.

---

#### TEST-OT-10: Filtrar órdenes por estado PENDIENTE
```
GET {BASE_URL}/api/ordenes?estado=PENDIENTE
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK`. Todas las órdenes deben tener `"estado": "PENDIENTE"`.

---

### US 2.4 — Máquina de estados (cambios de estado)

#### TEST-EST-01: Transición válida PENDIENTE → EN_PROCESO
```
PUT {BASE_URL}/api/ordenes/{ORDEN_ID_1}/estado
Authorization: Bearer {TECNICO_TOKEN}
Body: { "nuevoEstado": "EN_PROCESO" }
```
**Esperado:** `200 OK` con `"estado": "EN_PROCESO"`.

---

#### TEST-EST-02: Transición inválida EN_PROCESO → PENDIENTE (retroceso no permitido)
```
PUT {BASE_URL}/api/ordenes/{ORDEN_ID_1}/estado
Authorization: Bearer {TECNICO_TOKEN}
Body: { "nuevoEstado": "PENDIENTE" }
```
**Esperado:** `409 Conflict` con mensaje indicando la transición inválida.

---

#### TEST-EST-03: Transición inválida PENDIENTE → ENTREGADO (salto de estado)
```
PUT {BASE_URL}/api/ordenes/{ORDEN_ID_2}/estado
Authorization: Bearer {ADMIN_TOKEN}
Body: { "nuevoEstado": "ENTREGADO" }
```
**Esperado:** `409 Conflict`

---

#### TEST-EST-04: Intentar pasar a LISTO sin diagnóstico (MissingDiagnosticException)
```
PUT {BASE_URL}/api/ordenes/{ORDEN_ID_1}/estado
Authorization: Bearer {TECNICO_TOKEN}
Body: { "nuevoEstado": "LISTO" }
```
**Nota:** La orden está en EN_PROCESO y sin diagnóstico aún.
**Esperado:** `400 Bad Request` con mensaje indicando que falta el diagnóstico.

---

#### TEST-EST-05: Agregar diagnóstico (TECNICO)
```
PUT {BASE_URL}/api/ordenes/{ORDEN_ID_1}/diagnostico
Authorization: Bearer {TECNICO_TOKEN}
Body: { "diagnostico": "Placa base dañada por cortocircuito en chip de alimentación. Se requiere reemplazo de pantalla y revisión de placa." }
```
**Esperado:** `200 OK`. El campo `diagnostico` en la respuesta no debe ser null.

---

#### TEST-EST-06: Agregar diagnóstico vacío (validación)
```
PUT {BASE_URL}/api/ordenes/{ORDEN_ID_1}/diagnostico
Authorization: Bearer {TECNICO_TOKEN}
Body: { "diagnostico": "" }
```
**Esperado:** `400 Bad Request`

---

#### TEST-EST-07: Transición válida EN_PROCESO → LISTO (ahora con diagnóstico)
```
PUT {BASE_URL}/api/ordenes/{ORDEN_ID_1}/estado
Authorization: Bearer {TECNICO_TOKEN}
Body: { "nuevoEstado": "LISTO" }
```
**Esperado:** `200 OK` con `"estado": "LISTO"`.

---

### US 2.4 — Repuestos en órdenes (stock y presupuesto)

#### TEST-REP-OT-01: Agregar repuesto a una orden (TECNICO)
```
POST {BASE_URL}/api/ordenes/{ORDEN_ID_1}/repuestos
Authorization: Bearer {TECNICO_TOKEN}
Body:
{
  "repuestoId": {REPUESTO_ID_1},
  "cantidad": 1
}
```
**Nota:** El repuesto tiene precio 9200.00 y stock 10.
**Esperado:** `200 OK`
**Response debe mostrar:**
- `presupuesto` actualizado a `9200.00`
- `repuestos` con 1 elemento: `{ "nombreRepuesto": "Pantalla LCD 15.6 Full HD", "cantidad": 1, "precioUnit": 9200.00, "total": 9200.00 }`

---

#### TEST-REP-OT-02: Verificar que el stock del repuesto decrementó
```
GET {BASE_URL}/api/repuestos/{REPUESTO_ID_1}
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK` con `"stockActual": 9` (era 10, se usó 1).

---

#### TEST-REP-OT-03: Agregar segundo repuesto y verificar recálculo de presupuesto
```
POST {BASE_URL}/api/ordenes/{ORDEN_ID_1}/repuestos
Authorization: Bearer {TECNICO_TOKEN}
Body: { "repuestoId": {REPUESTO_ID_2}, "cantidad": 1 }
```
**Nota:** El repuesto 2 tiene precio 2200.00.
**Esperado:** `200 OK`
**Response:** `presupuesto` debe ser `11400.00` (9200 + 2200).

---

#### TEST-REP-OT-04: Intentar agregar repuesto con stock insuficiente
```
POST {BASE_URL}/api/ordenes/{ORDEN_ID_2}/repuestos
Authorization: Bearer {TECNICO_TOKEN}
Body: { "repuestoId": {REPUESTO_ID_2}, "cantidad": 10 }
```
**Nota:** El repuesto 2 tiene `stockActual: 1` (era 2, se usó 1 en el paso anterior).
**Esperado:** `409 Conflict` con mensaje de stock insuficiente.

---

#### TEST-REP-OT-05: Verificar que el stock NO decrementó tras el error anterior
```
GET {BASE_URL}/api/repuestos/{REPUESTO_ID_2}
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK` con el mismo `stockActual` que tenía antes del intento fallido (integridad transaccional).

---

#### TEST-REP-OT-06: Agregar repuesto con cantidad 0 (validación)
```
POST {BASE_URL}/api/ordenes/{ORDEN_ID_1}/repuestos
Authorization: Bearer {TECNICO_TOKEN}
Body: { "repuestoId": {REPUESTO_ID_1}, "cantidad": 0 }
```
**Esperado:** `400 Bad Request`

---

#### TEST-REP-OT-07: Listar repuestos de una orden
```
GET {BASE_URL}/api/ordenes/{ORDEN_ID_1}/repuestos
Authorization: Bearer {RECEPCION_TOKEN}
```
**Esperado:** `200 OK`, array con 2 repuestos. Verificar que `total = cantidad * precioUnit` para cada uno.

---

#### TEST-REP-OT-08: Verificar que precioUnit es snapshot (inmutable)
```
1. Editar el precio del REPUESTO_ID_1 a un valor diferente:
   PUT {BASE_URL}/api/repuestos/{REPUESTO_ID_1}
   Body: { "nombre": "Pantalla LCD 15.6 Full HD", "categoria": "Pantallas", "precio": 15000.00, "stockActual": 9, "stockMinimo": 3 }

2. Luego obtener la orden:
   GET {BASE_URL}/api/ordenes/{ORDEN_ID_1}
```
**Esperado:** El `precioUnit` del repuesto en la orden debe seguir siendo `9200.00`, no `15000.00`.

---

### US 2.5 — Completar flujo de estado hasta ENTREGADO

#### TEST-FLUJO-01: Transición LISTO → ENTREGADO (ADMIN)
```
PUT {BASE_URL}/api/ordenes/{ORDEN_ID_1}/estado
Authorization: Bearer {ADMIN_TOKEN}
Body: { "nuevoEstado": "ENTREGADO" }
```
**Esperado:** `200 OK` con `"estado": "ENTREGADO"`.

---

#### TEST-FLUJO-02: Verificar que la orden entregada no aparece en activas
```
GET {BASE_URL}/api/ordenes/activas
Authorization: Bearer {ADMIN_TOKEN}
```
**Esperado:** `200 OK`. La orden `ORDEN_ID_1` **no debe aparecer** en la lista.

---

#### TEST-FLUJO-03: Intentar cambiar estado de una orden ENTREGADA
```
PUT {BASE_URL}/api/ordenes/{ORDEN_ID_1}/estado
Authorization: Bearer {ADMIN_TOKEN}
Body: { "nuevoEstado": "LISTO" }
```
**Esperado:** `409 Conflict` (no hay transición válida desde ENTREGADO).

---

#### TEST-FLUJO-04: Intentar agregar diagnóstico a una orden ENTREGADA
```
PUT {BASE_URL}/api/ordenes/{ORDEN_ID_1}/diagnostico
Authorization: Bearer {TECNICO_TOKEN}
Body: { "diagnostico": "Intento de modificar orden cerrada" }
```
**Esperado:** `409 Conflict` o `400 Bad Request`.

---

## Checklist de verificación final

Al terminar todas las pruebas, verificar que:

- [ ] Todos los tokens JWT se generaron correctamente con `userId`, `email` y `rol` en el payload
- [ ] El soft delete de clientes no elimina físicamente el registro (`activo = false`)
- [ ] Los roles se respetan: TECNICO no puede crear clientes ni repuestos
- [ ] La máquina de estados solo permite las transiciones: PENDIENTE→EN_PROCESO→LISTO→ENTREGADO
- [ ] No es posible pasar a LISTO sin diagnóstico
- [ ] El stock nunca queda negativo (transaccional con bloqueo pesimista)
- [ ] El `precioUnit` en `OrdenRepuesto` no cambia si se actualiza el precio del repuesto
- [ ] El `presupuesto` de la orden se recalcula correctamente como suma de `cantidad * precioUnit`
- [ ] Los errores siguen la estructura estándar: `{ "status", "error", "message", "timestamp" }`
- [ ] Los endpoints sin token retornan `401`, los sin permiso retornan `403`

---

## Notas para Copilot

- Si algún test falla, reportar el status code recibido, el body de la respuesta y el motivo probable del error.
- Ejecutar los tests en el orden definido, ya que muchos dependen de IDs generados en pasos anteriores.
- Si la base de datos está vacía, primero insertar los usuarios iniciales con el endpoint `/auth/register` o con un script SQL de seed.
- El endpoint `/api/pagos/webhook` debe ser accesible **sin JWT** (no cubrir en estas pruebas, pero verificar que no retorna 401).