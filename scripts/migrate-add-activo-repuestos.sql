-- Migration: add activo column to repuestos table (soft delete support)
ALTER TABLE repuestos ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT true;

-- All existing repuestos start as active
UPDATE repuestos SET activo = true WHERE activo IS NULL;

CREATE INDEX IF NOT EXISTS idx_repuestos_activo ON repuestos(activo);
