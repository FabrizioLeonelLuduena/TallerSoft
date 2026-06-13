-- Migration: add stock_bajo column to repuestos table
-- Represents the "low stock" warning threshold (above stock_minimo but below this = BAJO state)
-- Default: stock_minimo * 2, consistent with the application default of 10 (when stock_minimo = 5)
ALTER TABLE repuestos ADD COLUMN IF NOT EXISTS stock_bajo INTEGER NOT NULL DEFAULT 10;

ALTER TABLE repuestos ADD CONSTRAINT check_stock_bajo CHECK (stock_bajo >= 0);

-- Backfill: set stock_bajo = stock_minimo * 2 for all existing rows
UPDATE repuestos SET stock_bajo = stock_minimo * 2;
