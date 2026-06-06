-- Migration: add mp_qr_base64 column to cobros table
-- Run this on existing databases that already have the cobros table
ALTER TABLE cobros ADD COLUMN IF NOT EXISTS mp_qr_base64 TEXT;
