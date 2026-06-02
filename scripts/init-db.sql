-- ================================================================
-- TALLERSOFT DATABASE SCHEMA
-- PostgreSQL 16 DDL
-- ================================================================

-- Create extension for UUID support (if needed)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ================================================================
-- TABLE: usuarios (Sistema users with roles)
-- ================================================================
CREATE TABLE usuarios (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    email               VARCHAR(150) UNIQUE NOT NULL,
    password            VARCHAR(255) NOT NULL,
    rol                 VARCHAR(20) NOT NULL DEFAULT 'RECEPCION',
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_rol CHECK (rol IN ('ADMIN', 'TECNICO', 'RECEPCION'))
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_rol ON usuarios(rol);
CREATE INDEX idx_usuarios_activo ON usuarios(activo);

-- ================================================================
-- TABLE: clientes (Workshop clients)
-- ================================================================
CREATE TABLE clientes (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    telefono            VARCHAR(20),
    email               VARCHAR(150),
    direccion           VARCHAR(200),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_cliente_email UNIQUE NULLS NOT DISTINCT (email)
);

CREATE INDEX idx_clientes_nombre ON clientes(nombre);
CREATE INDEX idx_clientes_telefono ON clientes(telefono);
CREATE INDEX idx_clientes_activo ON clientes(activo);

-- ================================================================
-- TABLE: equipos (Customer equipment)
-- ================================================================
CREATE TABLE equipos (
    id                  BIGSERIAL PRIMARY KEY,
    cliente_id          BIGINT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    tipo                VARCHAR(50) NOT NULL,
    marca               VARCHAR(50),
    modelo              VARCHAR(100),
    numero_serie        VARCHAR(100),
    observaciones       TEXT,
    
    CONSTRAINT fk_equipos_cliente FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) ON DELETE CASCADE
);

CREATE INDEX idx_equipos_cliente_id ON equipos(cliente_id);
CREATE INDEX idx_equipos_tipo ON equipos(tipo);

-- ================================================================
-- TABLE: ordenes_trabajo (Work orders)
-- ================================================================
CREATE TABLE ordenes_trabajo (
    id                  BIGSERIAL PRIMARY KEY,
    equipo_id           BIGINT NOT NULL REFERENCES equipos(id) ON DELETE RESTRICT,
    cliente_id          BIGINT NOT NULL REFERENCES clientes(id) ON DELETE RESTRICT,
    tecnico_id          BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    falla_reportada     TEXT NOT NULL,
    diagnostico         TEXT,
    estado              VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    prioridad           VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    presupuesto         NUMERIC(10,2) DEFAULT 0.00,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_orden_equipo FOREIGN KEY (equipo_id) 
        REFERENCES equipos(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orden_cliente FOREIGN KEY (cliente_id) 
        REFERENCES clientes(id) ON DELETE RESTRICT,
    CONSTRAINT fk_orden_tecnico FOREIGN KEY (tecnico_id) 
        REFERENCES usuarios(id) ON DELETE SET NULL,
    CONSTRAINT check_estado CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'LISTO', 'ENTREGADO')),
    CONSTRAINT check_prioridad CHECK (prioridad IN ('BAJA', 'NORMAL', 'ALTA'))
);

CREATE INDEX idx_ordenes_estado ON ordenes_trabajo(estado);
CREATE INDEX idx_ordenes_tecnico_id ON ordenes_trabajo(tecnico_id);
CREATE INDEX idx_ordenes_cliente_id ON ordenes_trabajo(cliente_id);
CREATE INDEX idx_ordenes_created_at ON ordenes_trabajo(created_at);
CREATE INDEX idx_ordenes_prioridad ON ordenes_trabajo(prioridad);

-- ================================================================
-- TABLE: repuestos (Parts/Spare parts inventory)
-- ================================================================
CREATE TABLE repuestos (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(150) NOT NULL,
    categoria           VARCHAR(80),
    precio              NUMERIC(10,2) NOT NULL,
    stock_actual        INTEGER NOT NULL DEFAULT 0,
    stock_minimo        INTEGER NOT NULL DEFAULT 5,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT check_stock_actual CHECK (stock_actual >= 0),
    CONSTRAINT check_stock_minimo CHECK (stock_minimo >= 0)
);

CREATE INDEX idx_repuestos_nombre ON repuestos(nombre);
CREATE INDEX idx_repuestos_categoria ON repuestos(categoria);
CREATE INDEX idx_repuestos_stock_actual ON repuestos(stock_actual);

-- ================================================================
-- TABLE: orden_repuestos (Join table: parts used in work orders)
-- ================================================================
CREATE TABLE orden_repuestos (
    id                  BIGSERIAL PRIMARY KEY,
    orden_id            BIGINT NOT NULL REFERENCES ordenes_trabajo(id) ON DELETE CASCADE,
    repuesto_id         BIGINT NOT NULL REFERENCES repuestos(id) ON DELETE RESTRICT,
    cantidad            INTEGER NOT NULL,
    precio_unit         NUMERIC(10,2) NOT NULL,
    
    CONSTRAINT fk_orden_repuesto_orden FOREIGN KEY (orden_id) 
        REFERENCES ordenes_trabajo(id) ON DELETE CASCADE,
    CONSTRAINT fk_orden_repuesto_repuesto FOREIGN KEY (repuesto_id) 
        REFERENCES repuestos(id) ON DELETE RESTRICT,
    CONSTRAINT check_cantidad CHECK (cantidad > 0)
);

CREATE INDEX idx_orden_repuestos_orden_id ON orden_repuestos(orden_id);
CREATE INDEX idx_orden_repuestos_repuesto_id ON orden_repuestos(repuesto_id);

-- ================================================================
-- TABLE: cobros (Payments)
-- ================================================================
CREATE TABLE cobros (
    id                  BIGSERIAL PRIMARY KEY,
    orden_id            BIGINT NOT NULL UNIQUE REFERENCES ordenes_trabajo(id) ON DELETE CASCADE,
    monto               NUMERIC(10,2) NOT NULL,
    monto_recibido      NUMERIC(10,2),
    vuelto              NUMERIC(10,2),
    medio_pago          VARCHAR(20) NOT NULL,
    estado_pago         VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    mp_payment_id       VARCHAR(100),
    mp_link_pago        VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cobro_orden FOREIGN KEY (orden_id)
        REFERENCES ordenes_trabajo(id) ON DELETE CASCADE,
    CONSTRAINT check_medio_pago CHECK (medio_pago IN ('EFECTIVO', 'TARJETA', 'MERCADOPAGO')),
    CONSTRAINT check_estado_pago CHECK (estado_pago IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    CONSTRAINT check_monto CHECK (monto > 0)
);

CREATE INDEX idx_cobros_orden_id ON cobros(orden_id);
CREATE INDEX idx_cobros_estado_pago ON cobros(estado_pago);
CREATE INDEX idx_cobros_created_at ON cobros(created_at);
CREATE INDEX idx_cobros_mp_payment_id ON cobros(mp_payment_id) WHERE mp_payment_id IS NOT NULL;

-- ================================================================
-- INITIAL DATA (Optional: seed with test user)
-- ================================================================
-- INSERT INTO usuarios (nombre, email, password, rol, activo)
-- VALUES ('Admin User', 'admin@tallersoft.local', '$2a$10$...', 'ADMIN', true);
