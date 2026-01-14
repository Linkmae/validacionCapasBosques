-- Script para crear usuario específico para el servicio SAF
-- Ejecutar como superusuario de PostgreSQL

-- 1. Crear usuario para la aplicación
CREATE USER saf_app WITH PASSWORD 'saf_app_2026';

-- 2. Otorgar permisos en base de datos saf_interconexion
GRANT CONNECT ON DATABASE saf_interconexion TO saf_app;

\c saf_interconexion

-- Permisos en esquema public
GRANT USAGE ON SCHEMA public TO saf_app;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO saf_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO saf_app;

-- Permisos para tablas futuras
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
GRANT SELECT, INSERT, UPDATE ON TABLES TO saf_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public 
GRANT USAGE, SELECT ON SEQUENCES TO saf_app;

-- 3. Otorgar permisos en base de datos saf_postgis
\c saf_postgis

GRANT CONNECT ON DATABASE saf_postgis TO saf_app;
GRANT USAGE ON SCHEMA public TO saf_app;

-- Solo lectura para capas (el servicio no modifica capas)
GRANT SELECT ON ALL TABLES IN SCHEMA public TO saf_app;

-- Permisos para tablas futuras en saf_postgis
ALTER DEFAULT PRIVILEGES IN SCHEMA public 
GRANT SELECT ON TABLES TO saf_app;

-- 4. Verificar permisos
\c saf_interconexion
SELECT 
    grantee,
    table_schema,
    table_name,
    string_agg(privilege_type, ', ') as privileges
FROM information_schema.table_privileges
WHERE grantee = 'saf_app'
GROUP BY grantee, table_schema, table_name
ORDER BY table_schema, table_name;

\c saf_postgis
SELECT 
    grantee,
    table_schema,
    table_name,
    string_agg(privilege_type, ', ') as privileges
FROM information_schema.table_privileges
WHERE grantee = 'saf_app'
GROUP BY grantee, table_schema, table_name
ORDER BY table_schema, table_name;

-- 5. Información del usuario creado
\du saf_app

SELECT 'Usuario saf_app creado exitosamente' as status,
       'Usuario: saf_app' as credentials,
       'Password: saf_app_2026' as password_info;
