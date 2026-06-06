# Base de Datos — TallerSoft

## Versión: PostgreSQL 16

---

## Diagrama Entidad-Relación (ERD)

```
┌──────────────┐       ┌───────────────────┐       ┌─────────────┐
│   usuarios   │       │  ordenes_trabajo  │       │   equipos   │
├──────────────┤  1:N  ├───────────────────┤  N:1  ├─────────────┤
│ id (PK)      │◄──────│ tecnico_id (FK)   │──────►│ id (PK)     │
│ nombre       │       │ id (PK)           │       │ cliente_id  │
│ email        │       │ equipo_id (FK) ───┘       │ tipo        │
│ password     │       │ cliente_id (FK) ──┐       │ marca       │
│ rol          │       │ falla_reportada   │       │ modelo      │
│ activo       │       │ diagnostico       │       │ numero_serie│
│ created_at   │       │ estado            │       └──────┬──────┘
└──────────────┘       │ prioridad         │              │
                       │ presupuesto       │              │ N:1
                       │ created_at        │       ┌──────▼──────┐
                       │ updated_at        │       │   clientes  │
                       └─────────┬─────────┘       ├─────────────┤
                                 │                 │ id (PK)     │
                                 │ 1:N             │ nombre      │
                       ┌─────────▼──────────┐      │ telefono    │
                       │  orden_repuestos   │      │ email       │
                       ├────────────────────┤      │ direccion   │
                       │ id (PK)            │      │ activo      │
                       │ orden_id (FK)      │      │ created_at  │
                       │ repuesto_id (FK)   │      └─────────────┘
                       │ cantidad           │
                       │ precio_unit        │
                       └─────────┬──────────┘
                                 │ N:1
                       ┌─────────▼──────────┐
                       │    repuestos        │
                       ├────────────────────┤
                       │ id (PK)            │
                       │ nombre             │
                       │ categoria          │
                       │ precio             │
                       │ stock_actual       │
                       │ stock_minimo       │
                       │ created_at         │
                       └────────────────────┘

┌──────────────────────┐
│       cobros         │
├──────────────────────┤
│ id (PK)              │
│ orden_id (FK) ───────►── ordenes_trabajo
│ monto                │
│ monto_recibido       │
│ vuelto               │
│ medio_pago           │
│ estado_pago          │
│ mp_payment_id        │
│ mp_link_pago         │
│ mp_qr_image_url      │
│ created_at           │
└──────────────────────┘
```

---

## Descripción de Tablas

### `usuarios`
| Campo | Tipo | Constraint | Descripción |
|-------|------|-----------|-------------|
| `id` | BIGSERIAL | PK | ID autoincremental |
| `nombre` | VARCHAR(100) | NOT NULL | Nombre completo del empleado |
| `email` | VARCHAR(150) | UNIQUE, NOT NULL | Email para login |
| `password` | VARCHAR(255) | NOT NULL | Hash BCrypt de la contraseña |
| `rol` | VARCHAR(20) | CHECK(ADMIN/TECNICO/RECEPCION) | Rol del sistema |
| `activo` | BOOLEAN | NOT NULL, DEFAULT true | Baja lógica |
| `created_at` | TIMESTAMP | NOT NULL | Fecha de creación |

---

### `clientes`
| Campo | Tipo | Constraint | Descripción |
|-------|------|-----------|-------------|
| `id` | BIGSERIAL | PK | ID autoincremental |
| `nombre` | VARCHAR(100) | NOT NULL | Nombre del cliente |
| `telefono` | VARCHAR(20) | — | Teléfono de contacto |
| `email` | VARCHAR(150) | UNIQUE NULLS NOT DISTINCT | Email (puede ser NULL) |
| `direccion` | VARCHAR(200) | — | Dirección |
| `activo` | BOOLEAN | NOT NULL, DEFAULT true | Baja lógica |
| `created_at` | TIMESTAMP | NOT NULL | Fecha de alta |

---

### `equipos`
| Campo | Tipo | Constraint | Descripción |
|-------|------|-----------|-------------|
| `id` | BIGSERIAL | PK | ID autoincremental |
| `cliente_id` | BIGINT | FK → clientes | Dueño del equipo |
| `tipo` | VARCHAR(50) | NOT NULL | Tipo de equipo (Celular, Notebook, etc.) |
| `marca` | VARCHAR(50) | — | Marca |
| `modelo` | VARCHAR(100) | — | Modelo |
| `numero_serie` | VARCHAR(100) | — | Número de serie |
| `observaciones` | TEXT | — | Notas adicionales |

---

### `ordenes_trabajo`
| Campo | Tipo | Constraint | Descripción |
|-------|------|-----------|-------------|
| `id` | BIGSERIAL | PK | ID autoincremental |
| `equipo_id` | BIGINT | FK → equipos, NOT NULL | Equipo a reparar |
| `cliente_id` | BIGINT | FK → clientes, NOT NULL | Propietario del equipo |
| `tecnico_id` | BIGINT | FK → usuarios, ON DELETE SET NULL | Técnico asignado |
| `falla_reportada` | TEXT | NOT NULL | Descripción del problema |
| `diagnostico` | TEXT | — | Diagnóstico técnico (requerido para LISTO) |
| `estado` | VARCHAR(20) | CHECK(PENDIENTE/EN_PROCESO/LISTO/ENTREGADO) | Estado actual |
| `prioridad` | VARCHAR(10) | CHECK(BAJA/NORMAL/ALTA), DEFAULT NORMAL | Urgencia |
| `presupuesto` | NUMERIC(10,2) | DEFAULT 0 | Total de repuestos usados |
| `created_at` | TIMESTAMP | NOT NULL, updatable=false | Fecha de ingreso |
| `updated_at` | TIMESTAMP | NOT NULL | Última modificación (@PreUpdate) |

**Transiciones válidas de estado:**
```
PENDIENTE → EN_PROCESO → LISTO → ENTREGADO
```
No se permiten retrocesos ni saltos de estado.

---

### `orden_repuestos`
| Campo | Tipo | Constraint | Descripción |
|-------|------|-----------|-------------|
| `id` | BIGSERIAL | PK | ID autoincremental |
| `orden_id` | BIGINT | FK → ordenes_trabajo, NOT NULL | Orden a la que pertenece |
| `repuesto_id` | BIGINT | FK → repuestos, NOT NULL | Repuesto utilizado |
| `cantidad` | INTEGER | NOT NULL, > 0 | Cantidad usada |
| `precio_unit` | NUMERIC(10,2) | NOT NULL | Precio al momento del uso (snapshot) |

El `precio_unit` es un snapshot del precio al momento de agregar el repuesto, para que cambios futuros de precio no afecten presupuestos ya generados.

---

### `repuestos`
| Campo | Tipo | Constraint | Descripción |
|-------|------|-----------|-------------|
| `id` | BIGSERIAL | PK | ID autoincremental |
| `nombre` | VARCHAR(150) | NOT NULL | Nombre del repuesto |
| `categoria` | VARCHAR(80) | — | Categoría (Pantallas, Baterías, etc.) |
| `precio` | NUMERIC(10,2) | NOT NULL | Precio actual de venta |
| `stock_actual` | INTEGER | NOT NULL, DEFAULT 0 | Unidades en stock |
| `stock_minimo` | INTEGER | NOT NULL, DEFAULT 5 | Umbral de stock crítico |
| `created_at` | TIMESTAMP | NOT NULL | Fecha de creación |

**Stock crítico:** cuando `stock_actual <= stock_minimo`.

---

### `cobros`
| Campo | Tipo | Constraint | Descripción |
|-------|------|-----------|-------------|
| `id` | BIGSERIAL | PK | ID autoincremental |
| `orden_id` | BIGINT | FK → ordenes_trabajo, UNIQUE | Una orden → un cobro activo |
| `monto` | NUMERIC(10,2) | NOT NULL, > 0 | Monto a cobrar |
| `monto_recibido` | NUMERIC(10,2) | — | Monto entregado (EFECTIVO) |
| `vuelto` | NUMERIC(10,2) | — | Vuelto calculado (EFECTIVO) |
| `medio_pago` | VARCHAR(20) | CHECK(EFECTIVO/TARJETA/MERCADOPAGO) | Medio de pago |
| `estado_pago` | VARCHAR(20) | CHECK(PENDIENTE/APROBADO/RECHAZADO) | Estado del cobro |
| `mp_payment_id` | VARCHAR(100) | — | ID de pago en MercadoPago |
| `mp_link_pago` | VARCHAR(500) | — | Link de pago (Checkout Pro) |
| `mp_qr_image_url` | VARCHAR(500) | — | URL del QR del POS |
| `created_at` | TIMESTAMP | NOT NULL | Fecha del cobro |

---

## Índices

| Índice | Tabla | Campo(s) | Justificación |
|--------|-------|---------|--------------|
| `idx_usuarios_email` | usuarios | email | Lookup por email en login (O(log n)) |
| `idx_usuarios_rol` | usuarios | rol | Filtrar técnicos en asignación |
| `idx_clientes_nombre` | clientes | nombre | Búsqueda por nombre (LIKE) |
| `idx_clientes_activo` | clientes | activo | Filtrar clientes activos |
| `idx_equipos_cliente_id` | equipos | cliente_id | JOIN con clientes |
| `idx_ordenes_estado` | ordenes_trabajo | estado | Kanban: filtrar por estado |
| `idx_ordenes_tecnico_id` | ordenes_trabajo | tecnico_id | Mis órdenes |
| `idx_ordenes_cliente_id` | ordenes_trabajo | cliente_id | Historial del cliente |
| `idx_ordenes_created_at` | ordenes_trabajo | created_at | GROUP BY en analytics |
| `idx_ordenes_prioridad` | ordenes_trabajo | prioridad | Alertas de alta prioridad |
| `idx_cobros_estado_pago` | cobros | estado_pago | Filtrar APROBADOS en caja |
| `idx_cobros_created_at` | cobros | created_at | GROUP BY en analytics mensual |
| `idx_cobros_mp_payment_id` | cobros | mp_payment_id | Lookup webhook MP (WHERE NOT NULL) |

---

## Baja Lógica

Las siguientes entidades nunca se eliminan físicamente de la BD:

| Entidad | Campo | Valor de baja | Motivo |
|---------|-------|--------------|--------|
| `usuarios` | `activo` | `false` | Auditoría: los empleados que crearon órdenes deben seguir referenciables |
| `clientes` | `activo` | `false` | Historial de órdenes y cobros vinculados |

Las `ordenes_trabajo`, `cobros`, `equipos` y `repuestos` tampoco se eliminan: se mantiene el historial completo para auditoría y analytics.

---

## Script SQL Completo de Creación

```sql
-- ejecutar: psql -U postgres -d tallersoft -f scripts/init-db.sql

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE usuarios (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    email       VARCHAR(150) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    rol         VARCHAR(20) NOT NULL DEFAULT 'RECEPCION',
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_rol CHECK (rol IN ('ADMIN', 'TECNICO', 'RECEPCION'))
);

CREATE TABLE clientes (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    telefono    VARCHAR(20),
    email       VARCHAR(150),
    direccion   VARCHAR(200),
    activo      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_cliente_email UNIQUE NULLS NOT DISTINCT (email)
);

CREATE TABLE equipos (
    id              BIGSERIAL PRIMARY KEY,
    cliente_id      BIGINT NOT NULL,
    tipo            VARCHAR(50) NOT NULL,
    marca           VARCHAR(50),
    modelo          VARCHAR(100),
    numero_serie    VARCHAR(100),
    observaciones   TEXT,
    CONSTRAINT fk_equipos_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes(id) ON DELETE CASCADE
);

CREATE TABLE ordenes_trabajo (
    id              BIGSERIAL PRIMARY KEY,
    equipo_id       BIGINT NOT NULL,
    cliente_id      BIGINT NOT NULL,
    tecnico_id      BIGINT,
    falla_reportada TEXT NOT NULL,
    diagnostico     TEXT,
    estado          VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    prioridad       VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    presupuesto     NUMERIC(10,2) DEFAULT 0.00,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orden_equipo FOREIGN KEY (equipo_id) REFERENCES equipos(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orden_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orden_tecnico FOREIGN KEY (tecnico_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    CONSTRAINT check_estado CHECK (estado IN ('PENDIENTE','EN_PROCESO','LISTO','ENTREGADO')),
    CONSTRAINT check_prioridad CHECK (prioridad IN ('BAJA','NORMAL','ALTA'))
);

CREATE TABLE repuestos (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(150) NOT NULL,
    categoria    VARCHAR(80),
    precio       NUMERIC(10,2) NOT NULL,
    stock_actual INTEGER NOT NULL DEFAULT 0,
    stock_minimo INTEGER NOT NULL DEFAULT 5,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orden_repuestos (
    id          BIGSERIAL PRIMARY KEY,
    orden_id    BIGINT NOT NULL,
    repuesto_id BIGINT NOT NULL,
    cantidad    INTEGER NOT NULL,
    precio_unit NUMERIC(10,2) NOT NULL,
    CONSTRAINT fk_or_orden FOREIGN KEY (orden_id) REFERENCES ordenes_trabajo(id) ON DELETE CASCADE,
    CONSTRAINT fk_or_repuesto FOREIGN KEY (repuesto_id) REFERENCES repuestos(id) ON DELETE RESTRICT,
    CONSTRAINT check_cantidad CHECK (cantidad > 0)
);

CREATE TABLE cobros (
    id              BIGSERIAL PRIMARY KEY,
    orden_id        BIGINT NOT NULL UNIQUE,
    monto           NUMERIC(10,2) NOT NULL,
    monto_recibido  NUMERIC(10,2),
    vuelto          NUMERIC(10,2),
    medio_pago      VARCHAR(20) NOT NULL,
    estado_pago     VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    mp_payment_id   VARCHAR(100),
    mp_link_pago    VARCHAR(500),
    mp_qr_image_url VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cobro_orden FOREIGN KEY (orden_id) REFERENCES ordenes_trabajo(id) ON DELETE CASCADE,
    CONSTRAINT check_medio_pago CHECK (medio_pago IN ('EFECTIVO','TARJETA','MERCADOPAGO')),
    CONSTRAINT check_estado_pago CHECK (estado_pago IN ('PENDIENTE','APROBADO','RECHAZADO')),
    CONSTRAINT check_monto CHECK (monto > 0)
);

-- Índices
CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_rol ON usuarios(rol);
CREATE INDEX idx_clientes_nombre ON clientes(nombre);
CREATE INDEX idx_clientes_activo ON clientes(activo);
CREATE INDEX idx_equipos_cliente_id ON equipos(cliente_id);
CREATE INDEX idx_ordenes_estado ON ordenes_trabajo(estado);
CREATE INDEX idx_ordenes_tecnico_id ON ordenes_trabajo(tecnico_id);
CREATE INDEX idx_ordenes_cliente_id ON ordenes_trabajo(cliente_id);
CREATE INDEX idx_ordenes_created_at ON ordenes_trabajo(created_at);
CREATE INDEX idx_cobros_estado_pago ON cobros(estado_pago);
CREATE INDEX idx_cobros_created_at ON cobros(created_at);
CREATE INDEX idx_cobros_mp_payment_id ON cobros(mp_payment_id) WHERE mp_payment_id IS NOT NULL;
```

---

## Script SQL de Datos de Ejemplo (Seed)

```sql
-- Usuarios (contraseña: "admin123" hasheada con BCrypt)
INSERT INTO usuarios (nombre, email, password, rol) VALUES
  ('Admin Principal', 'admin@tallersoft.com',
   '$2a$10$TzgbkW2VHfQz8LvY.Kj0IuUJrEJ7GJzWfbHi5E1vLB9X8kVCfNkwy', 'ADMIN'),
  ('Carlos Gómez', 'carlos@tallersoft.com',
   '$2a$10$TzgbkW2VHfQz8LvY.Kj0IuUJrEJ7GJzWfbHi5E1vLB9X8kVCfNkwy', 'TECNICO'),
  ('María López', 'maria@tallersoft.com',
   '$2a$10$TzgbkW2VHfQz8LvY.Kj0IuUJrEJ7GJzWfbHi5E1vLB9X8kVCfNkwy', 'RECEPCION');

-- Clientes
INSERT INTO clientes (nombre, telefono, email) VALUES
  ('Juan Pérez', '351-4001234', 'juan@mail.com'),
  ('Ana Rodríguez', '351-4005678', 'ana@mail.com'),
  ('Pedro Martínez', '351-4009012', NULL),
  ('Laura Fernández', '351-4003456', 'laura@mail.com'),
  ('Roberto Sánchez', '351-4007890', NULL);

-- Equipos
INSERT INTO equipos (cliente_id, tipo, marca, modelo) VALUES
  (1, 'Celular', 'Samsung', 'Galaxy S21'),
  (2, 'Notebook', 'Lenovo', 'IdeaPad 5'),
  (3, 'Celular', 'Apple', 'iPhone 13'),
  (4, 'Tablet', 'Samsung', 'Galaxy Tab A8'),
  (5, 'Notebook', 'HP', 'Pavilion 15');

-- Repuestos
INSERT INTO repuestos (nombre, categoria, precio, stock_actual, stock_minimo) VALUES
  ('Pantalla OLED Samsung S21', 'Pantallas', 45000, 3, 5),
  ('Batería iPhone 13', 'Baterías', 8500, 8, 3),
  ('Teclado Lenovo IdeaPad', 'Teclados', 12000, 2, 2),
  ('Pasta térmica Arctic MX-4', 'Insumos', 1200, 15, 5),
  ('Conector de carga USB-C', 'Conectores', 3500, 6, 4),
  ('Pantalla LCD genérica 6"', 'Pantallas', 18000, 4, 3),
  ('Batería Samsung S21', 'Baterías', 11000, 1, 3),
  ('Disipador de calor 65W', 'Refrigeración', 4500, 5, 2),
  ('Memoria RAM DDR4 8GB', 'RAM', 22000, 3, 2),
  ('SSD NVMe 256GB', 'Almacenamiento', 38000, 2, 2);

-- Órdenes de trabajo
INSERT INTO ordenes_trabajo (equipo_id, cliente_id, tecnico_id, falla_reportada, diagnostico, estado, prioridad) VALUES
  (1, 1, 2, 'Pantalla rota tras caída', NULL, 'PENDIENTE', 'ALTA'),
  (2, 2, 2, 'No enciende', 'Conector de carga dañado', 'EN_PROCESO', 'NORMAL'),
  (3, 3, 2, 'Batería no carga', 'Batería degradada al 40%', 'LISTO', 'NORMAL'),
  (4, 4, NULL, 'Pantalla con rayas', NULL, 'PENDIENTE', 'BAJA'),
  (5, 5, 2, 'Sobrecalentamiento', 'Pasta térmica seca, ventilador obstruido', 'ENTREGADO', 'ALTA');

-- Cobros de prueba
INSERT INTO cobros (orden_id, monto, monto_recibido, vuelto, medio_pago, estado_pago) VALUES
  (5, 5700.00, 6000.00, 300.00, 'EFECTIVO', 'APROBADO'),
  (3, 8500.00, NULL, NULL, 'MERCADOPAGO', 'APROBADO');
```

---

## Conexión a la Base de Datos

**Local:**
```bash
psql -U postgres -d tallersoft -h localhost -p 5432
```

**Docker:**
```bash
docker compose exec db psql -U postgres -d tallersoft
```

**Variables de entorno (Core Service):**
```
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tallersoft
DB_USER=postgres
DB_PASSWORD=tu_password_seguro
```

---

## Permisos: Usuario de Solo Lectura para Analytics

```sql
-- Crear usuario de solo lectura para el Analytics Service
CREATE USER analytics_readonly WITH PASSWORD 'password_seguro_analytics';
GRANT CONNECT ON DATABASE tallersoft TO analytics_readonly;
GRANT USAGE ON SCHEMA public TO analytics_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_readonly;

-- Para tablas futuras:
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO analytics_readonly;
```

El Core Service usa el usuario `postgres` (o uno con privilegios de escritura).
El Analytics Service usa `analytics_readonly` (solo SELECT).
