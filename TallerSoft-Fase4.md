# TallerSoft — Plan de Desarrollo: Módulo 4 — Stock de Repuestos

> Este documento es el input para que Claude Code genere un plan de desarrollo detallado del Módulo 4 (Stock de Repuestos) del sistema TallerSoft.
> Los módulos 1 (Auth), 2 (Clientes/Equipos) y 3 (Órdenes de Trabajo) ya están implementados.

---

## Contexto del proyecto

**TallerSoft** es un ERP liviano para talleres de servicio técnico (electrónica, celulares, PCs, electrodomésticos). Está desarrollado como trabajo final de la Tecnicatura Universitaria en Programación — UTN FRC — 2026.

### Arquitectura

```
Frontend Angular 17 (PWA)
        ↓ HTTP/REST
API Gateway (Spring Cloud Gateway — puerto 8080)
        ↓
Core Service (Spring Boot 3 / Java 21 — puerto 8081)
        ↓
PostgreSQL 16 (puerto 5432)
```

El Analytics Service (Python/FastAPI — puerto 8082) consume la BD en modo solo lectura y expone endpoints analíticos. El Módulo 4 impacta directamente en los endpoints analíticos de stock.

### Stack tecnológico relevante para este módulo

| Capa | Tecnología |
|---|---|
| Frontend | Angular 17, Angular Material, ApexCharts |
| Backend | Java 21, Spring Boot 3, Spring Data JPA, MapStruct |
| Seguridad | Spring Security + JWT (`@PreAuthorize`) |
| ORM | Hibernate / JPA |
| Base de datos | PostgreSQL 16 |
| Analytics | Python 3.11, FastAPI, Pandas, SQLAlchemy (solo lectura) |
| Testing backend | JUnit 5 + Mockito |
| Testing frontend | Jasmine + Karma |
| Build | Maven 3.9+ |

---

## Estado actual del proyecto

### Ya implementado (no tocar)

- **Módulo 1 — Auth:** JWT, roles (ADMIN, TECNICO, RECEPCION), `SecurityConfig`, `JwtFilter`, `JwtInterceptor` Angular, `AuthGuard`.
- **Módulo 2 — Clientes y Equipos:** entidades `Cliente` y `Equipo`, ABM completo, búsqueda con debounce, historial de órdenes por cliente.
- **Módulo 3 — Órdenes de Trabajo:** entidad `OrdenTrabajo`, máquina de estados (PENDIENTE → EN_PROCESO → LISTO → ENTREGADO), Kanban drag & drop, asignación de técnico, endpoint `POST /api/ordenes/{id}/repuestos` que **ya existe pero depende del Módulo 4** para descontar stock.

### Entidades ya definidas en la BD (scripts ya ejecutados)

```sql
CREATE TABLE repuestos (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    categoria       VARCHAR(80),
    precio          NUMERIC(10,2) NOT NULL,
    stock_actual    INTEGER DEFAULT 0,
    stock_minimo    INTEGER DEFAULT 5,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE orden_repuestos (
    id          BIGSERIAL PRIMARY KEY,
    orden_id    BIGINT REFERENCES ordenes_trabajo(id),
    repuesto_id BIGINT REFERENCES repuestos(id),
    cantidad    INTEGER NOT NULL,
    precio_unit NUMERIC(10,2) NOT NULL
);
```

---

## Alcance del Módulo 4

### Funcionalidades a implementar

1. **ABM de repuestos** — crear, editar, listar y buscar repuestos con nombre, categoría, precio y stock.
2. **Descuento automático de stock** — al asociar un repuesto a una orden de trabajo, el `stock_actual` se reduce en la misma transacción.
3. **Alerta visual de stock crítico** — badge rojo cuando `stock_actual <= stock_minimo`.
4. **Widget de stock crítico en el Dashboard** — listado de repuestos bajo el mínimo visible en la pantalla principal.
5. **Endpoints analíticos de stock** — en el Analytics Service (Python), exponer datos de stock crítico y repuestos más usados.

### Fuera de alcance (no implementar en este módulo)

- Ajuste manual de stock (inventario): no está en los requisitos de este sprint.
- Historial de movimientos de stock: no está contemplado en el modelo de datos actual.
- Notificaciones externas (email/push) por stock bajo: explícitamente excluido del README.
- Módulo 5 (Caja/Facturación) y Módulo 6 (Asistente IA): sprints posteriores.

---

## Reglas de negocio críticas

> Claude Code debe respetar estas reglas en toda la implementación.

1. **Stock nunca negativo:** antes de descontar stock en `repuestos`, validar que `stock_actual >= cantidad`. Si no hay stock suficiente, lanzar excepción con mensaje descriptivo y hacer rollback completo.
2. **Transaccionalidad obligatoria:** el registro en `orden_repuestos` y el descuento en `repuestos.stock_actual` deben ocurrir en la **misma transacción JPA** (`@Transactional`). Nunca desacoplarlos.
3. **Alerta solo visual:** la alerta de stock mínimo es un badge rojo en el frontend. No dispara ninguna notificación externa.
4. **Stock mínimo configurable por repuesto:** el campo `stock_minimo` es por fila, no un valor global.
5. **Cobro aprobado no elimina datos de orden_repuestos:** los repuestos asociados a una orden son inmutables una vez la orden está en estado `ENTREGADO`.

---

## Estructura de archivos esperada

### Backend — Core Service (`backend/src/main/java/com/tallersoft/`)

```
model/
  Repuesto.java           # Entidad JPA
  OrdenRepuesto.java      # Entidad JPA (tabla orden_repuestos)

dto/
  RepuestoRequest.java    # DTO entrada: nombre, categoría, precio, stockActual, stockMinimo
  RepuestoResponse.java   # DTO salida: todos los campos + flag booleano stockCritico
  OrdenRepuestoRequest.java  # DTO para asociar repuesto a orden: repuestoId, cantidad
  OrdenRepuestoResponse.java # DTO de respuesta con precioUnit calculado

repository/
  RepuestoRepository.java       # JPA Repository (incluye findByStockCritico)
  OrdenRepuestoRepository.java  # JPA Repository

service/
  RepuestoService.java      # Lógica de negocio + validación stock
  OrdenRepuestoService.java # Lógica de asociación con transacción

mapper/
  RepuestoMapper.java       # MapStruct: Repuesto ↔ RepuestoRequest/Response

controller/
  RepuestoController.java   # REST endpoints de repuestos
```

### Frontend — Angular (`frontend/src/app/modules/stock/`)

```
stock/
  stock.module.ts
  stock-routing.module.ts
  components/
    stock-list/             # Listado con filtros y badge de stock crítico
    stock-form/             # Formulario ABM (crear/editar)
    stock-detail/           # Detalle de un repuesto (opcional)
  services/
    repuesto.service.ts     # HTTP calls al backend
  models/
    repuesto.model.ts       # Interfaces TypeScript
```

### Analytics Service — Python (`analytics/app/`)

```
routers/
  stock.py         # Endpoints GET /analytics/stock/critico y /analytics/stock/mas-usados

services/
  analytics_service.py    # Agregar funciones: get_stock_critico(), get_repuestos_mas_usados()
```

---

## Endpoints a implementar

### Core Service (Spring Boot — ruteados por Gateway en `/api/...`)

```
GET    /api/repuestos                    → Listar todos (?critico=true para filtrar solo alertas)
GET    /api/repuestos/{id}               → Detalle de un repuesto
POST   /api/repuestos                    → Crear repuesto
PUT    /api/repuestos/{id}               → Editar repuesto
POST   /api/ordenes/{id}/repuestos       → Asociar repuesto a orden (descuenta stock)
```

**Control de acceso por rol:**
- `GET /api/repuestos` y `GET /api/repuestos/{id}`: roles ADMIN, RECEPCION, TECNICO.
- `POST`, `PUT` en `/api/repuestos`: solo ADMIN.
- `POST /api/ordenes/{id}/repuestos`: roles ADMIN y TECNICO.

### Analytics Service (FastAPI — ruteados por Gateway en `/analytics/...`)

```
GET /analytics/stock/critico          → Repuestos con stock_actual <= stock_minimo
GET /analytics/stock/mas-usados       → Top N repuestos más utilizados en órdenes (últimos 30 días)
```

---

## DTOs detallados

### `RepuestoRequest`
```json
{
  "nombre": "Pantalla OLED iPhone 13",
  "categoria": "Pantallas",
  "precio": 45000.00,
  "stockActual": 3,
  "stockMinimo": 5
}
```

### `RepuestoResponse`
```json
{
  "id": 1,
  "nombre": "Pantalla OLED iPhone 13",
  "categoria": "Pantallas",
  "precio": 45000.00,
  "stockActual": 3,
  "stockMinimo": 5,
  "stockCritico": true,
  "createdAt": "2026-01-15T10:30:00"
}
```

### `OrdenRepuestoRequest`
```json
{
  "repuestoId": 1,
  "cantidad": 1
}
```

### `OrdenRepuestoResponse`
```json
{
  "id": 10,
  "ordenId": 5,
  "repuestoId": 1,
  "nombreRepuesto": "Pantalla OLED iPhone 13",
  "cantidad": 1,
  "precioUnit": 45000.00,
  "subtotal": 45000.00
}
```

---

## Comportamiento esperado del servicio de stock

```java
// Pseudocódigo de la lógica en OrdenRepuestoService

@Transactional
public OrdenRepuestoResponse agregarRepuestoAOrden(Long ordenId, OrdenRepuestoRequest request) {
    // 1. Buscar la orden. Lanzar NotFoundException si no existe.
    // 2. Verificar que la orden no esté en estado ENTREGADO.
    //    Si está ENTREGADO, lanzar excepción (la orden ya fue cerrada).
    // 3. Buscar el repuesto. Lanzar NotFoundException si no existe.
    // 4. Verificar stock: si repuesto.stockActual < request.cantidad → lanzar StockInsuficienteException.
    // 5. Descontar stock: repuesto.stockActual -= request.cantidad
    // 6. Guardar repuesto actualizado.
    // 7. Crear y guardar OrdenRepuesto con precioUnit = repuesto.precio al momento del registro.
    // 8. Retornar OrdenRepuestoResponse.
    // Si cualquier paso falla → rollback automático por @Transactional.
}
```

---

## Componente Angular: listado de repuestos

### Comportamiento esperado

- Tabla con columnas: Nombre, Categoría, Precio, Stock Actual, Stock Mínimo, Acciones.
- Los repuestos con `stockCritico = true` deben mostrar un **badge rojo** en la columna de Stock Actual.
- Filtro por nombre con debounce de 300ms (igual que el módulo de clientes).
- Checkbox o toggle para mostrar "Solo stock crítico" (llama al endpoint con `?critico=true`).
- Botones de acción: Editar (abre formulario), Ver detalle.
- Botón "Nuevo Repuesto" visible solo para rol ADMIN.
- Usar `MatTable`, `MatBadge`, `MatChip` o similar de Angular Material para los indicadores visuales.

---

## Widget de stock crítico en el Dashboard

El módulo de dashboard ya existe (`frontend/src/app/modules/dashboard/`). Debe agregarse un widget/card que:

- Consuma el endpoint `GET /api/repuestos?critico=true` o `GET /analytics/stock/critico`.
- Muestre la lista de repuestos bajo stock mínimo con nombre, stock actual y stock mínimo.
- Si no hay repuestos críticos, mostrar un mensaje positivo ("Todos los repuestos tienen stock suficiente").
- Actualizar el dato al cargar el dashboard (no requiere polling en tiempo real).

---

## Analytics Service — Python

### Función `get_stock_critico(db)`

```python
# Debe retornar lista de repuestos donde stock_actual <= stock_minimo
# Ordenados por (stock_actual - stock_minimo) ASC (los más críticos primero)
# Campos: id, nombre, categoria, stock_actual, stock_minimo, diferencia
```

### Función `get_repuestos_mas_usados(db, dias=30, top=10)`

```python
# JOIN entre orden_repuestos y repuestos
# Filtrar por órdenes creadas en los últimos `dias` días
# Agrupar por repuesto_id, sumar cantidades
# Retornar top N con: id, nombre, categoria, total_usado
```

---

## Tests a implementar

### Backend — JUnit 5 + Mockito

Archivo: `src/test/java/com/tallersoft/service/RepuestoServiceTest.java`

Casos mínimos requeridos:
- `crearRepuesto_exitoso()`
- `editarRepuesto_exitoso()`
- `listarRepuestos_soloStockCritico_retornaSoloCriticos()`
- `agregarRepuestoAOrden_stockSuficiente_descuentaStock()`
- `agregarRepuestoAOrden_stockInsuficiente_lanzaExcepcion()`
- `agregarRepuestoAOrden_ordenEntregada_lanzaExcepcion()`

### Frontend — Jasmine + Karma

Archivo: `src/app/modules/stock/services/repuesto.service.spec.ts`

Casos mínimos requeridos:
- `getRepuestos_llamaAlEndpointCorrecto()`
- `getRepuestosCriticos_pasaParametroCritico()`
- `crearRepuesto_enviaRequestCorrectamente()`

---

## Convenciones del proyecto (resumen)

- **DTOs separados** para request y response. Nunca exponer entidades JPA en controllers.
- **MapStruct** para mapeo entidad ↔ DTO.
- **Baja lógica** para eliminaciones (campo `activo`), aunque repuestos en esta versión no tienen baja lógica; se editan pero no se eliminan.
- **GlobalExceptionHandler** ya existe en `exception/GlobalExceptionHandler.java`. Agregar ahí `StockInsuficienteException` y el handler correspondiente (HTTP 409 Conflict).
- **Ramas Git:** `feature/modulo4-stock` para el desarrollo completo de este módulo.
- **Commits** en formato Conventional Commits: `feat(stock): ...`, `test(stock): ...`, `fix(stock): ...`

---

## Dependencias entre módulos

```
Módulo 4 (Stock)
  ← depende de → Módulo 1 (Auth): usa JWT y @PreAuthorize
  ← depende de → Módulo 3 (Órdenes): el endpoint POST /api/ordenes/{id}/repuestos vive
                 lógicamente en el contexto de órdenes pero consume el servicio de stock
  → impacta en → Dashboard: widget de stock crítico
  → impacta en → Analytics Service: endpoints /analytics/stock/...
  → impacta en → Módulo 6 (Asistente IA): el contexto del asistente incluye repuestos críticos
```

---

## Criterios de aceptación del módulo

El Módulo 4 se considera completo cuando:

- [ ] Un ADMIN puede crear, editar y listar repuestos desde el frontend.
- [ ] La lista de repuestos muestra badge rojo en los que tienen stock crítico.
- [ ] El filtro "Solo críticos" funciona correctamente.
- [ ] Un TECNICO puede agregar un repuesto a una orden activa desde el módulo de órdenes.
- [ ] Al agregar el repuesto, el stock se descuenta correctamente en la BD.
- [ ] Si no hay stock suficiente, el frontend muestra un mensaje de error claro.
- [ ] El widget de stock crítico aparece en el Dashboard con datos reales.
- [ ] Los endpoints `GET /analytics/stock/critico` y `GET /analytics/stock/mas-usados` responden correctamente.
- [ ] Todos los tests unitarios definidos pasan (`mvn test` verde).
- [ ] Los endpoints están documentados en Swagger (`http://localhost:8081/swagger-ui.html`).

---

*TallerSoft — Trabajo Final Integrador — TUP UTN FRC 2026*