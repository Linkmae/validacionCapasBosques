-- Script para insertar datos iniciales ACTUALIZADOS en tablas de configuración SAF
-- Base de datos: saf_interconexion
-- Ejecutar después de crear las tablas
-- Fecha actualización: 2026-03-12

\c saf_interconexion

BEGIN;

-- ===========================================
-- DATOS INICIALES: saf_validation_layers
-- ===========================================
INSERT INTO saf_validation_layers (
    layer_key, table_name, schema_name, layer_display_name, validation_type,
    max_intersection_percentage, min_intersection_area_m2, validation_message,
    active, version, zone_type, message_approved, message_rejected, notes
) VALUES
('bosque_no_bosque', 'bosque_no_bosque', 'public', 'Bosque No Bosque', 'BOSQUE_NO_BOSQUE',
 0.0, 100.0, 'Intersección con bosque no bosque',
 true, 'prueba-v1', NULL,
 'Su producción se encuentra en zona libre de deforestación. Cumple requisito para EUDR',
 'Su producción se encuentra en zona boscosa. No cumple requisito para EUDR',
 'Actualizado según CSV 4dic2025 - Umbrales escalonados por tamaño'),

('areas_protegidas_snap', 'areas_protegidas_snap', 'public', 'Sistema Nacional de Áreas Protegidas', 'AREAS_CONSERVACION',
 0.0, 100.0, 'Intersección con áreas protegidas SNAP',
 true, '2019-08-08', NULL,
 'Su producción se encuentra fuera del SNAP. Cumple requisito para EUDR',
 'Su producción se encuentra en zona de protección SNAP. No cumple requisito para EUDR',
 'Actualizado según CSV 4dic2025 - Zonas de protección y recuperación'),

('patrimonio_forestal_estado', 'patrimonio_forestal_estado', 'public', 'Patrimonio Forestal del Estado', 'AREAS_CONSERVACION',
 0.0, 100.0, 'Intersección con Patrimonio Forestal del Estado',
 true, '2018-07-11', NULL,
 'Su producción se encuentra fuera de Patrimonio Forestal del Estado. Cumple requisito para EUDR',
 'Su producción se encuentra en Patrimonio Forestal del Estado. No cumple requisito para EUDR',
 'Actualizado según CSV 4dic2025'),

('bosques_protectores', 'bosques_protectores', 'public', 'Bosques Protectores', 'AREAS_CONSERVACION',
 0.0, 100.0, 'Intersección con Bosques Protectores',
 true, '2019-08-07', NULL,
 'Su producción se encuentra fuera de Bosques y Vegetación Protectora. Cumple requisito para EUDR',
 'Su producción se encuentra en zona de protección BVP. No cumple requisito para EUDR',
 'Actualizado según CSV 4dic2025'),

('vegetacion_protectora', 'vegetacion_protectora', 'public', 'Vegetación Protectora', 'AREAS_CONSERVACION',
 5.0, 100.0, 'Intersección con Vegetación Protectora',
 true, '2019-08-07', NULL,
 'Su producción se encuentra fuera de Bosques y Vegetación Protectora. Cumple requisito para EUDR',
 'Su producción se encuentra en zona de protección BVP. No cumple requisito para EUDR',
 'Actualizado según CSV 4dic2025'),

('reservas_marinas', 'reservas_marinas', 'public', 'Reservas Marinas', 'AREAS_CONSERVACION',
 0.0, 100.0, 'Intersección con Reservas Marinas/Biosfera',
 true, '2019', NULL,
 'Su producción se encuentra fuera de Reservas de Biosfera. Cumple requisito para EUDR',
 'Su producción se encuentra en Reserva de Biosfera. No cumple requisito para EUDR',
 'Actualizado según CSV 4dic2025'),

('programa_sociobosque', 'v_hc005_area_bajo_conservacion_a', 'h_demarcacion', 'Programa SocioBosque', 'AREAS_CONSERVACION',
 0.0, 100.0, 'Intersección con áreas del Programa SocioBosque',
 true, '2021-09-14', NULL,
 'Su producción se encuentra fuera de predios Proyecto SocioBosque. Cumple legislación nacional por lo tanto cumple requisito para EUDR',
 'Su producción cruza con predios Proyecto SocioBosque. No cumple legislación nacional por lo tanto no cumple requisito para EUDR',
 'Agregado según CSV 4dic2025'),

('zona_intangible', 'hc003_zona_intangible_a', 'h_demarcacion', 'Zona Intangible Tagaeri-Taromenane', 'AREAS_CONSERVACION',
 0.0, 100.0, 'Intersección con Zona Intangible',
 true, '2020-04-08', NULL,
 'Su producción se encuentra fuera de Zona Intangible. Cumple legislación nacional por lo tanto cumple requisito para EUDR',
 'Su producción cruza Zona Intangible. No cumple legislación nacional por lo tanto no cumple requisito para EUDR',
 'Agregado según CSV 4dic2025'),

('zona_recarga_hidrica', 'zona_recarga_hidrica', 'h_demarcacion', 'Zona de Recarga Hídrica', 'AREAS_CONSERVACION',
 0.0, 100.0, 'Intersección con Zona de Recarga Hídrica',
 true, '2024-01-25', NULL,
 'Su producción se encuentra fuera de Zona de Recarga Hídrica. Cumple legislación nacional por lo tanto cumple requisito para EUDR',
 'Su producción cruza con Zona de Recarga Hídrica. No cumple legislación nacional por lo tanto no cumple requisito para EUDR',
 'Agregado según CSV 4dic2025')
ON CONFLICT (layer_key) DO UPDATE SET
    table_name = EXCLUDED.table_name,
    schema_name = EXCLUDED.schema_name,
    layer_display_name = EXCLUDED.layer_display_name,
    validation_type = EXCLUDED.validation_type,
    max_intersection_percentage = EXCLUDED.max_intersection_percentage,
    min_intersection_area_m2 = EXCLUDED.min_intersection_area_m2,
    validation_message = EXCLUDED.validation_message,
    active = EXCLUDED.active,
    version = EXCLUDED.version,
    zone_type = EXCLUDED.zone_type,
    message_approved = EXCLUDED.message_approved,
    message_rejected = EXCLUDED.message_rejected,
    notes = EXCLUDED.notes,
    updated_at = CURRENT_TIMESTAMP;

-- ===========================================
-- DATOS INICIALES: saf_validation_thresholds
-- ===========================================
-- Se reemplazan umbrales de capas con configuración escalonada
DELETE FROM saf_validation_thresholds
WHERE layer_id IN (
    SELECT id FROM saf_validation_layers
    WHERE layer_key IN ('bosque_no_bosque', 'programa_sociobosque', 'zona_intangible', 'zona_recarga_hidrica')
);

-- Umbrales bosque_no_bosque (13 tramos)
INSERT INTO saf_validation_thresholds (layer_id, min_hectares, max_hectares, max_percentage, description)
SELECT l.id, t.min_h, t.max_h, t.max_p, t.desc_txt
FROM saf_validation_layers l
JOIN (
    VALUES
    (0.0, 1.0, 50.0, 'Predios ≤1 ha: máximo 50% de intersección con bosque'),
    (1.0, 2.0, 25.0, 'Predios 1-2 ha: máximo 25% de intersección con bosque'),
    (2.0, 3.0, 17.0, 'Predios 2-3 ha: máximo 17% de intersección con bosque'),
    (3.0, 4.0, 13.0, 'Predios 3-4 ha: máximo 13% de intersección con bosque'),
    (4.0, 5.0, 10.0, 'Predios 4-5 ha: máximo 10% de intersección con bosque'),
    (5.0, 6.0, 9.0,  'Predios 5-6 ha: máximo 9% de intersección con bosque'),
    (6.0, 7.0, 8.0,  'Predios 6-7 ha: máximo 8% de intersección con bosque'),
    (7.0, 9.0, 7.0,  'Predios 7-9 ha: máximo 7% de intersección con bosque'),
    (9.0, 10.0, 6.0, 'Predios 9-10 ha: máximo 6% de intersección con bosque'),
    (10.0, 15.0, 5.0,'Predios 10-15 ha: máximo 5% de intersección con bosque'),
    (15.0, 25.0, 3.0,'Predios 15-25 ha: máximo 3% de intersección con bosque'),
    (25.0, 50.0, 2.0,'Predios 25-50 ha: máximo 2% de intersección con bosque'),
    (50.0, NULL, 1.0,'Predios >50 ha: máximo 1% de intersección con bosque')
) AS t(min_h, max_h, max_p, desc_txt) ON true
WHERE l.layer_key = 'bosque_no_bosque';

-- Umbrales comunes (SocioBosque, Zona Intangible, Zona Recarga Hídrica)
INSERT INTO saf_validation_thresholds (layer_id, min_hectares, max_hectares, max_percentage, description)
SELECT l.id, t.min_h, t.max_h, t.max_p, t.desc_txt
FROM saf_validation_layers l
JOIN (
    VALUES
    (0.0, 5.0, 10.0, 'Predios ≤5 ha: máximo 10% de intersección'),
    (5.0, 10.0, 5.0, 'Predios 5-10 ha: máximo 5% de intersección'),
    (10.0, NULL, 3.0, 'Predios >10 ha: máximo 3% de intersección')
) AS t(min_h, max_h, max_p, desc_txt) ON true
WHERE l.layer_key IN ('programa_sociobosque', 'zona_intangible', 'zona_recarga_hidrica');

-- ===========================================
-- DATOS INICIALES: config_parameters
-- ===========================================
INSERT INTO config_parameters (parameter_key, parameter_value, description, is_active)
VALUES
('predios_service_url', 'http://localhost:8080/servicio-soap-predios/PrediosService?wsdl', 'URL del servicio SOAP de predios de Agrocalidad', true),
('predios_service_usuario', 'admin', 'Usuario del servicio externo de predios', true),
('predios_service_clave', '1234', 'Clave del servicio externo de predios', true),
('validation_types', 'BOSQUE_NO_BOSQUE,AREAS_CONSERVACION', 'Tipos de validación habilitados', true),
('bosque_no_bosque_min_area_m2', '100', 'Área mínima en m² para evaluar la capa bosque_no_bosque', true),
('areas_conservacion_min_area_m2', '100', 'Área mínima en m² para evaluar capas de áreas de conservación', true)
ON CONFLICT (parameter_key) DO UPDATE SET
    parameter_value = EXCLUDED.parameter_value,
    description = EXCLUDED.description,
    is_active = EXCLUDED.is_active,
    updated_at = CURRENT_TIMESTAMP;

COMMIT;

-- ===========================================
-- VERIFICACIÓN DE DATOS INSERTADOS
-- ===========================================
SELECT 'Capas activas' AS info, COUNT(*) AS total
FROM saf_validation_layers
WHERE active = true;

SELECT 'Umbrales configurados' AS info, COUNT(*) AS total
FROM saf_validation_thresholds;

SELECT 'Parámetros de configuración' AS info, COUNT(*) AS total
FROM config_parameters
WHERE is_active = true;

SELECT
    vl.layer_key,
    vl.schema_name,
    vl.table_name,
    vl.validation_type,
    COUNT(vt.id) AS num_umbrales
FROM saf_validation_layers vl
LEFT JOIN saf_validation_thresholds vt ON vl.id = vt.layer_id
WHERE vl.active = true
GROUP BY vl.layer_key, vl.schema_name, vl.table_name, vl.validation_type
ORDER BY vl.validation_type, vl.layer_key;