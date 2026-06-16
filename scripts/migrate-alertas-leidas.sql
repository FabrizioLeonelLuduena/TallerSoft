-- Migración: tabla para persistir el estado de alertas leídas por usuario
-- Ejecutar manualmente en deployments existentes: psql -U tallersoft -d tallersoft -f migrate-alertas-leidas.sql
-- En docker compose fresh: incluido como 03-migrate en docker-compose.yml

CREATE TABLE IF NOT EXISTS alertas_leidas (
    id           BIGSERIAL PRIMARY KEY,
    usuario_id   BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    alerta_key   VARCHAR(255) NOT NULL,
    leida_en     TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, alerta_key)
);

CREATE INDEX IF NOT EXISTS idx_alertas_leidas_usuario ON alertas_leidas(usuario_id);
