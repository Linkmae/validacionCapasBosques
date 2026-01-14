-- Script para crear las tablas principales del sistema SAF Interconexión
-- Base de datos: saf_interconexion
-- Ejecutar después de crear las bases de datos
-- Fecha: 13 de enero de 2026

-- Conectar a la base de datos principal
\c saf_interconexion

-- ===========================================
-- 1. TABLA: saf_validation_layers
-- ===========================================
CREATE TABLE IF NOT EXISTS saf_validation_layers (
    id SERIAL PRIMARY KEY,
    layer_key VARCHAR(100) UNIQUE NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    schema_name VARCHAR(50) DEFAULT 'public',
    layer_display_name VARCHAR(255) NOT NULL,
    validation_type VARCHAR(50) NOT NULL,
    max_intersection_percentage NUMERIC(5,2) DEFAULT 0.0,
    min_intersection_area_m2 NUMERIC(10,2) DEFAULT 10.0,
    validation_message TEXT,
    active BOOLEAN DEFAULT true,
    version VARCHAR(50),
    zone_type VARCHAR(50),
    message_approved TEXT,
    message_rejected TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para saf_validation_layers
CREATE INDEX IF NOT EXISTS idx_validation_layers_type ON saf_validation_layers(validation_type);
CREATE INDEX IF NOT EXISTS idx_validation_layers_active ON saf_validation_layers(active);

-- Comentarios
COMMENT ON TABLE saf_validation_layers IS 'Configuración de reglas de validación para capas geográficas';
COMMENT ON COLUMN saf_validation_layers.layer_key IS 'Clave única para identificar la capa';
COMMENT ON COLUMN saf_validation_layers.validation_type IS 'Tipo de validación (AREAS_CONSERVACION, BOSQUE_NO_BOSQUE)';

-- ===========================================
-- 2. TABLA: saf_validation_thresholds
-- ===========================================
CREATE TABLE IF NOT EXISTS saf_validation_thresholds (
    id SERIAL PRIMARY KEY,
    layer_id INTEGER NOT NULL REFERENCES saf_validation_layers(id) ON DELETE CASCADE,
    min_hectares NUMERIC(10,2) NOT NULL CHECK (min_hectares >= 0),
    max_hectares NUMERIC(10,2) NULL CHECK (max_hectares IS NULL OR max_hectares > min_hectares),
    max_percentage NUMERIC(5,2) NOT NULL CHECK (max_percentage >= 0 AND max_percentage <= 100),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para saf_validation_thresholds
CREATE INDEX IF NOT EXISTS idx_thresholds_layer ON saf_validation_thresholds(layer_id);
CREATE INDEX IF NOT EXISTS idx_thresholds_range ON saf_validation_thresholds(min_hectares, max_hectares);

-- Comentarios
COMMENT ON TABLE saf_validation_thresholds IS 'Umbrales escalonados de validación según tamaño de predio';
COMMENT ON COLUMN saf_validation_thresholds.min_hectares IS 'Tamaño mínimo del predio en hectáreas';
COMMENT ON COLUMN saf_validation_thresholds.max_percentage IS 'Porcentaje máximo de intersección permitido';

-- ===========================================
-- 3. TABLA: saf_request_logs
-- ===========================================
CREATE TABLE IF NOT EXISTS saf_request_logs (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(50) UNIQUE,
    identifier_type VARCHAR(50),
    identifier_value VARCHAR(100),
    verification_type VARCHAR(50),
    status_code VARCHAR(10),
    error_type VARCHAR(50),
    status_message TEXT,
    total_predios INTEGER DEFAULT 0,
    predios_procesados INTEGER DEFAULT 0,
    predios_exitosos INTEGER DEFAULT 0,
    total_layers_checked INTEGER DEFAULT 0,
    layers_not_loaded INTEGER DEFAULT 0,
    layers_with_intersection INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    response_timestamp TIMESTAMP
);

-- Índices para saf_request_logs
CREATE INDEX IF NOT EXISTS idx_request_logs_identifier ON saf_request_logs(identifier_value);
CREATE INDEX IF NOT EXISTS idx_request_logs_timestamp ON saf_request_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_request_logs_status ON saf_request_logs(status_code);

-- Comentarios
COMMENT ON TABLE saf_request_logs IS 'Auditoría de solicitudes al servicio de verificación';

-- ===========================================
-- 4. TABLA: saf_error_logs
-- ===========================================
CREATE TABLE IF NOT EXISTS saf_error_logs (
    id SERIAL PRIMARY KEY,
    request_id VARCHAR(100),
    error_type VARCHAR(100) NOT NULL,
    error_message TEXT,
    stack_trace TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Índices para saf_error_logs
CREATE INDEX IF NOT EXISTS idx_error_logs_request_id ON saf_error_logs(request_id);
CREATE INDEX IF NOT EXISTS idx_error_logs_error_type ON saf_error_logs(error_type);
CREATE INDEX IF NOT EXISTS idx_error_logs_timestamp ON saf_error_logs(timestamp);

-- Comentarios
COMMENT ON TABLE saf_error_logs IS 'Registro de errores y excepciones del sistema';

-- ===========================================
-- 5. TABLA: saf_predio_logs
-- ===========================================
CREATE TABLE IF NOT EXISTS saf_predio_logs (
    id BIGSERIAL PRIMARY KEY,
    request_id VARCHAR(50) NOT NULL,
    predio_id VARCHAR(100) NOT NULL,
    predio_area_ha NUMERIC(10,2),
    layer_key VARCHAR(100) NOT NULL,
    intersection_area_m2 NUMERIC(15,2) DEFAULT 0,
    intersection_percentage NUMERIC(5,2) DEFAULT 0,
    validation_result VARCHAR(20) NOT NULL, -- APPROVED, REJECTED, WARNING
    validation_message TEXT,
    threshold_applied_id INTEGER REFERENCES saf_validation_thresholds(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para saf_predio_logs
CREATE INDEX IF NOT EXISTS idx_predio_logs_request_id ON saf_predio_logs(request_id);
CREATE INDEX IF NOT EXISTS idx_predio_logs_predio_id ON saf_predio_logs(predio_id);
CREATE INDEX IF NOT EXISTS idx_predio_logs_layer ON saf_predio_logs(layer_key);
CREATE INDEX IF NOT EXISTS idx_predio_logs_result ON saf_predio_logs(validation_result);

-- Comentarios
COMMENT ON TABLE saf_predio_logs IS 'Detalle de validación para cada predio individual';

-- ===========================================
-- 6. TABLA: config_parameters (opcional)
-- ===========================================
CREATE TABLE IF NOT EXISTS config_parameters (
    id SERIAL PRIMARY KEY,
    parameter_key VARCHAR(100) UNIQUE NOT NULL,
    parameter_value TEXT NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Insertar configuración por defecto
INSERT INTO config_parameters (parameter_key, parameter_value, description, is_active)
VALUES
('predios_service_url', 'http://localhost:8080/saf-predios-service/PrediosService?wsdl', 'URL del servicio SOAP de predios', true),
('predios_service_namespace', 'http://saf.com/predios', 'Namespace del servicio de predios', true),
('verification_timeout_seconds', '30', 'Timeout para llamadas de verificación en segundos', true),
('max_retry_attempts', '3', 'Número máximo de reintentos para llamadas externas', true)
ON CONFLICT (parameter_key) DO NOTHING;

-- Comentarios
COMMENT ON TABLE config_parameters IS 'Parámetros de configuración del sistema';

-- ===========================================
-- Verificación de creación de tablas
-- ===========================================
SELECT
    schemaname,
    tablename,
    tableowner
FROM pg_tables
WHERE schemaname = 'public'
    AND tablename LIKE 'saf_%'
ORDER BY tablename;