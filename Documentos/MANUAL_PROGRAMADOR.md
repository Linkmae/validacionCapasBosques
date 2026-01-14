# Manual del Programador - Sistema SAF Interconexión

## Fecha de Actualización
13 de enero de 2026

## 📋 Índice

1. [Arquitectura General](#arquitectura-general)
2. [Flujo de Procesamiento](#flujo-de-procesamiento)
3. [Componentes Principales](#componentes-principales)
4. [Funciones y Métodos Clave](#funciones-y-métodos-clave)
5. [Sistema de Validación de Capas](#sistema-de-validación-de-capas)
6. [Gestión de Base de Datos](#gestión-de-base-de-datos)
7. [Sistema de Umbrales Escalonados](#sistema-de-umbrales-escalonados)
8. [Manejo de Errores](#manejo-de-errores)
9. [Logging y Auditoría](#logging-y-auditoría)

---

## 🏗️ Arquitectura General

### Stack Tecnológico

```
Cliente SOAP → JBoss EAP 7.4 → Servicio SAF → PostgreSQL + PostGIS
     ↓              ↓              ↓              ↓
   Request    JAX-WS Web Service  Business Logic  Spatial Queries
```

### Componentes Principales

- **VerificationService.java**: Servicio web SOAP principal (endpoint)
- **DatabaseManager.java**: Gestor de conexiones y consultas a BD
- **LayerValidationConfig.java**: Configuración de reglas de validación
- **PrediosClient.java**: Cliente para servicio externo de predios
- **ConfigManager.java**: Gestión de configuración del sistema

### Bases de Datos

- **saf_interconexion**: Logs, configuración, reglas de validación
- **saf_postgis**: Capas geográficas y vistas del MAE

---

## 🔄 Flujo de Procesamiento

### 1. Recepción de Solicitud

```java
@WebMethod(operationName = "verifyPrediosByIdentifier")
public VerifyPrediosByIdentifierResponse verifyPrediosByIdentifier(
    @WebParam(name = "request") VerifyPrediosByIdentifierRequest request)
```

**Proceso:**
- Genera ID único de solicitud
- Valida parámetros de entrada
- Inicializa componentes (DatabaseManager, PrediosClient)

### 2. Consulta de Predios Externos

```java
// Llama al servicio externo de predios
GetPrediosResponse prediosResponse = prediosClient.getPredios(
    request.getIdentifierType(),
    request.getIdentifierValue()
);
```

**Funciones involucradas:**
- `PrediosClient.getPredios()`: Consulta SOAP al servicio de predios
- Validación de respuesta externa
- Mapeo de datos del predio (geometría WKT, área, propietario)

### 3. Procesamiento Individual de Predios

```java
for (Predio predio : prediosResponse.getPredios()) {
    PredioVerification verification = processPredio(predio, verificationType, layersToCheck);
    verifications.add(verification);
}
```

**Método `processPredio()`:**
- Determina tipo de validación (AREAS_CONSERVACION por defecto)
- Obtiene reglas de validación desde `LayerValidationConfig`
- Filtra capas si se especificaron específicas
- Calcula intersecciones para cada regla activa

### 4. Cálculo de Intersecciones PostGIS

```java
LayerResult result = calculateIntersectionWithValidation(predio, rule);
```

**Consulta PostGIS principal:**
```sql
SELECT
    CASE WHEN ST_Area(intersection_geom) > 0 THEN true ELSE false END AS intersects,
    ST_Area(ST_Transform(intersection_geom, 4326)::geography) AS area_m2,
    ST_AsGeoJSON(ST_Transform(intersection_geom, 4326)) AS geojson
FROM (
    SELECT ST_Union(ST_Intersection(ST_GeomFromText(?, 4326), geom)) AS intersection_geom
    FROM capa_especifica
    WHERE ST_Intersects(ST_GeomFromText(?, 4326), geom)
) AS subquery
WHERE intersection_geom IS NOT NULL
```

### 5. Aplicación de Reglas de Validación

```java
// Aplicar umbrales escalonados según tamaño del predio
ThresholdBySize threshold = rule.getThresholdForArea(predio.getAreaM2());
double maxAllowed = threshold.getMaxPercentage();
boolean exceedsThreshold = intersectionPercentage > maxAllowed;
```

### 6. Generación de Respuesta

```java
response.setRequestStatus(new RequestStatus("0", "OK", "Verificación completada"));
response.setPredioVerifications(verifications);
response.setSummary(createSummary(verifications));
```

### 7. Logging y Auditoría

```java
// Registrar solicitud completa
dbManager.logRequest(request, response);

// Registrar detalles de cada predio
dbManager.logPredioDetails(requestId, verification);
```

---

## 🔧 Componentes Principales

### VerificationService.java

**Responsabilidades:**
- Endpoint SOAP principal
- Coordinación del flujo de procesamiento
- Manejo de errores de alto nivel
- Logging de solicitudes

**Métodos clave:**
- `verifyPrediosByIdentifier()`: Método web principal
- `processPredio()`: Procesa un predio individual
- `calculateIntersectionWithValidation()`: Calcula intersección con validación
- `createSummary()`: Genera resumen estadístico

### DatabaseManager.java

**Responsabilidades:**
- Conexiones a bases de datos (logs y capas)
- Ejecución de consultas PostGIS
- Logging de auditoría
- Gestión de configuración

**Métodos clave:**
- `calculateIntersection()`: Consulta PostGIS de intersección
- `logRequest()`: Registra solicitud completa
- `logPredioDetails()`: Registra detalles de validación
- `getConfigValue()`: Obtiene configuración del sistema

### LayerValidationConfig.java

**Responsabilidades:**
- Carga de reglas de validación desde BD
- Cache de configuración (TTL 5 minutos)
- Asociación de umbrales por tamaño de predio

**Métodos clave:**
- `getRulesForType()`: Obtiene reglas para tipo de validación
- `ensureCacheLoaded()`: Asegura cache actualizado
- `loadRulesFromDatabase()`: Carga reglas desde BD

### PrediosClient.java

**Responsabilidades:**
- Cliente SOAP para servicio externo de predios
- Mapeo de respuestas externas
- Conversión de geometrías

---

## 🎯 Funciones y Métodos Clave

### Procesamiento de Geometrías

```java
// Conversión WKT → PostGIS
ST_GeomFromText(?, 4326)

// Cálculo de intersección
ST_Intersection(predio_geom, capa_geom)

// Unión de geometrías intersectadas
ST_Union(intersections)

// Cálculo de área en metros cuadrados
ST_Area(ST_Transform(geom, 4326)::geography)

// Conversión a GeoJSON
ST_AsGeoJSON(ST_Transform(geom, 4326))
```

### Sistema de Umbrales

```java
// Umbrales escalonados por tamaño de predio
class ThresholdBySize {
    private double minAreaM2;
    private double maxAreaM2;
    private double maxPercentage;
}

// Aplicación de umbral según área
ThresholdBySize threshold = rule.getThresholdForArea(predioArea);
if (intersectionPercentage > threshold.getMaxPercentage()) {
    // Excede umbral permitido
}
```

### Validación de Capas

```java
// Regla de validación por capa
class LayerValidationRule {
    private String layerName;
    private String tableName;
    private boolean active;
    private List<ThresholdBySize> thresholds;
}

// Verificación de intersección
boolean intersects = result.get("intersects");
double areaM2 = result.get("area_m2");
double percentage = (areaM2 / predioArea) * 100;
```

---

## 🗂️ Sistema de Validación de Capas

### Tipos de Validación

- **AREAS_CONSERVACION**: Áreas de conservación nacional
- **BOSQUE_NO_BOSQUE**: Cobertura boscosa
- **USO_SUELO**: Uso del suelo agrícola/forestal
- **ZONAS_AMORTIGUAMIENTO**: Zonas de amortiguamiento
- **CORREDORES_BIOLOGICOS**: Corredores biológicos
- **FUENTES_AGUA**: Fuentes de agua
- **RIOS_PRINCIPALES**: Ríos principales
- **INFRAESTRUCTURA_CRITICA**: Infraestructura crítica

### Reglas de Validación

Cada capa tiene:
- **Nombre de tabla** en PostGIS
- **Estado activo/inactivo**
- **Umbrales escalonados** por tamaño de predio
- **Nombre para WMS**

### Cache de Configuración

```java
// Cache con TTL de 5 minutos
private static final long CACHE_TTL_MS = 5 * 60 * 1000;
private static final Map<String, List<LayerValidationRule>> VALIDATION_RULES_CACHE;
```

---

## 💾 Gestión de Base de Datos

### Conexiones

```java
// Datasource para logs y configuración
@Resource(lookup = "java:jboss/datasources/SAFLogsDS")
private DataSource logsDS;

// Datasource para capas geográficas
@Resource(lookup = "java:jboss/datasources/SAFCapasDS")
private DataSource capasDS;
```

### Consultas PostGIS

```sql
-- Verificación de intersección
SELECT ST_Intersects(predio_geom, capa_geom) FROM tabla_capa;

-- Cálculo de área de intersección
SELECT ST_Area(ST_Intersection(predio_geom, capa_geom));

-- Unión de múltiples intersecciones
SELECT ST_Union(ST_Intersection(predio_geom, geom)) FROM tabla_capa;
```

### Logging de Auditoría

```sql
-- Tabla saf_request_logs
INSERT INTO saf_request_logs (
    request_id, identifier_type, identifier_value,
    total_predios, predios_exitosos, predios_fallidos,
    total_layers, layers_with_intersection, layers_not_loaded,
    processing_time_ms, request_timestamp
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

-- Tabla saf_predio_logs
INSERT INTO saf_predio_logs (
    request_id, predio_id, predio_codigo, predio_area_m2,
    layer_name, intersects, intersection_area_m2, intersection_percentage,
    exceeds_threshold, threshold_applied, processing_timestamp
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

---

## 📏 Sistema de Umbrales Escalonados

### Estructura de Umbrales

```java
class ThresholdBySize {
    double minAreaM2;      // Área mínima del rango
    double maxAreaM2;      // Área máxima del rango
    double maxPercentage;  // Porcentaje máximo permitido
}
```

### Ejemplo de Umbrales

| Tamaño de Predio | Porcentaje Máximo |
|------------------|-------------------|
| 0 - 5 ha (0 - 50,000 m²) | 10% |
| 5 - 20 ha (50,000 - 200,000 m²) | 15% |
| 20+ ha (200,000+ m²) | 20% |

### Aplicación de Umbrales

```java
// Encontrar umbral apropiado
for (ThresholdBySize threshold : rule.getThresholds()) {
    if (predioArea >= threshold.getMinAreaM2() &&
        predioArea <= threshold.getMaxAreaM2()) {
        maxAllowed = threshold.getMaxPercentage();
        break;
    }
}

// Calcular porcentaje de intersección
double intersectionPercentage = (intersectionArea / predioArea) * 100;

// Verificar si excede umbral
boolean exceedsThreshold = intersectionPercentage > maxAllowed;
```

---

## ⚠️ Manejo de Errores

### Niveles de Error

1. **Errores de Validación (400)**: Parámetros inválidos
2. **Errores de Servicio Externo (503)**: Servicio de predios no disponible
3. **Errores de Base de Datos (500)**: Problemas de conectividad
4. **Errores de Configuración (500)**: Reglas no encontradas

### Manejo Robusto

```java
try {
    // Operación crítica
    result = dbManager.calculateIntersection(predioWkt, tableName);
} catch (Exception e) {
    // Loggear error
    log.severe("ERROR calculando intersección: " + e.getMessage());
    
    // Retornar resultado seguro
    result = createSafeResult();
}
```

### Errores Específicos

- **Tabla no existe**: Retorna `table_not_found: true`
- **Geometría inválida**: Loggea y continúa con siguiente predio
- **Servicio externo caído**: Retorna error 503 con mensaje descriptivo

---

## 📊 Logging y Auditoría

### Niveles de Logging

- **INFO**: Operaciones normales, métricas
- **WARNING**: Situaciones no críticas
- **ERROR**: Errores que afectan funcionalidad
- **SEVERE**: Errores críticos del sistema

### Información Auditada

**Por Solicitud:**
- ID único de solicitud
- Timestamp de procesamiento
- Identificador consultado
- Número total de predios
- Métricas de éxito/fallo

**Por Predio:**
- ID y código del predio
- Área del predio
- Capa validada
- Área de intersección
- Porcentaje calculado
- Umbral aplicado
- Resultado de validación

### Consultas de Auditoría

```sql
-- Solicitudes recientes
SELECT * FROM saf_request_logs
ORDER BY request_timestamp DESC LIMIT 10;

-- Detalles de validación por predio
SELECT * FROM saf_predio_logs
WHERE request_id = ?
ORDER BY processing_timestamp;
```

---

## 🔍 Debugging y Desarrollo

### Logs de Consola

```bash
# Habilitar logs detallados
tail -f /opt/jboss-eap-7.4/standalone/log/server.log

# Buscar logs de una solicitud específica
grep "\[ABC123\]" server.log
```

### Puntos de Debug

1. **Recepción de solicitud**: Verificar parámetros
2. **Consulta externa**: Validar respuesta del servicio de predios
3. **Procesamiento de geometrías**: Verificar WKT y conversiones
4. **Consultas PostGIS**: Validar sintaxis SQL
5. **Aplicación de umbrales**: Verificar cálculos de porcentaje

### Testing

```bash
# Ejecutar tests unitarios
mvn test

# Ejecutar tests de integración
mvn verify

# Generar reporte de cobertura
mvn jacoco:report
```

---

## 📚 Referencias

- `DICCIONARIO_DATOS_SAF.md`: Especificaciones de base de datos
- `VALIDACION_IMPLEMENTACION.md`: Detalles de reglas de validación
- `CONFIGURACION.md`: Configuración del sistema
- `GUIA_PROGRAMADOR.md`: Guía detallada en saf-verification-service/</content>
<parameter name="filePath">/home/linkmaedev/Proyecto_Interconeccion/SAF_Services/Documentos/MANUAL_PROGRAMADOR.md