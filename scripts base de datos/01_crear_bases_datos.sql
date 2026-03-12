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

