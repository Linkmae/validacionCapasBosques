-- Script para insertar datos iniciales en las tablas de configuración SAF
-- Base de datos: saf_interconexion
-- Ejecutar después de crear las tablas
-- Fecha: 13 de enero de 2026

\c saf_interconexion

-- ===========================================
-- DATOS INICIALES: saf_validation_layers
-- ===========================================
INSERT INTO saf_validation_layers (
    layer_key, table_name, schema_name, layer_display_name, validation_type,
    max_intersection_percentage, min_intersection_area_m2, validation_message,
    active, version, zone_type, message_approved, message_rejected, notes
) VALUES
-- Capas de Áreas de Conservación
('areas_conservacion_nacional', 'areas_conservacion', 'public', 'Áreas de Conservación Nacional', 'AREAS_CONSERVACION',
 0.0, 10.0, 'El predio intersecta con áreas de conservación nacional',
 true, '2026.01', 'NACIONAL',
 'Predio aprobado - No intersecta áreas de conservación nacional',
 'Predio rechazado - Intersecta con áreas de conservación nacional',
 'Capa oficial del MAE - Áreas de Conservación Nacional'),

('areas_conservacion_regional', 'areas_conservacion_regional', 'public', 'Áreas de Conservación Regional', 'AREAS_CONSERVACION',
 0.0, 10.0, 'El predio intersecta con áreas de conservación regional',
 true, '2026.01', 'REGIONAL',
 'Predio aprobado - No intersecta áreas de conservación regional',
 'Predio rechazado - Intersecta con áreas de conservación regional',
 'Capa regional del MAE'),

-- Capas de Bosque/No Bosque
('bosque_no_bosque', 'bosque_no_bosque', 'public', 'Cobertura Boscosa MAE', 'BOSQUE_NO_BOSQUE',
 0.0, 10.0, 'El predio intersecta con zonas boscosas',
 true, '2026.01', NULL,
 'Predio aprobado - Ubicado en zona no boscosa',
 'Predio rechazado - Intersecta con zona boscosa protegida',
 'Capa de cobertura boscosa del MAE - Bosque/No Bosque'),

-- Capas de Uso del Suelo
('uso_suelo_agricola', 'uso_suelo_agricola', 'public', 'Uso del Suelo Agrícola', 'USO_SUELO',
 0.0, 10.0, 'El predio intersecta con zonas de uso agrícola',
 true, '2026.01', NULL,
 'Predio aprobado - Compatible con uso agrícola',
 'Predio rechazado - No compatible con uso agrícola autorizado',
 'Capa de uso del suelo agrícola'),

('uso_suelo_forestal', 'uso_suelo_forestal', 'public', 'Uso del Suelo Forestal', 'USO_SUELO',
 0.0, 10.0, 'El predio intersecta con zonas de uso forestal',
 true, '2026.01', NULL,
 'Predio aprobado - Compatible con uso forestal',
 'Predio rechazado - No compatible con uso forestal autorizado',
 'Capa de uso del suelo forestal'),

-- Capas de Áreas Protegidas
('zonas_amortiguamiento', 'zonas_amortiguamiento', 'public', 'Zonas de Amortiguamiento', 'AREAS_PROTEGIDAS',
 0.0, 10.0, 'El predio intersecta con zonas de amortiguamiento',
 true, '2026.01', NULL,
 'Predio aprobado - Fuera de zonas de amortiguamiento',
 'Predio rechazado - Intersecta con zona de amortiguamiento',
 'Zonas de amortiguamiento de áreas protegidas'),

('corredores_biologicos', 'corredores_biologicos', 'public', 'Corredores Biológicos', 'AREAS_PROTEGIDAS',
 0.0, 10.0, 'El predio intersecta con corredores biológicos',
 true, '2026.01', NULL,
 'Predio aprobado - Fuera de corredores biológicos',
 'Predio rechazado - Intersecta con corredor biológico',
 'Corredores biológicos del MAE'),

-- Capas de Recursos Hídricos
('fuentes_agua', 'fuentes_agua', 'public', 'Fuentes de Agua', 'RECURSOS_HIDRICOS',
 0.0, 10.0, 'El predio intersecta con fuentes de agua',
 true, '2026.01', NULL,
 'Predio aprobado - Fuera de fuentes de agua',
 'Predio rechazado - Intersecta con fuente de agua protegida',
 'Fuentes de agua superficiales'),

('rios_principales', 'rios_principales', 'public', 'Ríos Principales', 'RECURSOS_HIDRICOS',
 0.0, 10.0, 'El predio intersecta con ríos principales',
 true, '2026.01', NULL,
 'Predio aprobado - Fuera de ríos principales',
 'Predio rechazado - Intersecta con río principal',
 'Ríos principales y afluentes mayores'),

-- Capas de Infraestructura
('infraestructura_critica', 'infraestructura_critica', 'public', 'Infraestructura Crítica', 'INFRAESTRUCTURA',
 0.0, 10.0, 'El predio intersecta con infraestructura crítica',
 true, '2026.01', NULL,
 'Predio aprobado - Fuera de infraestructura crítica',
 'Predio rechazado - Intersecta con infraestructura crítica',
 'Infraestructura crítica (oleoductos, gasoductos, líneas eléctricas)')
ON CONFLICT (layer_key) DO NOTHING;

-- ===========================================
-- DATOS INICIALES: saf_validation_thresholds
-- ===========================================
-- Obtener IDs de las capas insertadas
INSERT INTO saf_validation_thresholds (layer_id, min_hectares, max_hectares, max_percentage, description)
SELECT
    vl.id,
    thresholds.min_hectares,
    thresholds.max_hectares,
    thresholds.max_percentage,
    thresholds.description
FROM saf_validation_layers vl
CROSS JOIN (
    VALUES
    -- Umbrales para predios pequeños (0-5 ha)
    (0.0, 5.0, 0.0, 'Predios pequeños: Sin tolerancia de intersección'),
    -- Umbrales para predios medianos (5-50 ha)
    (5.0, 50.0, 1.0, 'Predios medianos: Máximo 1% de intersección'),
    -- Umbrales para predios grandes (50-500 ha)
    (50.0, 500.0, 5.0, 'Predios grandes: Máximo 5% de intersección'),
    -- Umbrales para predios muy grandes (500+ ha)
    (500.0, NULL, 10.0, 'Predios muy grandes: Máximo 10% de intersección')
) AS thresholds(min_hectares, max_hectares, max_percentage, description)
WHERE vl.active = true
ON CONFLICT DO NOTHING;

-- ===========================================
-- VERIFICACIÓN DE DATOS INSERTADOS
-- ===========================================
SELECT
    'Capas activas:' as info,
    COUNT(*) as total
FROM saf_validation_layers
WHERE active = true;

SELECT
    'Umbrales configurados:' as info,
    COUNT(*) as total
FROM saf_validation_thresholds;

-- Mostrar configuración completa
SELECT
    vl.layer_display_name,
    vl.validation_type,
    vt.min_hectares,
    vt.max_hectares,
    vt.max_percentage,
    vt.description as threshold_description
FROM saf_validation_layers vl
JOIN saf_validation_thresholds vt ON vl.id = vt.layer_id
WHERE vl.active = true
ORDER BY vl.layer_display_name, vt.min_hectares;