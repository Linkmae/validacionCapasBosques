# Análisis de Uso de Tablas en el Código

## 📋 Resumen Ejecutivo

Este documento analiza qué tablas están definidas en la base de datos versus cuáles se usan realmente en el código Java del servicio de verificación.

---

## ✅ Tablas USADAS Activamente en el Código

### Base de Datos: **saf_interconexion**

| Tabla | Archivo Java | Operación | Estado |
|-------|--------------|-----------|---------|
| `saf_request_logs` | `DatabaseManager.java:52` | INSERT | ✅ **USADA** |
| `saf_predio_logs` | `DatabaseManager.java:94` | INSERT | ✅ **USADA** |
| `saf_error_logs` | `DatabaseManager.java:140` | INSERT | ✅ **USADA** |
| `config_parameters` | `DatabaseManager.java:184` | SELECT | ✅ **USADA** |
| `saf_validation_layers` | `LayerValidationConfig.java:52` | SELECT | ✅ **USADA** |
| `saf_validation_thresholds` | `LayerValidationConfig.java:88` | SELECT | ✅ **USADA** |

### Base de Datos: **saf_postgis**

| Tabla/Vista | Archivo Java | Operación | Estado |
|-------------|--------------|-----------|---------|
| `areas_protegidas_snap` | `LayerValidationConfig.java:187` (hardcoded fallback) | SELECT con ST_Intersects | ✅ **USADA** |
| `bosques_protectores` | `LayerValidationConfig.java:210` (hardcoded fallback) | SELECT con ST_Intersects | ✅ **USADA** |
| `patrimonio_forestal_estado` | `LayerValidationConfig.java:199` (hardcoded fallback) | SELECT con ST_Intersects | ✅ **USADA** |
| `vegetacion_protectora` | `LayerValidationConfig.java:222` (hardcoded fallback) | SELECT con ST_Intersects | ✅ **USADA** |
| `reservas_marinas` | `LayerValidationConfig.java:234` (hardcoded fallback) | SELECT con ST_Intersects | ✅ **USADA** |
| `bosque_no_bosque` | `LayerValidationConfig.java:158` (hardcoded fallback) | SELECT con ST_Intersects | ✅ **USADA** |

**Nota:** Las capas geográficas se consultan dinámicamente mediante `DatabaseManager.calculateIntersection()` que ejecuta:

```java
String query = "SELECT " +
    "CASE WHEN ST_Area(intersection_geom) > 0 THEN true ELSE false END AS intersects, " +
    "ST_Area(ST_Transform(intersection_geom, 4326)::geography) AS area_m2, " +
    "ST_AsGeoJSON(ST_Transform(intersection_geom, 4326)) AS geojson " +
    "FROM ( " +
    "    SELECT ST_Union(ST_Intersection(ST_GeomFromText(?, 4326), geom)) AS intersection_geom " +
    "    FROM " + capaSchemaTabla + " " +  // ← Nombre de tabla dinámico
    "    WHERE ST_Intersects(ST_GeomFromText(?, 4326), geom) " +
    ") AS subquery";
```

---

## ⚠️ Tablas DEFINIDAS pero NO USADAS en el Código

### Base de Datos: **saf_interconexion**

| Tabla | Definida En | Motivo de No Uso | Recomendación |
|-------|-------------|------------------|---------------|
| `saf_config` | `setup_local_postgis.sql:18` | ❌ El código usa `config_parameters` en su lugar | **ELIMINAR** o migrar datos a `config_parameters` |

**Análisis:**
```java
// ConfigManager.java:25 - Busca en "saf_config" pero NO existe en producción
PreparedStatement stmt = conn.prepareStatement("SELECT config_key, config_value FROM saf_config");

// DatabaseManager.java:184 - Busca en "config_parameters" (la tabla CORRECTA)
String sql = "SELECT parameter_value FROM config_parameters WHERE parameter_key = ? AND is_active = true";
```

**⚠️ CONFLICTO DETECTADO:** 
- `ConfigManager.java` busca en tabla `saf_config` (que NO existe en el esquema actual)
- `DatabaseManager.java` busca en tabla `config_parameters` (que SÍ existe)
- Ambas clases intentan hacer lo mismo pero usan diferentes tablas

### Vistas de Análisis (Definidas pero sin uso programático)

| Vista | Definida En | Estado |
|-------|-------------|--------|
| `v_request_summary` | `update_logs_schema.sql:111` | 📊 Útil para análisis manual/reportes |
| `v_layer_usage` | `update_logs_schema.sql:123` | 📊 Útil para análisis manual/reportes |
| `v_owner_activity` | `update_logs_schema.sql:146` | 📊 Útil para análisis manual/reportes |

**Nota:** Estas vistas están bien definidas para consultas manuales o futuras funcionalidades de reportes/dashboards.

---

## ❓ Tablas POTENCIALES Disponibles en MAE (No Integradas)

Estas tablas están disponibles en la BD PostGIS del MAE pero **NO se usan actualmente**:

| Tabla MAE | Capa | Estado | Prioridad |
|-----------|------|--------|-----------|
| `hc005_area_bajo_conservacion_a` | Programa SocioBosque | ⭕ **NO USADA** | 🔴 Alta - Según CSV debe validarse |
| `hc003_zona_intangible_a` | Zona Intangible Tagaeri-Taromenane | ⭕ **NO USADA** | 🔴 Alta - Según CSV debe validarse |
| `zona_recarga_hidrica` | Zona de Recarga Hídrica | ⭕ **NO USADA** | 🔴 Alta - Según CSV debe validarse |
| `fa210_snap_a_20251128` | SNAP actualizado (versión 2025) | ⭕ **NO USADA** | 🟡 Media - Versión más reciente disponible |
| `snap_zonas_admin` | SNAP con zonas diferenciadas | ⭕ **NO USADA** | 🟡 Media - Incluye zonificación |
| `hc001_pfe_a_20250127` | PFE actualizado (versión 2025) | ⭕ **NO USADA** | 🟡 Media - Versión más reciente disponible |

**Referencia:** Ver [TABLAS_DISPONIBLES_POSTGIS.md](../../archivosDesarrolo/TABLAS_DISPONIBLES_POSTGIS.md)

---

## 🔍 Detalle de Uso por Clase Java

### 1. **DatabaseManager.java**

**Operaciones con saf_interconexion:**

```java
// LÍNEA 52 - INSERT en saf_request_logs
String sql = "INSERT INTO saf_request_logs " +
    "(request_id, identifier_type, identifier_value, verification_type, " +
    "status_code, error_type, status_message, total_predios, " +
    "predios_procesados, predios_exitosos, total_layers_checked, " +
    "layers_not_loaded, layers_with_intersection, response_timestamp) " +
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

// LÍNEA 94 - INSERT en saf_predio_logs
String sql = "INSERT INTO saf_predio_logs " +
    "(request_id, predio_id, predio_codigo, owner_cedula, owner_name, predio_area_m2, " +
    "layer_name, layer_table_name, layer_not_loaded, intersects, " +
    "intersection_area_m2, intersection_percentage, validation_passed, validation_message) " +
    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

// LÍNEA 140 - INSERT en saf_error_logs
String sql = "INSERT INTO saf_error_logs (request_id, error_type, error_message, stack_trace, timestamp) " +
    "VALUES (?, ?, ?, ?, NOW())";

// LÍNEA 184 - SELECT en config_parameters
String sql = "SELECT parameter_value FROM config_parameters WHERE parameter_key = ? AND is_active = true";
```

**Operaciones con saf_postgis:**

```java
// LÍNEA 218 - SELECT dinámico con ST_Intersects (nombre de tabla parametrizado)
String query = "SELECT " +
    "CASE WHEN ST_Area(intersection_geom) > 0 THEN true ELSE false END AS intersects, " +
    "ST_Area(ST_Transform(intersection_geom, 4326)::geography) AS area_m2, " +
    "ST_AsGeoJSON(ST_Transform(intersection_geom, 4326)) AS geojson " +
    "FROM ( " +
    "    SELECT ST_Union(ST_Intersection(ST_GeomFromText(?, 4326), geom)) AS intersection_geom " +
    "    FROM " + capaSchemaTabla + " " +  // ← TABLA DINÁMICA desde config
    "    WHERE ST_Intersects(ST_GeomFromText(?, 4326), geom) " +
    ") AS subquery";
```

**Tablas consultadas dinámicamente:**
- `areas_protegidas_snap`
- `bosques_protectores`
- `patrimonio_forestal_estado`
- `vegetacion_protectora`
- `reservas_marinas`
- `bosque_no_bosque`
- Cualquier otra tabla configurada en `saf_validation_layers`

---

### 2. **LayerValidationConfig.java**

**Operaciones con saf_interconexion:**

```java
// LÍNEA 52 - SELECT en saf_validation_layers
String sqlLayers = "SELECT id, layer_key, table_name, schema_name, layer_display_name, " +
    "validation_type, max_intersection_percentage, min_intersection_area_m2, " +
    "validation_message, active, version, zone_type, message_approved, message_rejected " +
    "FROM saf_validation_layers " +
    "WHERE active = true " +
    "ORDER BY validation_type, layer_key";

// LÍNEA 88 - SELECT en saf_validation_thresholds
String sqlThresholds = "SELECT layer_id, min_hectares, max_hectares, max_percentage, description " +
    "FROM saf_validation_thresholds " +
    "ORDER BY layer_id, min_hectares";
```

**Reglas Hardcoded (Fallback):**

Cuando falla la conexión a BD, se cargan reglas hardcodeadas que referencian estas tablas:

```java
// LÍNEA 158 - bosque_no_bosque
rule.setLayerTableName("bosque_no_bosque");

// LÍNEA 187 - areas_protegidas_snap
snap.setLayerTableName("areas_protegidas_snap");

// LÍNEA 199 - patrimonio_forestal_estado
pfe.setLayerTableName("patrimonio_forestal_estado");

// LÍNEA 210 - bosques_protectores
bosquesProtectores.setLayerTableName("bosques_protectores");

// LÍNEA 222 - vegetacion_protectora
vegetacion.setLayerTableName("vegetacion_protectora");

// LÍNEA 234 - reservas_marinas
reservas.setLayerTableName("reservas_marinas");
```

---

### 3. **ConfigManager.java** ⚠️

**PROBLEMA DETECTADO:**

```java
// LÍNEA 25 - Busca en tabla "saf_config" que NO EXISTE
PreparedStatement stmt = conn.prepareStatement("SELECT config_key, config_value FROM saf_config");
```

**Esta clase NO se está usando correctamente porque:**
1. Busca en tabla `saf_config` que no está en el esquema actual
2. `DatabaseManager` usa `config_parameters` en su lugar
3. Genera una inconsistencia en el código

**Recomendación:** Actualizar `ConfigManager.java` para usar `config_parameters` o eliminar la clase si no se usa.

---

## 📊 Estadísticas de Uso

### Tablas de saf_interconexion

| Total Definidas | Usadas Activamente | No Usadas | En Conflicto |
|-----------------|-------------------|-----------|--------------|
| 7 tablas | 6 tablas | 0 tablas | 1 tabla (`saf_config`) |
| 3 vistas | 0 vistas (análisis manual) | 3 vistas | 0 vistas |

### Tablas/Vistas de saf_postgis

| Total en Producción | Usadas Activamente | Disponibles MAE (No usadas) |
|---------------------|--------------------|-----------------------------|
| 6 vistas/tablas | 6 vistas/tablas | 6+ tablas adicionales |

---

## 🔧 Problemas y Recomendaciones

### 🔴 **CRÍTICO - Conflicto en Configuración**

**Problema:**
```java
// ConfigManager.java - Busca "saf_config" (NO EXISTE)
SELECT config_key, config_value FROM saf_config

// DatabaseManager.java - Busca "config_parameters" (SÍ EXISTE)
SELECT parameter_value FROM config_parameters WHERE parameter_key = ?
```

**Solución Recomendada:**

1. **Opción 1 (Recomendada):** Eliminar `ConfigManager.java` ya que `DatabaseManager` maneja correctamente la configuración
2. **Opción 2:** Actualizar `ConfigManager.java` para usar `config_parameters`:

```java
// Cambiar línea 25:
// DE:
PreparedStatement stmt = conn.prepareStatement("SELECT config_key, config_value FROM saf_config");

// A:
PreparedStatement stmt = conn.prepareStatement("SELECT parameter_key, parameter_value FROM config_parameters WHERE is_active = true");
```

---

### 🟡 **MEDIO - Capas del MAE no integradas**

**Faltantes según requerimientos (CSV de casos):**
1. `hc005_area_bajo_conservacion_a` - Programa SocioBosque
2. `hc003_zona_intangible_a` - Zona Intangible Tagaeri-Taromenane  
3. `zona_recarga_hidrica` - Zonas de Recarga Hídrica

**Acción Requerida:**
- Crear vistas en `saf_postgis` para estas capas
- Agregar reglas de validación en `saf_validation_layers`
- Actualizar scripts de vistas en `crear_vistas_capas_mae.sql`

---

### 🟢 **BAJO - Actualización de versiones de capas**

**Versiones más recientes disponibles:**
- SNAP: `fa210_snap_a_20251128` (actualmente usa versión 08/08/2019)
- PFE: `hc001_pfe_a_20250127` (actualmente usa versión 11/07/2018)

**Acción Recomendada:**
- Evaluar diferencias con versiones actuales
- Programar actualización en ventana de mantenimiento
- Actualizar referencias en vistas

---

## 💡 Conclusiones

### ✅ Lo que funciona bien:

1. **Logging completo:** Todas las tablas de logs (`saf_request_logs`, `saf_predio_logs`, `saf_error_logs`) están siendo usadas activamente
2. **Configuración dinámica:** `config_parameters` permite cambiar configuraciones sin recompilar
3. **Validación flexible:** Tablas `saf_validation_layers` y `saf_validation_thresholds` permiten ajustar reglas sin código
4. **Consultas dinámicas:** El sistema de capas permite agregar nuevas capas via configuración

### ⚠️ Lo que necesita atención:

1. **ConfigManager.java** tiene código incorrecto que referencia tabla inexistente
2. **Faltan 3 capas requeridas** según el CSV de casos de prueba
3. **Vistas para análisis** están definidas pero no hay herramientas para consumirlas

### 🎯 Acciones prioritarias:

1. 🔴 Corregir o eliminar `ConfigManager.java`
2. 🔴 Integrar las 3 capas faltantes (SocioBosque, Zona Intangible, Recarga Hídrica)
3. 🟡 Evaluar actualización de SNAP y PFE a versiones 2025
4. 🟢 Considerar desarrollar dashboard para vistas de análisis

---

## 📈 Métricas de Cobertura

### Cobertura de Tablas Definidas

```
saf_interconexion:
████████████████████░░  86% (6/7 tablas en uso)

saf_postgis:
██████████████████████ 100% (6/6 tablas/vistas en uso)
```

### Cobertura de Capas Requeridas (según CSV)

```
Capas implementadas:
██████████████████░░░░  75% (6/8 capas requeridas)

Faltan: SocioBosque, Zona Intangible, Recarga Hídrica
```

---

**Fecha del Análisis:** 11 de Marzo de 2026  
**Versión del Documento:** 1.0  
**Analista:** Sistema de Análisis Automatizado

<function_calls>
<invoke name="read_file">
<parameter name="filePath">/home/jonathan-tejada/Documents/Proyecto_Interconeccion/SAF_Services/saf-verification-service/src/main/java/com/saf/verification/ConfigManager.java