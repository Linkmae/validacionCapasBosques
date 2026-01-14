# Diagramas Mermaid - Sistema SAF Interconexión

## Fecha de Actualización
13 de enero de 2026

## 📋 Índice

1. [Diagrama de Flujo](#diagrama-de-flujo)
2. [Modelo Lógico de Datos](#modelo-lógico-de-datos)
3. [Modelo Físico de Datos](#modelo-físico-de-datos)
4. [Modelo Relacional de Datos](#modelo-relacional-de-datos)
5. [Diagrama de Componentes](#diagrama-de-componentes)
6. [Diagrama de Clases](#diagrama-de-clases)
7. [Generación de Imágenes](#generación-de-imágenes)

---

## 🔄 Diagrama de Flujo

```mermaid
flowchart TD
    %% Inicio del proceso
    A[Cliente SOAP] --> B{Recibir Request\nverifyPrediosByIdentifier}

    %% Validación inicial
    B --> C[Validar parámetros\nidentifierType, identifierValue]
    C --> D{Parámetros válidos?}

    D -->|No| E[Retornar error\n400 - ERROR_VALIDACION]
    D -->|Sí| F[Inicializar servicios\nConfigManager, PrediosClient, DatabaseManager]

    %% Consulta al servicio externo
    F --> G[Invocar servicio SOAP\nPredios MAE]
    G --> H{Respuesta exitosa?}

    H -->|No| I[Retornar error\n503 - ERROR_SERVICIO_EXTERNO]
    H -->|Sí| J[Procesar lista de predios]

    %% Procesamiento de predios
    J --> K{Número de predios\n> 0?}
    K -->|No| L[Retornar error\n404 - NO_ENCONTRADO]
    K -->|Sí| M[Inicializar contador\nprediosExitosos = 0]

    M --> N[Tomar siguiente predio]
    N --> O[Procesar predio individual]
    O --> P{Procesamiento\nexitoso?}

    P -->|Sí| Q[prediosExitosos++\nLoggear detalles del predio]
    P -->|No| R[Loggear error del predio]

    Q --> S[Crear PredioVerification]
    R --> S

    S --> T{Más predios\npor procesar?}
    T -->|Sí| N
    T -->|No| U[Crear Summary\nestadísticas finales]

    %% Logging y respuesta
    U --> V[Loggear request completo\nen saf_request_logs]
    V --> W[Crear VerifyPrediosByIdentifierResponse]
    W --> X[Retornar respuesta SOAP\n200 - OK]

    %% Manejo de errores
    E --> Y[Fin - Error]
    I --> Y
    L --> Y
    X --> Z[Fin - Éxito]
    Y --> Z

    %% Estilos
    classDef success fill:#d4edda,stroke:#155724,color:#155724
    classDef error fill:#f8d7da,stroke:#721c24,color:#721c24
    classDef process fill:#cce5ff,stroke:#004085,color:#004085
    classDef decision fill:#fff3cd,stroke:#856404,color:#856404

    class A,C,F,G,J,M,N,O,S,U,V,W success
    class E,I,L,R error
    class B,D,H,K,P,T process
    class Q decision
```

---

## 🏗️ Modelo Lógico de Datos

```mermaid
erDiagram
    %% Entidades principales
    PREDIO ||--o{ PREDIO_VERIFICACION : ""
    PREDIO_VERIFICACION ||--o{ RESULTADO_CAPA : ""
    PREDIO_VERIFICACION ||--|| RESUMEN : ""

    %% Entidades de configuración
    CAPA_VALIDACION ||--o{ REGLA_VALIDACION : ""
    REGLA_VALIDACION ||--o{ UMBRAL_TAMANIO : ""

    %% Entidades de auditoría
    SOLICITUD ||--o{ LOG_PREDIO : ""
    SOLICITUD ||--o| LOG_ERROR : ""

    %% Entidades externas
    PREDIO ||--|| PROPIETARIO : ""
    PREDIO ||--|| UBICACION_GEOGRAFICA : ""

    %% Relaciones con capas geográficas
    RESULTADO_CAPA ||--|| CAPA_GEOGRAFICA : ""

    %% Detalles de entidades
    PREDIO {
        string id_predio PK
        string codigo_predio
        string cedula_propietario
        string nombre_propietario
        number area_hectareas
        geometry geometria
        string srid
    }

    PREDIO_VERIFICACION {
        string id_predio FK
        string tipo_validacion
        boolean validacion_paso
        string mensaje_validacion
        datetime fecha_verificacion
    }

    RESULTADO_CAPA {
        string id_capa FK
        string nombre_capa
        boolean intersecta
        number area_interseccion_m2
        number porcentaje_interseccion
        boolean validacion_paso
        string mensaje_validacion
    }

    RESUMEN {
        number total_predios
        number predios_con_intersecciones
        number total_capas_verificadas
        number capas_con_intersecciones
        boolean validacion_general
        string mensaje_general
    }

    CAPA_VALIDACION {
        string id_capa PK
        string nombre_capa
        string tipo_capa
        boolean activa
        string descripcion
    }

    REGLA_VALIDACION {
        string id_regla PK
        string id_capa FK
        string tipo_validacion
        number porcentaje_maximo
        string mensaje_error
        string mensaje_exito
    }

    UMBRAL_TAMANIO {
        string id_umbral PK
        string id_regla FK
        number tamano_minimo_ha
        number tamano_maximo_ha
        number porcentaje_maximo
        string descripcion
    }

    SOLICITUD {
        string id_solicitud PK
        string tipo_identificador
        string valor_identificador
        string tipo_validacion
        datetime fecha_solicitud
        string estado
        string ip_cliente
    }

    LOG_PREDIO {
        string id_log PK
        string id_solicitud FK
        string id_predio
        string detalle_procesamiento
        datetime fecha_log
    }

    LOG_ERROR {
        string id_error PK
        string id_solicitud FK
        string tipo_error
        string mensaje_error
        string detalle_error
        datetime fecha_error
    }

    PROPIETARIO {
        string cedula PK
        string nombre_completo
        string tipo_persona
        string email
        string telefono
    }

    UBICACION_GEOGRAFICA {
        string id_ubicacion PK
        string provincia
        string canton
        string parroquia
        geometry centroide
    }

    CAPA_GEOGRAFICA {
        string id_capa PK
        string nombre_capa
        string tipo_geometria
        geometry geometria_capa
        string fuente_datos
        datetime fecha_actualizacion
    }
```

---

## 🗄️ Modelo Físico de Datos

```mermaid
erDiagram
    %% Base de datos saf_interconexion
    saf_validation_layers {
        integer id PK
        varchar_255 layer_key UK
        varchar_255 table_name
        varchar_100 schema_name
        varchar_255 layer_display_name
        varchar_50 validation_type
        boolean active "DEFAULT true"
        timestamp created_at "DEFAULT CURRENT_TIMESTAMP"
        timestamp updated_at "DEFAULT CURRENT_TIMESTAMP"
    }

    saf_validation_rules {
        integer id PK
        integer layer_id FK
        varchar_50 validation_type
        decimal_5_2 max_percentage
        decimal_10_2 min_area_m2
        text validation_message
        varchar_255 layer_version
        boolean is_active "DEFAULT true"
        varchar_50 zone_type
        text message_approved
        text message_rejected
        timestamp created_at "DEFAULT CURRENT_TIMESTAMP"
    }

    saf_validation_thresholds {
        integer id PK
        integer rule_id FK
        decimal_10_2 min_hectares
        decimal_10_2 max_hectares
        decimal_5_2 max_percentage
        text description
        timestamp created_at "DEFAULT CURRENT_TIMESTAMP"
    }

    saf_request_logs {
        bigserial id PK
        varchar_50 request_id UK
        varchar_20 identifier_type
        text identifier_value
        varchar_50 verification_type
        jsonb layers_to_check
        integer total_predios
        integer predios_exitosos
        integer predios_fallidos
        integer total_layers
        integer layers_with_intersection
        integer layers_not_loaded
        varchar_10 response_code
        text response_message
        timestamp request_timestamp "DEFAULT CURRENT_TIMESTAMP"
        interval processing_time
        inet client_ip
        text user_agent
    }

    saf_predio_logs {
        bigserial id PK
        varchar_50 request_id FK
        varchar_50 predio_id
        varchar_255 predio_codigo
        varchar_20 predio_owner_cedula
        varchar_255 predio_owner_name
        decimal_15_6 predio_area_m2
        varchar_10 predio_srid
        jsonb predio_geometry_geojson
        varchar_50 layer_id
        varchar_255 layer_name
        varchar_255 wms_layer_name
        boolean intersects
        decimal_15_6 intersection_area_m2
        decimal_7_4 percentage
        jsonb intersection_geojson
        boolean validation_passed
        text validation_message
        decimal_5_2 max_allowed_percentage
        boolean layer_not_loaded
        timestamp logged_at "DEFAULT CURRENT_TIMESTAMP"
    }

    saf_error_logs {
        bigserial id PK
        varchar_50 request_id FK
        varchar_50 error_type
        text error_message
        text error_detail
        text stack_trace
        varchar_50 predio_id
        varchar_50 layer_id
        timestamp error_timestamp "DEFAULT CURRENT_TIMESTAMP"
        inet client_ip
    }

    config_parameters {
        varchar_100 param_key PK
        text param_value
        varchar_255 description
        varchar_50 param_type
        boolean is_encrypted "DEFAULT false"
        timestamp created_at "DEFAULT CURRENT_TIMESTAMP"
        timestamp updated_at "DEFAULT CURRENT_TIMESTAMP"
    }

    %% Base de datos saf_postgis (vistas)
    mae_areas_conservacion {
        varchar_50 codigo
        varchar_255 nombre
        varchar_50 tipo_conservacion
        geometry_4326 geom "SRID 4326"
        varchar_50 fuente
        date fecha_actualizacion
    }

    mae_bosque_no_bosque {
        varchar_10 periodo
        varchar_50 clase_cobertura
        decimal_5_2 confianza
        geometry_4326 geom "SRID 4326"
        varchar_50 fuente
        date fecha_actualizacion
    }

    mae_uso_suelo_agricola {
        varchar_50 tipo_agricultura
        varchar_50 sub_tipo
        decimal_5_2 area_ha
        geometry_4326 geom "SRID 4326"
        varchar_50 fuente
        date fecha_actualizacion
    }

    mae_rios_principales {
        varchar_50 nombre_rio
        varchar_20 tipo_rio
        decimal_8_2 longitud_km
        geometry_4326 geom "SRID 4326"
        varchar_50 fuente
        date fecha_actualizacion
    }

    %% Relaciones
    saf_validation_rules ||--o{ saf_validation_thresholds : ""
    saf_validation_layers ||--o{ saf_validation_rules : ""
    saf_request_logs ||--o{ saf_predio_logs : ""
    saf_request_logs ||--o{ saf_error_logs : ""
```

---

## 🔗 Modelo Relacional de Datos

```mermaid
erDiagram
    %% Relaciones principales con cardinalidad
    saf_validation_layers ||--o{ saf_validation_rules : ""
    saf_validation_rules ||--o{ saf_validation_thresholds : ""

    saf_request_logs ||--o{ saf_predio_logs : ""
    saf_request_logs ||--o{ saf_error_logs : ""

    %% Relaciones de configuración
    config_parameters ||--o| saf_request_logs : ""

    %% Detalles de cardinalidad
    saf_validation_layers {
        integer id PK
        varchar layer_key UK
        varchar table_name
        varchar schema_name
        varchar layer_display_name
        varchar validation_type
        boolean active
    }

    saf_validation_rules {
        integer id PK
        integer layer_id FK "-> saf_validation_layers.id"
        varchar validation_type
        decimal max_percentage
        decimal min_area_m2
        text validation_message
        varchar layer_version
        boolean is_active
        varchar zone_type
        text message_approved
        text message_rejected
    }

    saf_validation_thresholds {
        integer id PK
        integer rule_id FK "-> saf_validation_rules.id"
        decimal min_hectares
        decimal max_hectares
        decimal max_percentage
        text description
    }

    saf_request_logs {
        bigserial id PK
        varchar request_id UK
        varchar identifier_type
        text identifier_value
        varchar verification_type
        jsonb layers_to_check
        integer total_predios
        integer predios_exitosos
        integer predios_fallidos
        integer total_layers
        integer layers_with_intersection
        integer layers_not_loaded
        varchar response_code
        text response_message
        timestamp request_timestamp
        interval processing_time
        inet client_ip
    }

    saf_predio_logs {
        bigserial id PK
        varchar request_id FK "-> saf_request_logs.request_id"
        varchar predio_id
        varchar predio_codigo
        varchar predio_owner_cedula
        varchar predio_owner_name
        decimal predio_area_m2
        varchar predio_srid
        jsonb predio_geometry_geojson
        varchar layer_id
        varchar layer_name
        varchar wms_layer_name
        boolean intersects
        decimal intersection_area_m2
        decimal percentage
        jsonb intersection_geojson
        boolean validation_passed
        text validation_message
        decimal max_allowed_percentage
        boolean layer_not_loaded
    }

    saf_error_logs {
        bigserial id PK
        varchar request_id FK "-> saf_request_logs.request_id"
        varchar error_type
        text error_message
        text error_detail
        text stack_trace
        varchar predio_id
        varchar layer_id
    }

    config_parameters {
        varchar param_key PK
        text param_value
        varchar description
        varchar param_type
        boolean is_encrypted
    }

    %% Vistas PostGIS (datos del MAE)
    mae_areas_conservacion {
        varchar codigo
        varchar nombre
        varchar tipo_conservacion
        geometry geom
        varchar fuente
        date fecha_actualizacion
    }

    mae_bosque_no_bosque {
        varchar periodo
        varchar clase_cobertura
        decimal confianza
        geometry geom
        varchar fuente
        date fecha_actualizacion
    }

    mae_uso_suelo_agricola {
        varchar tipo_agricultura
        varchar sub_tipo
        decimal area_ha
        geometry geom
        varchar fuente
        date fecha_actualizacion
    }

    mae_rios_principales {
        varchar nombre_rio
        varchar tipo_rio
        decimal longitud_km
        geometry geom
        varchar fuente
        date fecha_actualizacion
    }
```

---

## 🏛️ Diagrama de Componentes

```mermaid
graph TB
    %% Clientes externos
    subgraph "Clientes Externos"
        SOAP_Client[Cliente SOAP<br/>Sistema EUDR]
        MAE_Service[Servicio SOAP<br/>Predios MAE]
    end

    %% Servidor de aplicaciones
    subgraph "JBoss EAP 7.4"
        subgraph "WAR - SAF Verification Service"
            subgraph "Capa de Presentacion"
                SOAP_WS[VerificationService SOAP WebService]
            end

            subgraph "Capa de Negocio"
                ConfigMgr[ConfigManager<br/>Gestion de configuracion]
                PrediosClient[PrediosClient<br/>Cliente SOAP externo]
                ValidationEngine[Validation Engine<br/>Motor de validacion]
            end

            subgraph "Capa de Datos"
                DB_Manager[DatabaseManager<br/>Gestion BD y logs]
                LayerConfig[LayerValidationConfig<br/>Cache de reglas<br/>TTL: 5 min]
            end

            subgraph "Componentes Internos"
                Cache[(Cache System<br/>Reglas de validacion)]
                Logging[(Logging System<br/>Auditoria)]
                ErrorHandler[Error Handler<br/>Manejo de excepciones]
            end
        end
    end

    %% Bases de datos
    subgraph "Bases de Datos"
        subgraph "PostgreSQL - saf_interconexion"
            LogsDB[(saf_request_logs<br/>saf_predio_logs<br/>saf_error_logs<br/>saf_validation_*<br/>config_parameters)]
        end

        subgraph "PostGIS - saf_postgis"
            PostGISDB[(mae_areas_conservacion<br/>mae_bosque_no_bosque<br/>mae_uso_suelo_agricola<br/>mae_rios_principales<br/>...)]
        end
    end

    %% Conexiones principales
    SOAP_Client -->|SOAP/HTTP| SOAP_WS
    SOAP_WS -->|Invoca| ValidationEngine
    ValidationEngine -->|Consulta| ConfigMgr
    ValidationEngine -->|Invoca| PrediosClient
    ValidationEngine -->|Lee reglas| LayerConfig
    ValidationEngine -->|Calcula intersecciones| DB_Manager

    PrediosClient -->|SOAP/HTTP| MAE_Service

    DB_Manager -->|JDBC| LogsDB
    DB_Manager -->|JDBC| PostGISDB

    LayerConfig -->|Lee configuración| LogsDB
    ConfigMgr -->|Lee parámetros| LogsDB

    %% Conexiones internas
    ValidationEngine -.->|Actualiza| Cache
    LayerConfig -.->|Usa| Cache
    DB_Manager -.->|Registra| Logging
    ValidationEngine -.->|Maneja| ErrorHandler

    %% Estilos
    classDef external fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef server fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef component fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef database fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef internal fill:#fce4ec,stroke:#880e4f,stroke-width:2px

    class SOAP_Client,MAE_Service external
    class SOAP_WS,ConfigMgr,PrediosClient,ValidationEngine,DB_Manager,LayerConfig component
    class LogsDB,PostGISDB database
    class Cache,Logging,ErrorHandler internal

    %% Leyenda
    subgraph "Leyenda"
        Ext[External Client]:::external
        Srv[Application Server]:::server
        Comp[Business Component]:::component
        DB[Database]:::database
        Int[Internal Service]:::internal
    end
```

---

## 🏗️ Diagrama de Clases

```mermaid
classDiagram
    %% Clases principales del servicio
    class VerificationService {
        +DataSource logsDS
        +DataSource capasDS
        -ConfigManager configManager
        -PrediosClient prediosClient
        -DatabaseManager dbManager
        --
        +verifyPrediosByIdentifier(request): VerifyPrediosByIdentifierResponse
        -processPredio(predio, type, layers): PredioVerification
        -calculateIntersectionWithValidation(predio, rule): LayerResult
        -createSummary(verifications): Summary
        -initializeIfNeeded(): void
        -generateRequestId(): String
        -logErrorSafe(requestId, type, exception): void
    }

    class DatabaseManager {
        -DataSource logsDS
        -DataSource capasDS
        --
        +logRequest(request, response): void
        +logPredioDetails(requestId, verification): void
        +calculateIntersection(predioWkt, capaTabla): Map~String,Object~
        +getConfigValue(key): String
        +getValidationLayers(): List~LayerValidationRule~
        +getThresholdsBySize(layerId): List~ThresholdBySize~
        -closeQuietly(...): void
    }

    class LayerValidationConfig {
        -Map~String,List~LayerValidationRule~~ VALIDATION_RULES_CACHE
        -long lastCacheRefresh
        --
        +getRulesForType(validationType): List~LayerValidationRule~
        +loadRulesFromDatabase(): void
        -ensureCacheLoaded(): void
        -getJDBCConnection(): Connection
    }

    class PrediosClient {
        -String serviceUrl
        -String username
        -String password
        --
        +getPredios(identifierType, identifierValue): GetPrediosResponse
        -createSOAPClient(): Object
    }

    %% Clases de datos SOAP
    class VerifyPrediosByIdentifierRequest {
        -String identifierType
        -String identifierValue
        -String verificationType
        -List~String~ layersToCheck
        --
        +getters/setters
    }

    class VerifyPrediosByIdentifierResponse {
        -RequestStatus requestStatus
        -String identifierEcho
        -List~PredioVerification~ predioVerifications
        -Summary summary
        --
        +getters/setters
    }

    class PredioVerification {
        -String predioId
        -String predioCodigo
        -String predioOwnerCedula
        -String predioOwnerName
        -double predioAreaM2
        -int predioSRID
        -String predioGeometryGeoJSON
        -List~LayerResult~ layersResults
        --
        +getters/setters
    }

    class LayerResult {
        -String layerId
        -String layerName
        -String wmsLayerName
        -boolean intersects
        -double intersectionAreaM2
        -double percentage
        -String intersectionGeoJSON
        -boolean validationPassed
        -String validationMessage
        -Double maxAllowedPercentage
        -boolean layerNotLoaded
        --
        +getters/setters
    }

    %% Clases de configuración
    class LayerValidationRule {
        -String layerName
        -String layerTableName
        -String schemaName
        -String validationType
        -Double maxIntersectionPercentage
        -Double minIntersectionAreaM2
        -String validationMessage
        -String layerVersion
        -boolean isActive
        -String zoneType
        -String messageApproved
        -String messageRejected
        -List~ThresholdBySize~ thresholds
        --
        +getters/setters
        +isActive(): boolean
    }

    class ThresholdBySize {
        -Integer id
        -Double minHectares
        -Double maxHectares
        -Double maxPercentage
        -String description
        --
        +getters/setters
    }

    %% Clases de soporte
    class RequestStatus {
        -String code
        -String type
        -String message
        --
        +getters/setters
    }

    class Summary {
        -int totalPredios
        -int prediosWithIntersections
        -int totalLayersChecked
        -int layersWithIntersections
        -boolean validationPassed
        -String validationMessage
        --
        +getters/setters
    }

    class Predio {
        -String predioId
        -String predioCodigo
        -String identifier
        -String ownerName
        -double areaM2
        -int srid
        -String geometryWKT
        -String geometryGeoJSON
        --
        +getters/setters
    }

    class ConfigManager {
        -Map~String,String~ configCache
        --
        +getConfigValue(key): String
        +refreshCache(): void
    }

    %% Clases de integración
    class GetPrediosRequest {
        -String identifierType
        -String identifierValue
        --
        +getters/setters
    }

    class GetPrediosResponse {
        -List~Predio~ predios
        -String status
        -String message
        --
        +getters/setters
    }

    %% Relaciones de composición
    VerifyPrediosByIdentifierResponse *-- RequestStatus
    VerifyPrediosByIdentifierResponse *-- Summary
    VerifyPrediosByIdentifierResponse *-- PredioVerification
    PredioVerification *-- LayerResult
    LayerValidationRule *-- ThresholdBySize
    GetPrediosResponse *-- Predio

    %% Relaciones de asociación
    VerificationService --> DatabaseManager : usa
    VerificationService --> LayerValidationConfig : consulta
    VerificationService --> PrediosClient : invoca
    VerificationService --> ConfigManager : configura

    VerificationService ..> VerifyPrediosByIdentifierRequest : recibe
    VerificationService ..> VerifyPrediosByIdentifierResponse : retorna

    DatabaseManager --> LayerValidationRule : carga
    DatabaseManager --> ThresholdBySize : carga

    PrediosClient ..> GetPrediosRequest : envía
    PrediosClient ..> GetPrediosResponse : recibe

    %% Interfaces externas
    VerificationService ..|> WebService : implements
    VerificationService ..|> Stateless : implements
```

---

## 🖼️ Generación de Imágenes

### Archivos Mermaid (.mmd)

Los diagramas están disponibles en archivos separados:
- `diagrama_flujo.mmd` - Diagrama de flujo del proceso
- `modelo_logico_datos.mmd` - Modelo lógico de datos
- `modelo_fisico_datos.mmd` - Modelo físico de datos
- `modelo_relacional_datos.mmd` - Modelo relacional con cardinalidad
- `diagrama_componentes.mmd` - Arquitectura de componentes
- `diagrama_clases.mmd` - Diagrama de clases

### Generación Automática

```bash
# Usando Mermaid CLI (si está instalado)
npm install -g @mermaid-js/mermaid-cli
mmdc -i diagrama_flujo.mmd -o diagrama_flujo.png
mmdc -i diagrama_clases.mmd -o diagrama_clases.png

# Para todos los diagramas
for file in *.mmd; do
    mmdc -i "$file" -o "${file%.mmd}.png"
done
```

### Visualización Online

Los archivos `.mmd` pueden visualizarse en:
- [Mermaid Live Editor](https://mermaid.live)
- GitHub (renderiza automáticamente)
- VS Code con extensión Mermaid
- Draw.io con importación Mermaid

### Integración en Documentación

Los diagramas Mermaid se pueden integrar directamente en:
- Archivos Markdown (como este documento)
- Documentación Sphinx
- Sitios web con soporte Mermaid
- Wikis corporativos

---

## 📞 Soporte

Para soporte técnico, consultar:
- `MANUAL_PROGRAMADOR.md`: Detalles de implementación
- `MANUAL_INSTALACION.md`: Guía de instalación
- `DICCIONARIO_DATOS_SAF.md`: Especificaciones de datos