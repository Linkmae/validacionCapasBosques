# Diccionario de Datos - Sistema SAF de Verificación de Áreas Forestales

## Fecha de Generación
13 de enero de 2026

## Resumen Ejecutivo

Este documento contiene el diccionario de datos completo para las tablas del sistema SAF (Sistema de Verificación de Áreas Forestales). Todas las tablas listadas han sido validadas como activamente usadas en el código fuente del servicio de verificación.

## Tablas del Sistema

### 1. `saf_validation_layers`
**Propósito**: Configuración de reglas de validación para capas geográficas.  
**Uso en código**: `LayerValidationConfig.java` - carga reglas activas desde BD.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | SERIAL | PRIMARY KEY | Identificador único de la capa |
| `layer_key` | VARCHAR(100) | UNIQUE NOT NULL | Clave única para identificar la capa |
| `table_name` | VARCHAR(100) | NOT NULL | Nombre de la tabla PostGIS que contiene la geometría |
| `schema_name` | VARCHAR(50) | DEFAULT 'public' | Schema de la tabla PostGIS |
| `layer_display_name` | VARCHAR(255) | NOT NULL | Nombre descriptivo para mostrar al usuario |
| `validation_type` | VARCHAR(50) | NOT NULL | Tipo de validación (AREAS_CONSERVACION, BOSQUE_NO_BOSQUE) |
| `max_intersection_percentage` | NUMERIC(5,2) | DEFAULT 0.0 | Porcentaje máximo de intersección (obsoleto con umbrales) |
| `min_intersection_area_m2` | NUMERIC(10,2) | DEFAULT 10.0 | Área mínima de intersección para considerar |
| `validation_message` | TEXT | NULL | Mensaje de validación genérico |
| `active` | BOOLEAN | DEFAULT true | Indica si la capa está activa para validaciones |
| `version` | VARCHAR(50) | NULL | Versión de la capa geográfica |
| `zone_type` | VARCHAR(50) | NULL | Tipo de zona para capas subdivididas |
| `message_approved` | TEXT | NULL | Mensaje cuando la validación es aprobada (EUDR) |
| `message_rejected` | TEXT | NULL | Mensaje cuando la validación es rechazada (EUDR) |
| `notes` | TEXT | NULL | Notas administrativas |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Fecha de última actualización |

**Índices**: `idx_validation_layers_type`, `idx_validation_layers_active`

### 2. `saf_validation_thresholds`
**Propósito**: Umbrales escalonados de validación según tamaño de predio.  
**Uso en código**: `LayerValidationConfig.java` - carga umbrales asociados a cada capa.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | SERIAL | PRIMARY KEY | Identificador único del umbral |
| `layer_id` | INTEGER | NOT NULL, FK a saf_validation_layers(id) | Referencia a la capa de validación |
| `min_hectares` | NUMERIC(10,2) | NOT NULL, >= 0 | Tamaño mínimo del predio en hectáreas |
| `max_hectares` | NUMERIC(10,2) | NULL | Tamaño máximo del predio en hectáreas (NULL = sin límite) |
| `max_percentage` | NUMERIC(5,2) | NOT NULL, 0-100 | Porcentaje máximo de intersección permitido |
| `description` | TEXT | NULL | Descripción del umbral |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Fecha de creación |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Fecha de actualización |

**Índices**: `idx_thresholds_layer`, `idx_thresholds_range`

### 3. `saf_request_logs`
**Propósito**: Auditoría de solicitudes al servicio de verificación.  
**Uso en código**: `DatabaseManager.java` - registra cada solicitud procesada.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Identificador único del log |
| `request_id` | VARCHAR(50) | UNIQUE | ID único de la solicitud |
| `identifier_type` | VARCHAR(50) | NULL | Tipo de identificador (CEDULA, PREDIO_ID) |
| `identifier_value` | VARCHAR(100) | NULL | Valor del identificador |
| `verification_type` | VARCHAR(50) | NULL | Tipo de verificación realizada |
| `status_code` | VARCHAR(10) | NULL | Código de estado HTTP |
| `error_type` | VARCHAR(50) | NULL | Tipo de error si ocurrió |
| `status_message` | TEXT | NULL | Mensaje de estado o respuesta |
| `total_predios` | INTEGER | DEFAULT 0 | Número total de predios en la solicitud |
| `predios_procesados` | INTEGER | DEFAULT 0 | Número de predios procesados |
| `predios_exitosos` | INTEGER | DEFAULT 0 | Número de predios que pasaron validación |
| `total_layers_checked` | INTEGER | DEFAULT 0 | Total de capas verificadas |
| `layers_not_loaded` | INTEGER | DEFAULT 0 | Capas que no pudieron cargarse |
| `layers_with_intersection` | INTEGER | DEFAULT 0 | Capas que tuvieron intersección |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Fecha/hora de la solicitud |
| `response_timestamp` | TIMESTAMP | NULL | Fecha/hora de la respuesta |

**Índices**: `idx_identifier`, `idx_timestamp`, `idx_status`

### 4. `saf_error_logs`
**Propósito**: Registro de errores y excepciones del sistema.  
**Uso en código**: `DatabaseManager.java` - registra errores con stack traces.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | SERIAL | PRIMARY KEY | Identificador único del error |
| `request_id` | VARCHAR(100) | NULL, FK a saf_request_logs(request_id) | ID de la solicitud que generó el error |
| `error_type` | VARCHAR(100) | NOT NULL | Tipo de error (VALIDATION_ERROR, DATABASE_ERROR) |
| `error_message` | TEXT | NULL | Mensaje descriptivo del error |
| `stack_trace` | TEXT | NULL | Stack trace completo del error |
| `timestamp` | TIMESTAMP | NOT NULL, DEFAULT NOW() | Fecha/hora del error |

**Índices**: `idx_request_id`, `idx_error_type`, `idx_timestamp`

### 5. `saf_predio_logs`
**Propósito**: Detalle de validación para cada predio individual.  
**Uso en código**: `DatabaseManager.java` - registra resultados por predio y capa.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | BIGSERIAL | PRIMARY KEY | Identificador único del log de predio |
| `request_id` | VARCHAR(50) | NOT NULL, FK a saf_request_logs(request_id) | ID de la solicitud |
| `predio_id` | VARCHAR(100) | NULL | ID único del predio |
| `predio_codigo` | VARCHAR(50) | NULL | Código del predio |
| `owner_cedula` | VARCHAR(50) | NULL | Cédula del propietario |
| `owner_name` | VARCHAR(255) | NULL | Nombre del propietario |
| `predio_area_m2` | DOUBLE PRECISION | NULL | Área del predio en metros cuadrados |
| `layer_name` | VARCHAR(100) | NULL | Nombre de la capa validada |
| `layer_table_name` | VARCHAR(100) | NULL | Nombre de la tabla PostGIS |
| `layer_not_loaded` | BOOLEAN | DEFAULT FALSE | Indica si la capa no pudo cargarse |
| `intersects` | BOOLEAN | DEFAULT FALSE | Indica si hay intersección con la capa |
| `intersection_area_m2` | DOUBLE PRECISION | DEFAULT 0 | Área de intersección en m² |
| `intersection_percentage` | DOUBLE PRECISION | DEFAULT 0 | Porcentaje de intersección |
| `validation_passed` | BOOLEAN | NULL | Resultado de la validación (true/false) |
| `validation_message` | TEXT | NULL | Mensaje específico de validación |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Fecha/hora del procesamiento |

**Índices**: `idx_predio_logs_request`, `idx_predio_logs_owner`

### 6. `config_parameters`
**Propósito**: Parámetros de configuración del sistema.  
**Uso en código**: `DatabaseManager.java` - obtiene valores de configuración.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | SERIAL | PRIMARY KEY | Identificador único del parámetro |
| `parameter_key` | VARCHAR(100) | UNIQUE NOT NULL | Clave del parámetro |
| `parameter_value` | TEXT | NOT NULL | Valor del parámetro |
| `description` | TEXT | NULL | Descripción del parámetro |
| `is_active` | BOOLEAN | DEFAULT true | Indica si el parámetro está activo |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Fecha de creación |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Fecha de actualización |

### 7. `saf_config`
**Propósito**: Configuración adicional del servicio PostGIS.  
**Uso en código**: `ConfigManager.java` - carga configuraciones del sistema.

| Campo | Tipo | Restricciones | Descripción |
|-------|------|---------------|-------------|
| `id` | SERIAL | PRIMARY KEY | Identificador único |
| `config_key` | VARCHAR(100) | UNIQUE NOT NULL | Clave de configuración |
| `config_value` | TEXT | NULL | Valor de configuración |
| `description` | TEXT | NULL | Descripción |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Fecha de creación |

## Modelo Relacional

```
saf_validation_layers (1) ──── (N) saf_validation_thresholds
    │
    └─── (Referenciado por) saf_predio_logs (layer_name/table_name)

saf_request_logs (1) ──── (N) saf_predio_logs (request_id)
    │
    └─── (Referenciado por) saf_error_logs (request_id)

config_parameters (Independiente - configuración global)
saf_config (Independiente - configuración adicional)
```

## Validación de Uso

Todas las tablas listadas han sido validadas como activamente usadas en el código fuente del servicio de verificación SAF. No hay redundancias - cada tabla cumple un rol específico en el sistema de validación de áreas forestales según el Reglamento EUDR.

## Notas Técnicas

- **Base de datos**: PostgreSQL con extensión PostGIS
- **Encoding**: UTF-8
- **Schema principal**: `public`
- **Schema capas**: `h_demarcacion` (para algunas capas geográficas)
- **Framework**: Java EE con JDBC para acceso a datos
- **Caché**: Las reglas de validación se cachean por 5 minutos en memoria

## Contacto

Para preguntas sobre este diccionario de datos, contactar al equipo de desarrollo del Sistema SAF.</content>
<parameter name="filePath">/home/linkmaedev/Proyecto_Interconeccion/SAF_Services/DICCIONARIO_DATOS_SAF.md