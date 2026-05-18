-- ================================================================
-- CREATE READ-ONLY DATABASE USER FOR ANALYTICS SERVICE
-- PostgreSQL 16
-- ================================================================

-- Create the analytics_reader user with a secure password
CREATE USER analytics_reader WITH PASSWORD 'analytics_reader_secure_password_change_in_production';

-- Grant connect permission to the database
GRANT CONNECT ON DATABASE tallersoft TO analytics_reader;

-- Grant usage on public schema
GRANT USAGE ON SCHEMA public TO analytics_reader;

-- Grant SELECT on all existing tables
GRANT SELECT ON ALL TABLES IN SCHEMA public TO analytics_reader;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO analytics_reader;

-- Explicitly deny write operations for extra security
ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE INSERT, UPDATE, DELETE ON TABLES FROM analytics_reader;

-- Verify permissions
-- SELECT * FROM information_schema.role_table_grants WHERE grantee = 'analytics_reader';
