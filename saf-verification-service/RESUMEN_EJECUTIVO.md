# SAF Verification Service - Resumen Ejecutivo

## 🎯 Objetivo

Servicio SOAP para validar predios contra capas geográficas del MAE (áreas de conservación, bosques, patrimonio forestal, etc.) con **configuración 100% externalizada** para facilitar despliegue en diferentes ambientes.

---

## ✨ Características Principales

### 1. **Configuración Externalizada**
- ✅ Sin hardcodeo de credenciales ni URLs en código
- ✅ Configuración mediante variables de entorno o archivo properties
- ✅ Cambios sin recompilación ni redespliegue
- ✅ Soporte multi-ambiente (dev/qa/prod)

### 2. **Reglas Parametrizadas en Base de Datos**
- ✅ Reglas de validación almacenadas en tabla `saf_validation_layers`
- ✅ Modificación de umbrales sin cambios de código
- ✅ Activación/desactivación de capas en tiempo real
- ✅ Cache inteligente con TTL de 5 minutos

### 3. **Arquitectura Flexible**
- ✅ Conexión a 2 bases de datos PostgreSQL/PostGIS independientes
- ✅ Integración con servicio externo de predios vía SOAP
- ✅ Logging detallado de requests y resultados
- ✅ Respuesta detallada capa por capa

---

## 📦 Componentes

```
SAF Verification Service
│
├── Base de Datos 1: saf_interconexion
│   ├── saf_validation_layers    (reglas de validación)
│   ├── saf_request_logs          (logs de requests)
│   └── saf_predio_logs           (logs detallados por predio)
│
├── Base de Datos 2: saf_postgis
│   ├── areas_protegidas_snap
│   ├── bosques_protectores
│   ├── patrimonio_forestal_estado
│   ├── vegetacion_protectora
│   └── reservas_marinas
│
└── Servicio Externo: Predios Service
    └── Consulta de predios por cédula/RUC
```

---

## 🚀 Despliegue en Servidor MAE

### Método Simplificado

```bash
# 1. Configurar credenciales (una sola vez)
export DB_CONFIG_USERNAME="saf_app"
export DB_CONFIG_PASSWORD="password_del_mae"
export PREDIOS_SERVICE_USUARIO="usuario_mae"
export PREDIOS_SERVICE_CLAVE="clave_mae"

# 2. Ejecutar script de despliegue
./deploy_mae.sh prod
```

El script automáticamente:
1. Compila la aplicación
2. Genera configuración para el ambiente especificado
3. Configura JBoss con las propiedades necesarias
4. Despliega el WAR
5. Verifica que el servicio esté funcionando

### Configuración por Ambiente

| Parámetro | Desarrollo | QA | Producción |
|-----------|------------|-----|------------|
| **Base de Datos Config** | localhost:5432/saf_interconexion_dev | db-qa.mae.gob.ec/saf_interconexion_qa | db-prod.mae.gob.ec/saf_interconexion |
| **Base de Datos Capas** | localhost:5432/saf_postgis_dev | db-qa.mae.gob.ec/saf_postgis_qa | db-prod.mae.gob.ec/saf_postgis |
| **Servicio Predios** | localhost:8080/... | predios-qa.mae.gob.ec/... | predios.mae.gob.ec/... |

---

## 🔧 Operaciones Post-Despliegue

### Actualizar Umbral de Validación (sin redespliegue)

```sql
-- Ejemplo: Cambiar umbral de vegetación protectora de 5% a 10%
UPDATE saf_validation_layers
SET max_intersection_percentage = 10.0,
    version = '2026-01-11',
    notes = 'Actualizado por resolución XYZ'
WHERE layer_key = 'vegetacion_protectora';

-- Cambio efectivo en máximo 5 minutos (cache TTL)
```

### Activar/Desactivar Capa (sin redespliegue)

```sql
-- Desactivar temporalmente una capa
UPDATE saf_validation_layers
SET active = false,
    notes = 'Desactivado por mantenimiento en capa fuente'
WHERE layer_key = 'bosques_protectores';

-- Reactivar
UPDATE saf_validation_layers
SET active = true,
    notes = 'Reactivado después de mantenimiento'
WHERE layer_key = 'bosques_protectores';
```

### Agregar Nueva Capa (sin redespliegue)

```sql
INSERT INTO saf_validation_layers (
    layer_key, table_name, schema_name, layer_display_name,
    validation_type, max_intersection_percentage, min_intersection_area_m2,
    validation_message, active, version, notes
) VALUES (
    'zonas_intangibles',
    'zonas_intangibles',
    'public',
    'Zonas Intangibles',
    'AREAS_CONSERVACION',
    0.0,
    10.0,
    'El predio NO debe intersectar con Zonas Intangibles',
    true,
    '2026-01-11',
    'Nueva capa agregada por resolución ABC'
);

-- Cambio efectivo en máximo 5 minutos
```

---

## 📊 Ejemplo de Uso

### Request SOAP

```xml
POST http://servidor-mae/saf-verification-service/VerificationService/VerificationService

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
            
            <!-- Resultado por cada capa -->
            <layersResults>
               <layerName>areas_protegidas_snap</layerName>
               <intersects>false</intersects>
               <intersectionAreaM2>0.0</intersectionAreaM2>
               <percentage>0.0</percentage>
               <validationPassed>true</validationPassed>
               <validationMessage>Sin intersección. APROBADO</validationMessage>
            </layersResults>
            
            <layersResults>
               <layerName>vegetacion_protectora</layerName>
               <intersects>true</intersects>
               <intersectionAreaM2>18000.0</intersectionAreaM2>
               <percentage>4.0</percentage>
               <validationPassed>true</validationPassed>
               <validationMessage>Intersección dentro del rango permitido (4.00% <= 5.0%). APROBADO</validationMessage>
            </layersResults>
            
            <!-- ... más capas ... -->
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

---

## 🔒 Seguridad

### Archivos Sensibles

- `/opt/saf/config/saf-env.sh` - Credenciales (permisos 600, owner jboss)
- `/opt/saf/config/verification-prod.properties` - Configuración (permisos 600, owner jboss)

### NO Incluir en Repositorio Git

```gitignore
**/saf-env.sh
**/verification-*.properties
**/*password*
**/*credential*
```

---

## 📈 Monitoreo

### Logs de Aplicación

```bash
tail -f /opt/jboss-eap-7.4/standalone/log/server.log
```

### Consultas de Auditoría

```sql
-- Requests de las últimas 24 horas
SELECT request_id, identifier_value, verification_type, 
       status_code, total_layers_checked, created_at
FROM saf_request_logs
WHERE created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;

-- Predios con intersecciones detectadas
SELECT DISTINCT owner_cedula, owner_name, layer_name, 
       intersection_percentage, validation_passed
FROM saf_predio_logs
WHERE intersects = true
  AND created_at > NOW() - INTERVAL '7 days'
ORDER BY created_at DESC;
```

---

## 📞 Soporte

### Verificación de Salud

```bash
# 1. Servicio disponible
curl -I http://localhost:9080/saf-verification-service/VerificationService/VerificationService?wsdl

# 2. Base de datos accesible
psql -h db-prod.mae.gob.ec -U saf_app -d saf_interconexion -c "SELECT COUNT(*) FROM saf_validation_layers WHERE active = true"

# 3. Reglas cargadas
grep "Total de reglas cargadas" /opt/jboss-eap-7.4/standalone/log/server.log | tail -1
```

### Problemas Comunes

| Síntoma | Causa | Solución |
|---------|-------|----------|
| "Error cargando reglas desde BD" | Credenciales incorrectas | Verificar DB_CONFIG_PASSWORD en configuración |
| "No se encontraron predios" | Servicio de predios no disponible | Verificar PREDIOS_SERVICE_URL y credenciales |
| Respuesta sin layersResults | Reglas marcadas como inactivas | Verificar active=true en saf_validation_layers |
| "layerNotLoaded: true" | Tabla de capa no existe en PostGIS | Crear vista o verificar permisos de usuario saf_app |

---

## � Documentación Disponible

### Para Equipo Técnico
- **[GUIA_PROGRAMADOR.md](GUIA_PROGRAMADOR.md)** - Arquitectura técnica completa
- **[ANALISIS_CAPACIDAD.md](ANALISIS_CAPACIDAD.md)** - Proyecciones de capacidad 2026-2030
- **[VALIDACIONES.md](../VALIDACIONES.md)** - Sistema de validaciones y API

### Para Operaciones
- **[GUIA_INSTALACION.md](GUIA_INSTALACION.md)** - Instalación desde cero
- **[DEPLOY_MAE.md](DEPLOY_MAE.md)** - Despliegue en servidores MAE
- **[deploy_mae.sh](deploy_mae.sh)** - Script automatizado

### Documentación General
- **[README.md](README.md)** - Inicio rápido y overview
- **[INDICE_DOCUMENTACION.md](../INDICE_DOCUMENTACION.md)** - Índice maestro de toda la documentación

---

## �📋 Checklist de Puesta en Producción

- [ ] Usuario `saf_app` creado en PostgreSQL con permisos
- [ ] Tabla `saf_validation_layers` creada y poblada
- [ ] Vistas de capas geográficas creadas en `saf_postgis`
- [ ] Credenciales configuradas en `/opt/saf/config/saf-env.sh`
- [ ] Script de despliegue ejecutado exitosamente
- [ ] WSDL accesible desde navegador
- [ ] Prueba funcional con cédula real exitosa
- [ ] Logs verificados sin errores
- [ ] Documentación entregada al equipo de operaciones

---

**Desarrollado por**: Equipo SAF  
**Versión**: 1.0.0  
**Fecha**: Enero 2026  
**Contacto**: [Datos de contacto del equipo]
