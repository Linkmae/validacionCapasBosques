-- Script para crear las bases de datos del sistema SAF Interconexión
-- Ejecutar como superusuario de PostgreSQL
-- Fecha: 13 de enero de 2026

-- 1. Crear base de datos principal para el servicio de verificación
CREATE DATABASE saf_interconexion
    WITH OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- 2. Crear base de datos para capas PostGIS
CREATE DATABASE saf_postgis
    WITH OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TEMPLATE = template0;

-- 3. Conectar a saf_postgis y habilitar PostGIS
\c saf_postgis

-- Habilitar extensiones PostGIS
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;
CREATE EXTENSION IF NOT EXISTS postgis_raster;

-- 4. Verificar instalación
SELECT PostGIS_Version();

-- 5. Crear usuario de aplicación (opcional, ejecutar create_db_user.sql después)
-- CREATE USER saf_app WITH PASSWORD 'saf_app_2026';