-- ================================================================
-- MIGRACIÓN: cobros v2 — columnas adicionales para Módulo 5
-- Ejecutar ANTES de levantar el backend con las nuevas entidades
-- ================================================================

ALTER TABLE cobros ADD COLUMN IF NOT EXISTS monto_recibido NUMERIC(10,2);
ALTER TABLE cobros ADD COLUMN IF NOT EXISTS vuelto         NUMERIC(10,2);
ALTER TABLE cobros ADD COLUMN IF NOT EXISTS mp_link_pago   VARCHAR(500);
