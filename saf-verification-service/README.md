# SAF Verification Service

> Sistema de Verificación de Predios contra Capas Geográficas del MAE

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://semver.org)
[![Java](https://img.shields.io/badge/java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![JBoss](https://img.shields.io/badge/JBoss-EAP%207.4-red.svg)](https://www.redhat.com/en/technologies/jboss-middleware/application-platform)
[![PostgreSQL](https://img.shields.io/badge/postgresql-12%2B-blue.svg)](https://www.postgresql.org/)
[![PostGIS](https://img.shields.io/badge/PostGIS-3.x-green.svg)](https://postgis.net/)

---

## 📖 Descripción

**SAF Verification Service** es un servicio SOAP que valida predios contra múltiples capas geográficas del Ministerio del Ambiente del Ecuador (MAE), incluyendo:

- 🌳 Áreas Protegidas (SNAP)
- 🌲 Bosques Protectores
- 🏞️ Patrimonio Forestal del Estado
- 🌿 Vegetación Protectora
- 🌊 Reservas de Biosfera

### Características Principales

✅ **Configuración Externalizada** - Sin hardcodeo de credenciales  
✅ **Reglas Parametrizadas** - Almacenadas en base de datos  
✅ **Cache Inteligente** - TTL de 5 minutos para máximo rendimiento  
✅ **Multi-Ambiente** - Soporte para dev/qa/prod con misma aplicación  
✅ **Respuesta Detallada** - Resultado capa por capa con métricas  
✅ **PostGIS Integration** - Cálculos geoespaciales precisos  
✅ **Logging Completo** - Auditoría de todas las operaciones  

---

## 📚 Documentación

### Para Gerencia / Equipo Técnico

📄 **[RESUMEN_EJECUTIVO.md](RESUMEN_EJECUTIVO.md)**  
Visión general del proyecto, características, arquitectura simplificada y casos de uso.

### Para Operaciones / DevOps

🚀 **[GUIA_INSTALACION.md](GUIA_INSTALACION.md)**  
Instalación completa desde cero: Java, PostgreSQL, PostGIS, JBoss, configuración de base de datos y despliegue.

📋 **[DEPLOY_MAE.md](DEPLOY_MAE.md)**  
Guía específica para despliegue en servidores del MAE con configuración por ambiente.

📊 **[ANALISIS_CAPACIDAD.md](ANALISIS_CAPACIDAD.md)**  
Análisis de capacidad y proyecciones para los próximos 5 años (TPS, RAM, CPU, almacenamiento, red).

### Para Desarrolladores

👨‍💻 **[GUIA_PROGRAMADOR.md](GUIA_PROGRAMADOR.md)**  
Arquitectura técnica detallada, componentes, flujo de datos, base de datos, desarrollo local y extensibilidad.

📝 **[VALIDACIONES.md](VALIDACIONES.md)**  
Sistema de validaciones, tipos soportados, reglas de negocio y ejemplos de requests/responses.

### Otros Documentos

📊 **[PRUEBAS_IMPLEMENTACION.md](PRUEBAS_IMPLEMENTACION.md)**  
Guía de pruebas de la implementación parametrizada.

---

## 🚀 Inicio Rápido

### Pre-requisitos

- Java JDK 8 o 11
- Maven 3.6+
- PostgreSQL 12+ con PostGIS 3.x
- JBoss EAP 7.4

### Instalación Rápida

```bash
# 1. Configurar credenciales
export DB_CONFIG_USERNAME="saf_app"
export DB_CONFIG_PASSWORD="tu_password"
export PREDIOS_SERVICE_USUARIO="usuario_predios"
export PREDIOS_SERVICE_CLAVE="clave_predios"

# 2. Ejecutar script de despliegue
cd saf-verification-service
./deploy_mae.sh qa

# 3. Verificar
curl http://localhost:9080/saf-verification-service/VerificationService/VerificationService?wsdl
```

Ver **[GUIA_INSTALACION.md](GUIA_INSTALACION.md)** para instalación detallada paso a paso.

---

## 💡 Ejemplo de Uso

### Request SOAP

```xml
POST http://servidor/saf-verification-service/VerificationService/VerificationService

<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" 
                  xmlns:ver="http://saf.com/verification">
   <soapenv:Body>
      <ver:verifyPrediosByIdentifier>
         <request>
            <identifierType>CEDULA</identifierType>
            <identifierValue>1234567890</identifierValue>
            <verificationType>AREAS_CONSERVACION</verificationType>
            <includeIntersectionGeoJSON>false</includeIntersectionGeoJSON>
         </request>
      </ver:verifyPrediosByIdentifier>
   </soapenv:Body>
</soapenv:Envelope>
```

### Response

```xml
<soap:Body>
   <ns2:verifyPrediosByIdentifierResponse>
      <verificationResponse>
         <identifierEcho>CEDULA:1234567890</identifierEcho>
         <predioVerifications>
            <predioId>P-00123</predioId>
            <predioAreaM2>450000.0</predioAreaM2>
            <layersResults>
               <layerName>areas_protegidas_snap</layerName>
               <intersects>false</intersects>
               <percentage>0.0</percentage>
               <validationPassed>true</validationPassed>
               <validationMessage>Sin intersección. APROBADO</validationMessage>
            </layersResults>
            <!-- Más capas... -->
         </predioVerifications>
         <requestStatus>
            <code>0</code>
            <errorType>OK</errorType>
            <message>Verificación completada exitosamente</message>
         </requestStatus>
      </verificationResponse>
   </ns2:verifyPrediosByIdentifierResponse>
</soap:Body>
```

Ver **[VALIDACIONES.md](VALIDACIONES.md)** para más ejemplos.

---

## 🏗️ Arquitectura

```
┌─────────────────────┐
│   Cliente SOAP      │
└──────────┬──────────┘
           │
           ↓
┌──────────────────────────────────────┐
│  JBoss EAP 7.4                       │
│  ┌────────────────────────────────┐  │
│  │  VerificationService (SOAP)    │  │
│  └─────────────┬──────────────────┘  │
│                │                      │
│  ┌─────────────▼──────────────────┐  │
│  │  LayerValidationConfig         │  │
│  │  (Rules Cache)                 │  │
│  └─────────────┬──────────────────┘  │
│                │                      │
│  ┌─────────────▼──────────────────┐  │
│  │  DatabaseManager               │  │
│  │  (PostGIS Queries)             │  │
│  └────────────────────────────────┘  │
└──────────────────────────────────────┘
           │           │
    ┌──────┴───┐   ┌──┴──────┐
    │          │   │         │
    ↓          ↓   ↓         ↓
┌──────────┐  ┌──────────┐  ┌──────────────┐
│ Config   │  │ PostGIS  │  │ Predios      │
│ Database │  │ Database │  │ Service      │
└──────────┘  └──────────┘  └──────────────┘
```

Ver **[GUIA_PROGRAMADOR.md](GUIA_PROGRAMADOR.md)** para arquitectura detallada.

---

## 🗂️ Estructura del Proyecto

```
saf-verification-service/
├── src/
│   ├── main/
│   │   ├── java/com/saf/verification/
│   │   │   ├── VerificationService.java          # Endpoint SOAP
│   │   │   ├── LayerValidationConfig.java        # Gestión de reglas
│   │   │   ├── DatabaseManager.java              # Operaciones PostGIS
│   │   │   ├── PrediosClient.java                # Cliente SOAP externo
│   │   │   └── models/                           # DTOs
│   │   └── resources/
│   │       └── verification.properties           # Config default
│   └── test/
│       └── java/                                 # Tests unitarios
│
├── config-example.properties                     # Plantilla configuración
├── deploy_mae.sh                                 # Script despliegue
├── pom.xml                                       # Maven
│
├── README.md                                     # Este archivo
├── GUIA_INSTALACION.md                          # Guía instalación completa
├── GUIA_PROGRAMADOR.md                          # Guía técnica desarrolladores
├── DEPLOY_MAE.md                                # Guía despliegue MAE
├── RESUMEN_EJECUTIVO.md                         # Resumen para gerencia
├── VALIDACIONES.md                              # Sistema de validaciones
└── PRUEBAS_IMPLEMENTACION.md                    # Guía de pruebas
```

---

## ⚙️ Configuración

### Variables de Entorno

```bash
# Base de Datos de Configuración
export DB_CONFIG_URL="jdbc:postgresql://localhost:5432/saf_interconexion"
export DB_CONFIG_USERNAME="saf_app"
export DB_CONFIG_PASSWORD="password_seguro"

# Servicio Externo de Predios
export PREDIOS_SERVICE_URL="http://predios.mae.gob.ec/servicio-soap-predios/PrediosService?wsdl"
export PREDIOS_SERVICE_USUARIO="usuario"
export PREDIOS_SERVICE_CLAVE="clave"
```

### Actualizar Reglas (Sin Redespliegue)

```sql
-- Cambiar umbral de validación
UPDATE saf_validation_layers
SET max_intersection_percentage = 10.0,
    version = '2026-01-11'
WHERE layer_key = 'vegetacion_protectora';

-- Cambio efectivo en máximo 5 minutos (cache TTL)
```

---

## 🧪 Testing

### Verificar WSDL

```bash
curl http://localhost:9080/saf-verification-service/VerificationService/VerificationService?wsdl
```

### Prueba Funcional

```bash
curl -X POST http://localhost:9080/saf-verification-service/VerificationService/VerificationService \
  -H "Content-Type: text/xml" \
  -d @test-request.xml
```

Ver **[PRUEBAS_IMPLEMENTACION.md](PRUEBAS_IMPLEMENTACION.md)** para pruebas detalladas.

---

## 📊 Monitoreo

### Logs de Aplicación

```bash
tail -f /opt/jboss-eap-7.4/standalone/log/server.log
```

### Consultas de Auditoría

```sql
-- Requests últimas 24 horas
SELECT request_id, identifier_value, verification_type, 
       status_code, total_layers_checked, created_at
FROM saf_request_logs
WHERE created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;

-- Predios con intersecciones
SELECT owner_cedula, layer_name, 
       intersection_percentage, validation_passed
FROM saf_predio_logs
WHERE intersects = true
  AND created_at > NOW() - INTERVAL '7 days';
```

---

## 🔧 Troubleshooting

### Problema Común: "Error cargando reglas desde BD"

**Solución**:
```bash
# Verificar conectividad
psql -h localhost -U saf_app -d saf_interconexion -c "SELECT COUNT(*) FROM saf_validation_layers WHERE active = true"
```

### Problema: Respuesta sin layersResults

**Solución**:
```sql
-- Verificar que las reglas estén activas
SELECT layer_key, active FROM saf_validation_layers;

-- Activar si es necesario
UPDATE saf_validation_layers SET active = true WHERE layer_key = 'nombre_capa';
```

Ver **[GUIA_INSTALACION.md#troubleshooting](GUIA_INSTALACION.md#troubleshooting)** para más problemas comunes.

---

## 🤝 Contribuciones

Este proyecto es mantenido por el equipo SAF del MAE. Para contribuciones:

1. Fork el repositorio
2. Crear branch de feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push al branch (`git push origin feature/nueva-funcionalidad`)
5. Crear Pull Request

---

## 📄 Licencia

Este proyecto es propiedad del Ministerio del Ambiente del Ecuador (MAE).

---

## 📞 Contacto y Soporte

- **Equipo de Desarrollo SAF**: [contacto@mae.gob.ec]
- **Documentación Técnica**: Ver archivos MD en este repositorio
- **Issues**: Reportar en el sistema de tickets interno

---

## 🗓️ Historial de Versiones

### v1.0.0 (Enero 2026)
- ✨ Versión inicial
- ✅ Configuración externalizada
- ✅ Reglas parametrizadas en BD
- ✅ 6 capas de validación configuradas
- ✅ Cache con TTL
- ✅ Logging completo
- ✅ Documentación completa

---

## 📚 Documentación Adicional

| Documento | Audiencia | Descripción |
|-----------|-----------|-------------|
| [RESUMEN_EJECUTIVO.md](RESUMEN_EJECUTIVO.md) | Gerencia / Stakeholders | Visión general y casos de uso |
| [GUIA_INSTALACION.md](GUIA_INSTALACION.md) | DevOps / SysAdmin | Instalación completa desde cero |
| [DEPLOY_MAE.md](DEPLOY_MAE.md) | DevOps | Despliegue en servidores MAE |
| [GUIA_PROGRAMADOR.md](GUIA_PROGRAMADOR.md) | Desarrolladores | Arquitectura y componentes técnicos |
| [VALIDACIONES.md](VALIDACIONES.md) | Todos | Sistema de validaciones y API |
| [PRUEBAS_IMPLEMENTACION.md](PRUEBAS_IMPLEMENTACION.md) | QA / Testers | Guía de pruebas |

---

**Desarrollado por**: Equipo SAF - Ministerio del Ambiente del Ecuador  
**Versión**: 1.0.0  
**Última actualización**: Enero 2026
