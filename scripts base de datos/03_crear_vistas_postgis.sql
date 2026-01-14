-- =========================================================================
-- SCRIPT PARA CREAR VISTAS DE CAPAS DEL MAE
-- =========================================================================
-- Base de datos: saf_postgis
-- 
-- Este script crea vistas simplificadas que apuntan a las tablas oficiales
-- del MAE para facilitar su uso en el servicio de verificación SAF.
--
-- Las vistas estandarizan los nombres de columnas y proveen una capa de
-- abstracción para facilitar actualizaciones futuras de las capas del MAE.
--
-- Última actualización: 2026-01-11
-- =========================================================================

-- Conectar a la base de datos correcta
\c saf_postgis

\echo '========================================='
\echo '  Creando vistas de capas del MAE'
\echo '========================================='
\echo ''

-- =========================================================================
-- 1. AREAS PROTEGIDAS - SNAP
-- =========================================================================
-- Sistema Nacional de Áreas Protegidas
-- Fuente: Tabla oficial del MAE actualizada 08/08/2019
-- Registros: 59 áreas protegidas

DROP VIEW IF EXISTS areas_protegidas_snap CASCADE;

CREATE OR REPLACE VIEW areas_protegidas_snap AS
SELECT 
    gid,
    geom,
    nam AS nombre,
    are AS area_ha
FROM fa210_snap_a_08082019;

COMMENT ON VIEW areas_protegidas_snap IS 
'Vista del Sistema Nacional de Áreas Protegidas del Ecuador (SNAP). 
Fuente: fa210_snap_a_08082019 actualizada 08/08/2019. 
Total: 59 áreas protegidas.';

\echo '✓ Vista areas_protegidas_snap creada'

-- =========================================================================
-- 2. BOSQUES PROTECTORES
-- =========================================================================
-- Bosques y vegetación protectora
-- Fuente: Tabla oficial del MAE
-- Registros: 169 polígonos

DROP VIEW IF EXISTS bosques_protectores CASCADE;

CREATE OR REPLACE VIEW bosques_protectores AS
SELECT 
    gid,
    geom,
    nam AS nombre,
    are AS area_ha,
    tpbsq AS tipo
FROM hc000_bosque_vegetacion_protectora;

COMMENT ON VIEW bosques_protectores IS 
'Vista de Bosques y Vegetación Protectora del Ecuador. 
Fuente: hc000_bosque_vegetacion_protectora. 
Total: 169 polígonos.';

\echo '✓ Vista bosques_protectores creada'

-- =========================================================================
-- 3. VEGETACIÓN PROTECTORA
-- =========================================================================
-- Misma fuente que bosques protectores
-- (En el futuro podría filtrar por tipo)

DROP VIEW IF EXISTS vegetacion_protectora CASCADE;

CREATE OR REPLACE VIEW vegetacion_protectora AS
SELECT 
    gid,
    geom,
    nam AS nombre,
    are AS area_ha,
    tpbsq AS tipo
FROM hc000_bosque_vegetacion_protectora;

COMMENT ON VIEW vegetacion_protectora IS 
'Vista de Vegetación Protectora del Ecuador. 
Fuente: hc000_bosque_vegetacion_protectora. 
Total: 169 polígonos.
Nota: Comparte la misma fuente con bosques_protectores.';

\echo '✓ Vista vegetacion_protectora creada'

-- =========================================================================
-- 4. PATRIMONIO FORESTAL DEL ESTADO
-- =========================================================================
-- Patrimonio Forestal del Estado (PFE)
-- Fuente: Tabla oficial del MAE actualizada 11/07/2018
-- Registros: 28 áreas

DROP VIEW IF EXISTS patrimonio_forestal_estado CASCADE;

CREATE OR REPLACE VIEW patrimonio_forestal_estado AS
SELECT 
    gid,
    geom,
    nombre,
    area_ha
FROM hc001_pfe_a_11072018;

COMMENT ON VIEW patrimonio_forestal_estado IS 
'Vista del Patrimonio Forestal del Estado (PFE). 
Fuente: hc001_pfe_a_11072018 actualizada 11/07/2018. 
Total: 28 áreas del PFE.';

\echo '✓ Vista patrimonio_forestal_estado creada'

-- =========================================================================
-- 5. RESERVAS MARINAS / BIOSFERA
-- =========================================================================
-- Reservas de Biosfera
-- Fuente: Tabla oficial del MAE
-- Registros: 7 reservas

DROP VIEW IF EXISTS reservas_marinas CASCADE;

CREATE OR REPLACE VIEW reservas_marinas AS
SELECT 
    gid,
    geom,
    nam AS nombre,
    are AS area_ha
FROM hc002_reserva_biosfera_a;

COMMENT ON VIEW reservas_marinas IS 
'Vista de Reservas de Biosfera del Ecuador. 
Fuente: hc002_reserva_biosfera_a. 
Total: 7 reservas de biosfera.';

\echo '✓ Vista reservas_marinas creada'

-- =========================================================================
-- VERIFICACIÓN
-- =========================================================================
\echo ''
\echo '========================================='
\echo '  Verificación de Vistas Creadas'
\echo '========================================='
\echo ''

SELECT 
    viewname AS "Vista",
    CASE 
        WHEN viewname = 'areas_protegidas_snap' THEN 'fa210_snap_a_08082019'
        WHEN viewname = 'bosques_protectores' THEN 'hc000_bosque_vegetacion_protectora'
        WHEN viewname = 'vegetacion_protectora' THEN 'hc000_bosque_vegetacion_protectora'
        WHEN viewname = 'patrimonio_forestal_estado' THEN 'hc001_pfe_a_11072018'
        WHEN viewname = 'reservas_marinas' THEN 'hc002_reserva_biosfera_a'
    END AS "Tabla Origen",
    viewowner AS "Propietario"
FROM pg_views 
WHERE schemaname = 'public' 
AND viewname IN (
    'areas_protegidas_snap',
    'bosques_protectores',
    'patrimonio_forestal_estado',
    'reservas_marinas',
    'vegetacion_protectora'
)
ORDER BY viewname;

\echo ''
\echo '========================================='
\echo '  Conteo de Registros por Vista'
\echo '========================================='
\echo ''

SELECT 'areas_protegidas_snap' AS vista, COUNT(*) AS registros FROM areas_protegidas_snap
UNION ALL
SELECT 'bosques_protectores', COUNT(*) FROM bosques_protectores
UNION ALL
SELECT 'patrimonio_forestal_estado', COUNT(*) FROM patrimonio_forestal_estado
UNION ALL
SELECT 'reservas_marinas', COUNT(*) FROM reservas_marinas
UNION ALL
SELECT 'vegetacion_protectora', COUNT(*) FROM vegetacion_protectora
ORDER BY vista;

\echo ''
\echo '========================================='
\echo '  ✓ VISTAS CREADAS EXITOSAMENTE'
\echo '========================================='
\echo ''
\echo 'Las vistas están listas para ser usadas por el servicio SAF.'
\echo ''
\echo 'Para actualizar a una nueva versión de una capa del MAE:'
\echo ''
\echo '  -- Ejemplo: Nueva versión del SNAP'
\echo '  CREATE OR REPLACE VIEW areas_protegidas_snap AS'
\echo '  SELECT gid, geom, nam AS nombre, are AS area_ha'
\echo '  FROM fa210_snap_a_NUEVA_VERSION;'
\echo ''
\echo 'Luego actualizar la versión en saf_interconexion:'
\echo ''
\echo '  UPDATE saf_validation_layers'
\echo "  SET version = '2026-01-11'"
\echo "  WHERE layer_key = 'areas_protegidas_snap';"
\echo ''
