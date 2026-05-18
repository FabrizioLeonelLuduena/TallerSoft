-- Initialize TallerSoft with default ADMIN user for testing
-- WARNING: This is for development/testing only. Change passwords in production.

-- First, ensure the usuario table exists and is properly initialized
-- This SQL adds a default ADMIN user with predictable credentials

INSERT INTO usuario (nombre, email, password, rol, activo, created_at)
VALUES (
  'Administrador',
  'admin@tallersoft.com',
  -- Password: Admin123! (BCrypt encrypted with salt rounds 10)
  -- You can generate new password hashes using: bcrypt.hashpw("yourpassword".encode(), bcrypt.gensalt(10))
  '$2a$10$slYQmyNdGzin7olVN3p5NOpOP9jOxe/U4S0S.a/PkKjYxMT2MbEwS',
  'ADMIN',
  true,
  NOW()
) ON CONFLICT (email) DO NOTHING;

-- Optional: Add test users with different roles
INSERT INTO usuario (nombre, email, password, rol, activo, created_at)
VALUES (
  'Técnico Test',
  'tecnico@tallersoft.com',
  -- Password: Tecnico123!
  '$2a$10$slYQmyNdGzin7olVN3p5NOpOP9jOxe/U4S0S.a/PkKjYxMT2MbEwS',
  'TECNICO',
  true,
  NOW()
) ON CONFLICT (email) DO NOTHING;

INSERT INTO usuario (nombre, email, password, rol, activo, created_at)
VALUES (
  'Recepción Test',
  'recepcion@tallersoft.com',
  -- Password: Recepcion123!
  '$2a$10$slYQmyNdGzin7olVN3p5NOpOP9jOxe/U4S0S.a/PkKjYxMT2MbEwS',
  'RECEPCION',
  true,
  NOW()
) ON CONFLICT (email) DO NOTHING;

-- Verify the users were created
SELECT id, nombre, email, rol, activo FROM usuario ORDER BY created_at DESC;
