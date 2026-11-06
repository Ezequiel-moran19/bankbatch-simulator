-- Preparacion de una base vacia para BankBatch.
-- Se ejecuta una vez por base, como superusuario, antes de correr las migraciones.
-- Requiere que existan los roles bankbatch_migraciones y bankbatch_app.
-- Uso: psql -U postgres -h localhost -d NOMBRE_BASE -f base-datos/preparar-base.sql

ALTER SCHEMA public OWNER TO bankbatch_migraciones;
GRANT USAGE ON SCHEMA public TO bankbatch_app;

ALTER DEFAULT PRIVILEGES FOR ROLE bankbatch_migraciones IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO bankbatch_app;
ALTER DEFAULT PRIVILEGES FOR ROLE bankbatch_migraciones IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO bankbatch_app;
