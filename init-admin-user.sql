-- ================================================================
-- SEED: usuario administrador por defecto
-- Solo para desarrollo. Cambiar credenciales en producción.
-- Contraseña: Admin123!
-- ================================================================

INSERT INTO usuarios (nombre, email, password, rol, activo, created_at)
VALUES (
    'Administrador',
    'admin@techsoft.com',
    '$2b$10$xQrH9KN1k1E.ZrRr2eFYL.Dwdrqi8ckYPVByzTqCfUUfb4nTcQQ8y',
    'ADMIN',
    true,
    NOW()
) ON CONFLICT (email) DO NOTHING;
