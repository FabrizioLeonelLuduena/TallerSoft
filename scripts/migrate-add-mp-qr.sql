-- Migration: add MercadoPago QR columns to cobros table
-- Run this on existing databases that already have the cobros table
ALTER TABLE cobros ADD COLUMN IF NOT EXISTS mp_qr_base64    TEXT;
ALTER TABLE cobros ADD COLUMN IF NOT EXISTS mp_qr_image_url VARCHAR(500);
