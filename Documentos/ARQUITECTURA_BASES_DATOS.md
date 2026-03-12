# Arquitectura de Bases de Datos - Proyecto SAF Interconexión

## 📊 Resumen Ejecutivo

El proyecto utiliza **2 bases de datos PostgreSQL** con PostGIS para gestionar capas geográficas y operaciones del sistema. Ambas bases de datos se conectan a través de **2 DataSources JNDI** configurados en JBoss EAP 7.4.

---

## 🗄️ Bases de Datos

### 1. **saf_postgis** (Base de Datos de Capas Geográficas)

**Propósito:** Almacenar y consultar capas geográficas del Ministerio del Ambiente del Ecuador (MAE).

**DataSource JNDI:** `java:jboss/datasources/SAFCapasDS`

**Conexión:**
- **URL:** `jdbc:postgresql://localhost:5432/saf_postgis`
- **Usuario:** `saf_app`
- **Contraseña:** `saf_app_2026`
- **Driver:** PostgreSQL JDBC

**Tablas/Vistas Principales:**

| Tabla/Vista | Tipo | Fuente Original | Registros | Descripción |
|-------------|------|-----------------|-----------|-------------|
| `areas_protegidas_snap` | Vista | `fa210_snap_a_08082019` | 59 | Sistema Nacional de Áreas Protegidas |
| `bosques_protectores` | Vista | `hc000_bosque_vegetacion_protectora` | 169 | Bosques y Vegetación Protectora |
| `vegetacion_protectora` | Vista | `hc000_bosque_vegetacion_protectora` | 169 | Vegetación Protectora (misma fuente) |
| `patrimonio_forestal_estado` | Vista | `hc001_pfe_a_11072018` | 28 | Patrimonio Forestal del Estado |
| `reservas_marinas` | Vista | `hc002_reserva_biosfera_a` | 7 | Reservas de Biosfera |
| `mapa_bosque_no_bosque` | Tabla | - | Variable | Mapa de Bosque y No Bosque |

**Tablas Originales MAE (esquema h_demarcacion):**
- `fa210_snap_a_08082019` - SNAP actualizado 08/08/2019
- `fa210_snap_a_20251128` - SNAP versión 28/11/2025 (más reciente)
- `snap_zonas_admin` - SNAP con zonas administrativas diferenciadas
- `hc000_bosque_vegetacion_protectora` - Bosques protectores
- `hc001_pfe_a_11072018` - PFE actualizado 11/07/2018
- `hc001_pfe_a_20250127` - PFE versión 27/01/2025 (más reciente)
- `hc002_reserva_biosfera_a` - Reservas de Biosfera
- `hc003_zona_intangible_a` - Zona Intangible Tagaeri-Taromenane
- `hc005_area_bajo_conservacion_a` - Programa SocioBosque
- `zona_recarga_hidrica` - Zonas de Recarga Hídrica

**Extensiones Habilitadas:**
- PostGIS (geometrías espaciales)
- PostGIS Topology

---

### 2. **saf_interconexion** (Base de Datos de Logs y Configuración)

**Propósito:** Almacenar logs de auditoría, configuraciones del sistema y parámetros operativos.

**DataSource JNDI:** `java:jboss/datasources/SAFLogsDS`

**Conexión:**
- **URL:** `jdbc:postgresql://localhost:5432/saf_interconexion`
- **Usuario:** `saf_app`
- **Contraseña:** `saf_app_2026`
- **Driver:** PostgreSQL JDBC

**Tablas Principales:**

#### 📝 Tablas de Logs

| Tabla | Descripción | Campos Clave |
|-------|-------------|--------------|
| `saf_request_logs` | Log de todas las solicitudes de verificación | `request_id`, `identifier_type`, `identifier_value`, `status_code`, `total_predios`, `processing_time_ms` |
| `saf_predio_logs` | Detalles de cada predio verificado por capa | `request_id`, `predio_id`, `layer_name`, `intersects`, `intersection_percentage`, `validation_passed` |
| `saf_error_logs` | Registro de errores y excepciones | `request_id`, `error_type`, `error_message`, `stack_trace` |

#### ⚙️ Tablas de Configuración

| Tabla | Descripción | Campos Clave |
|-------|-------------|--------------|
| `config_parameters` | Parámetros de configuración del sistema | `parameter_key`, `parameter_value`, `description`, `is_active` |
| `saf_validation_layers` | Configuración de capas y reglas de validación | `layer_key`, `table_name`, `validation_type`, `max_intersection_percentage`, `active`, `version` |
| `saf_validation_thresholds` | Umbrales de validación por tamaño de predio | `layer_id`, `min_hectares`, `max_hectares`, `max_percentage` |

#### 📊 Vistas de Análisis

| Vista | Descripción |
|-------|-------------|
| `v_request_summary` | Resumen diario de requests por tipo y status |
| `v_layer_usage` | Estadísticas de uso y resultados por capa |
| `v_owner_activity` | Actividad de consultas por propietario |

---

## 🔌 Configuración de DataSources en JBoss

**Archivo:** `/home/jonathan-tejada/Documents/Proyecto_Interconeccion/archivosDesarrolo/standalone.xml`

### DataSource 1: SAFCapasDS

```xml
<datasource jndi-name="java:jboss/datasources/SAFCapasDS" 
            pool-name="SAFCapasDS" 
            enabled="true" 
            use-java-context="true">
    <connection-url>jdbc:postgresql://localhost:5432/saf_postgis</connection-url>
    <driver>postgresql</driver>
    <security>
        <user-name>saf_app</user-name>
        <password>saf_app_2026</password>
    </security>
    <validation>
        <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker"/>
        <validate-on-match>true</validate-on-match>
        <background-validation>true</background-validation>
        <background-validation-millis>60000</background-validation-millis>
    </validation>
    <timeout>
        <idle-timeout-minutes>5</idle-timeout-minutes>
        <query-timeout>60</query-timeout>
    </timeout>
</datasource>
```

### DataSource 2: SAFLogsDS

```xml
<datasource jndi-name="java:jboss/datasources/SAFLogsDS" 
            pool-name="SAFLogsDS" 
            enabled="true" 
            use-java-context="true">
    <connection-url>jdbc:postgresql://localhost:5432/saf_interconexion</connection-url>
    <driver>postgresql</driver>
    <security>
        <user-name>saf_app</user-name>
        <password>saf_app_2026</password>
    </security>
    <validation>
        <valid-connection-checker class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker"/>
        <validate-on-match>true</validate-on-match>
        <background-validation>true</background-validation>
        <background-validation-millis>60000</background-validation-millis>
    </validation>
    <timeout>
        <idle-timeout-minutes>5</idle-timeout-minutes>
        <query-timeout>60</query-timeout>
    </timeout>
</datasource>
```

---

## 💻 Uso en el Código Java

### Inyección de DataSources

**Archivo:** [VerificationService.java](../saf-verification-service/src/main/java/com/saf/verification/VerificationService.java)

```java
@Stateless
@WebService(serviceName = "VerificationService")
public class VerificationService {

    @Resource(lookup = "java:jboss/datasources/SAFLogsDS")
    private DataSource logsDS;

    @Resource(lookup = "java:jboss/datasources/SAFCapasDS")
    private DataSource capasDS;
    
    // ...
}
```

### Gestión de Conexiones

**Archivo:** [DatabaseManager.java](../saf-verification-service/src/main/java/com/saf/verification/DatabaseManager.java)

```java
public class DatabaseManager {
    
    private DataSource logsDS;
    private DataSource capasDS;

    public DatabaseManager(DataSource logsDS, DataSource capasDS) {
        this.logsDS = logsDS;
        this.capasDS = capasDS;
    }
    
    // Operaciones con logsDS (saf_interconexion)
    public void logRequest(...) {
        Connection conn = logsDS.getConnection();
        // INSERT INTO saf_request_logs ...
    }
    
    public void logPredioDetails(...) {
        Connection conn = logsDS.getConnection();
        // INSERT INTO saf_predio_logs ...
    }
    
    public void logError(...) {
        Connection conn = logsDS.getConnection();
        // INSERT INTO saf_error_logs ...
    }
    
    public String getConfigValue(...) {
        Connection conn = logsDS.getConnection();
        // SELECT FROM config_parameters ...
    }
}
```

### Carga de Reglas de Validación

**Archivo:** [LayerValidationConfig.java](../saf-verification-service/src/main/java/com/saf/verification/LayerValidationConfig.java)

```java
private static void loadRulesFromDatabase() {
    Connection conn = getJDBCConnection(); // Conecta a saf_interconexion
    
    // Cargar capas activas
    String sql = "SELECT * FROM saf_validation_layers WHERE active = true";
    // ...
    
    // Cargar umbrales por tamaño
    String sql = "SELECT * FROM saf_validation_thresholds";
    // ...
}
```

---

## 🔍 Consultas Geoespaciales

Las consultas de intersección se ejecutan contra **saf_postgis** usando funciones PostGIS:

```sql
-- Ejemplo de consulta de intersección
SELECT 
    ST_Intersects(geom, ST_GeomFromText(?, 4326)) AS intersects,
    ST_Area(ST_Intersection(geom, ST_GeomFromText(?, 4326))::geography) AS intersection_area_m2
FROM areas_protegidas_snap
WHERE ST_Intersects(geom, ST_GeomFromText(?, 4326));
```

**Funciones PostGIS Utilizadas:**
- `ST_GeomFromText()` - Crear geometría desde WKT
- `ST_Intersects()` - Verificar intersección
- `ST_Intersection()` - Calcular geometría de intersección
- `ST_Area()` - Calcular área en metros cuadrados
- `::geography` - Cast para cálculos precisos en esfera

---

## 📦 Scripts de Instalación y Configuración

### Scripts de Bases de Datos

| Script | Base de Datos | Descripción |
|--------|---------------|-------------|
| [setup_local_postgis.sql](../../archivosDesarrolo/setup_local_postgis.sql) | `saf_postgis` | Inicialización de PostGIS y tablas base |
| [crear_vistas_capas_mae.sql](../../archivosDesarrolo/crear_vistas_capas_mae.sql) | `saf_postgis` | Creación de vistas de capas MAE |
| [create_logs_tables.sql](../../archivosDesarrolo/create_logs_tables.sql) | `saf_interconexion` | Creación de tablas de logs |
| [update_logs_schema.sql](../../archivosDesarrolo/update_logs_schema.sql) | `saf_interconexion` | Actualización del esquema de logs |
| [update_validation_schema.sql](../../archivosDesarrolo/update_validation_schema.sql) | `saf_interconexion` | Creación de tablas de validación |
| [configure_service.sql](../../archivosDesarrolo/configure_service.sql) | `saf_interconexion` | Configuración de parámetros del servicio |
| [datos_prueba_capas_postgis.sql](../../archivosDesarrolo/datos_prueba_capas_postgis.sql) | `saf_postgis` | Datos de prueba para capas geográficas |

### Scripts de Mantenimiento

| Script | Descripción |
|--------|-------------|
| [actualizar_geometrias_capas.sql](../../archivosDesarrolo/actualizar_geometrias_capas.sql) | Actualizar versiones de capas MAE |
| [verificar_parametrizacion_capas.sql](../../archivosDesarrolo/verificar_parametrizacion_capas.sql) | Verificar configuración de capas |
| [inspect_remote_postgis.sql](../../archivosDesarrolo/inspect_remote_postgis.sql) | Inspeccionar estructura de BD remota |
| [migrate_validation_rules.sql](../../archivosDesarrolo/migrate_validation_rules.sql) | Migrar reglas de validación |

---

## 🔐 Usuarios y Permisos

**Usuario de Aplicación:** `saf_app`
- Contraseña: `saf_app_2026`
- Permisos: SELECT, INSERT, UPDATE en tablas operativas
- Acceso: Ambas bases de datos (`saf_postgis` y `saf_interconexion`)

**Creación del Usuario:**

```sql
-- Ejecutar como superusuario postgres
CREATE USER saf_app WITH PASSWORD 'saf_app_2026';

-- Permisos en saf_postgis
GRANT CONNECT ON DATABASE saf_postgis TO saf_app;
GRANT USAGE ON SCHEMA public, h_demarcacion TO saf_app;
GRANT SELECT ON ALL TABLES IN SCHEMA public, h_demarcacion TO saf_app;

-- Permisos en saf_interconexion
GRANT CONNECT ON DATABASE saf_interconexion TO saf_app;
GRANT USAGE ON SCHEMA public TO saf_app;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO saf_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO saf_app;
```

---

## 📊 Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    JBoss EAP 7.4                            │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  VerificationService (Stateless EJB)                 │  │
│  │                                                       │  │
│  │  @Resource SAFCapasDS   @Resource SAFLogsDS          │  │
│  │      │                         │                      │  │
│  └──────┼─────────────────────────┼──────────────────────┘  │
│         │                         │                          │
└─────────┼─────────────────────────┼──────────────────────────┘
          │                         │
          ▼                         ▼
  ┌───────────────┐         ┌───────────────┐
  │ PostgreSQL    │         │ PostgreSQL    │
  │ + PostGIS     │         │               │
  │               │         │               │
  │ saf_postgis   │         │saf_interconex.│
  │               │         │               │
  │ • Capas MAE   │         │ • Logs        │
  │ • Vistas      │         │ • Config      │
  │ • Geometrías  │         │ • Validación  │
  └───────────────┘         └───────────────┘
```

---

## 🎯 Parámetros de Configuración Clave

Almacenados en `config_parameters` (base de datos `saf_interconexion`):

| Parameter Key | Descripción | Valor Predeterminado |
|---------------|-------------|----------------------|
| `predios_service_url` | URL del servicio SOAP de predios | `http://localhost:8580/servicio-soap-predios/PrediosService?wsdl` |
| `predios_service_usuario` | Usuario para servicio de predios | `1750702068` |
| `predios_service_clave` | Contraseña para servicio de predios | `1234` |
| `validation_cache_ttl_minutes` | TTL del caché de reglas (minutos) | `5` |
| `max_query_timeout_seconds` | Timeout máximo para queries PostGIS | `60` |

---

## 📈 Métricas y Monitoreo

### Tablas de Log Registran:

1. **Métricas de Request** (`saf_request_logs`)
   - Tiempo de procesamiento total
   - Tiempo de servicio externo
   - Tiempo de consultas PostGIS
   - Cantidad de predios procesados
   - Cantidad de capas verificadas

2. **Métricas de Predio** (`saf_predio_logs`)
   - Área de intersección por capa
   - Porcentaje de intersección
   - Estado de validación por capa
   - Resultado de aprobación/rechazo

3. **Errores** (`saf_error_logs`)
   - Tipo de error
   - Stack trace completo
   - Contexto del error (JSON)

### Vistas de Análisis

- `v_request_summary`: Resumen diario agregado
- `v_layer_usage`: Estadísticas por capa
- `v_owner_activity`: Actividad por propietario

---

## 🔧 Mantenimiento

### Limpieza de Logs Antiguos

```sql
-- Ejecutar periódicamente para mantener rendimiento
DELETE FROM saf_request_logs 
WHERE request_timestamp < NOW() - INTERVAL '90 days';

DELETE FROM saf_error_logs 
WHERE created_at < NOW() - INTERVAL '90 days';
```

### Actualización de Capas MAE

Cuando el MAE libere nuevas versiones:

1. Cargar nueva tabla en `saf_postgis`
2. Actualizar la vista correspondiente
3. Actualizar registro en `saf_validation_layers`

```sql
-- Ejemplo: Actualizar SNAP a nueva versión
CREATE OR REPLACE VIEW areas_protegidas_snap AS
SELECT gid, geom, nam AS nombre, are AS area_ha
FROM fa210_snap_a_20251128;  -- Nueva versión

-- Registrar cambio
UPDATE saf_validation_layers
SET version = '2025-11-28',
    updated_at = NOW()
WHERE layer_key = 'areas_protegidas_snap';
```

---

## 📚 Referencias

- **PostGIS Documentation**: https://postgis.net/docs/
- **JBoss EAP Datasources**: https://access.redhat.com/documentation/en-us/red_hat_jboss_enterprise_application_platform/7.4/
- **PostgreSQL JDBC Driver**: https://jdbc.postgresql.org/

---

**Última actualización:** 11 de Marzo de 2026  
**Versión del Documento:** 1.0  
**Contacto:** Equipo SAF Interconexión
