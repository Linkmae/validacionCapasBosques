# Guía de Despliegue - Servidor MAE

## Descripción

Este servicio está diseñado para **despliegue flexible** en el servidor del MAE con configuración externalizada. No requiere recompilación para cambiar parámetros de conexión.

---

## 📋 Pre-requisitos en Servidor MAE

1. **JBoss EAP 7.4** instalado en `/opt/jboss-eap-7.4`
2. **PostgreSQL 12+** con PostGIS 3.x
3. **Java 8+** configurado
4. **Maven 3.6+** (solo para compilación local)
5. Usuario `jboss` con permisos adecuados

---

## 🔧 Preparación del Entorno

### 1. Crear Usuario de Base de Datos

```sql
-- En PostgreSQL del servidor MAE
CREATE USER saf_app WITH PASSWORD 'password_seguro_aqui';

-- Permisos en BD de configuración
GRANT CONNECT ON DATABASE saf_interconexion TO saf_app;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO saf_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO saf_app;

-- Permisos en BD de capas geográficas
GRANT CONNECT ON DATABASE saf_postgis TO saf_app;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO saf_app;
```

### 2. Configurar Credenciales de Despliegue

En el servidor MAE, crear archivo con credenciales:

```bash
# Crear archivo de variables de entorno
sudo vi /opt/saf/config/saf-env.sh
```

Contenido:

```bash
#!/bin/bash
# Credenciales SAF Verification Service
export SAF_DB_USERNAME="saf_app"
export SAF_DB_PASSWORD="password_seguro_del_mae"
export SAF_PREDIOS_USUARIO="usuario_servicio_predios"
export SAF_PREDIOS_CLAVE="clave_servicio_predios"
```

Proteger el archivo:

```bash
sudo chmod 600 /opt/saf/config/saf-env.sh
sudo chown jboss:jboss /opt/saf/config/saf-env.sh
```

---

## 🚀 Despliegue Rápido

### Opción A: Despliegue Automatizado (Recomendado)

```bash
# 1. Cargar credenciales
source /opt/saf/config/saf-env.sh

# 2. Ejecutar script de despliegue
cd /ruta/a/saf-verification-service
./deploy_mae.sh qa    # Para ambiente QA
# o
./deploy_mae.sh prod  # Para producción
```

El script automáticamente:
- ✅ Compila la aplicación
- ✅ Genera archivo de configuración según ambiente
- ✅ Despliega en JBoss
- ✅ Verifica que el servicio esté disponible

### Opción B: Despliegue Manual

#### 1. Compilar

```bash
cd saf-verification-service
mvn clean package -DskipTests
```

#### 2. Crear Configuración

```bash
sudo mkdir -p /opt/saf/config

# Crear archivo de configuración
sudo vi /opt/saf/config/verification-prod.properties
```

Contenido (ajustar URLs y credenciales):

```properties
# Base de Datos de Configuración
db.config.url=jdbc:postgresql://db-prod.mae.gob.ec:5432/saf_interconexion
db.config.username=saf_app
db.config.password=password_real

# Base de Datos de Capas
db.capas.url=jdbc:postgresql://db-prod.mae.gob.ec:5432/saf_postgis
db.capas.username=saf_app
db.capas.password=password_real

# Servicio de Predios
predios.service.url=http://predios.mae.gob.ec/servicio-soap-predios/PrediosService?wsdl
predios.service.usuario=usuario_real
predios.service.clave=clave_real

# Cache
cache.rules.ttl.minutes=5

# PostGIS
postgis.default.srid=32717
```

Proteger:

```bash
sudo chmod 600 /opt/saf/config/verification-prod.properties
sudo chown jboss:jboss /opt/saf/config/verification-prod.properties
```

#### 3. Configurar JBoss

Editar `/opt/jboss-eap-7.4/bin/standalone.conf` y agregar al final:

```bash
# SAF Verification Service Configuration
JAVA_OPTS="$JAVA_OPTS -DSAF_CONFIG_PATH=/opt/saf/config/verification-prod.properties"
```

#### 4. Desplegar WAR

```bash
# Copiar WAR
sudo cp target/saf-verification-service-1.0.0.war \
     /opt/jboss-eap-7.4/standalone/deployments/saf-verification-service.war

# Cambiar propietario
sudo chown jboss:jboss \
     /opt/jboss-eap-7.4/standalone/deployments/saf-verification-service.war
```

#### 5. Verificar Despliegue

```bash
# Esperar ~10 segundos y verificar
ls -l /opt/jboss-eap-7.4/standalone/deployments/saf-verification-service.war.*

# Debe aparecer: saf-verification-service.war.deployed
```

---

## 🔍 Verificación del Servicio

### 1. Verificar WSDL

```bash
curl http://localhost:9080/saf-verification-service/VerificationService/VerificationService?wsdl
```

Debe retornar el WSDL completo.

### 2. Prueba Funcional

```bash
curl -X POST http://localhost:9080/saf-verification-service/VerificationService/VerificationService \
  -H "Content-Type: text/xml" \
  -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ver="http://saf.com/verification">
   <soapenv:Header/>
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
</soapenv:Envelope>'
```

### 3. Ver Logs

```bash
tail -f /opt/jboss-eap-7.4/standalone/log/server.log
```

Buscar líneas como:
```
INFO  [com.saf.verification.LayerValidationConfig] Cargada regla: areas_protegidas_snap (2019-08-08) - Active: true
INFO  [com.saf.verification.LayerValidationConfig] Total de reglas cargadas: 6
```

---

## 🔄 Actualización de Configuración (SIN Redespliegue)

### Cambiar Parámetros de Conexión

```bash
# 1. Editar configuración
sudo vi /opt/saf/config/verification-prod.properties

# 2. Recargar configuración (sin reiniciar JBoss)
# El servicio recarga automáticamente cada 5 minutos
# O forzar recarga via endpoint administrativo (si está implementado)
```

### Actualizar Reglas de Validación

Las reglas se almacenan en la BD, no en código:

```sql
-- Conectarse a saf_interconexion
UPDATE saf_validation_layers
SET max_intersection_percentage = 10.0,
    version = '2026-01-11'
WHERE layer_key = 'vegetacion_protectora';

-- La actualización se aplica automáticamente en máximo 5 minutos (cache TTL)
```

---

## 📊 Configuración de Base de Datos

### Tabla de Reglas de Validación

La configuración de capas está en `saf_interconexion.saf_validation_layers`:

```sql
SELECT layer_key, layer_display_name, active, max_intersection_percentage, version
FROM saf_validation_layers
ORDER BY validation_type, layer_key;
```

### Activar/Desactivar Capas

```sql
-- Desactivar una capa
UPDATE saf_validation_layers
SET active = false
WHERE layer_key = 'bosque_no_bosque';

-- Reactivar
UPDATE saf_validation_layers
SET active = true
WHERE layer_key = 'bosque_no_bosque';
```

---

## 🛠️ Troubleshooting

### Problema: "Error cargando reglas desde BD"

**Causa**: No puede conectarse a `saf_interconexion`

**Solución**:
1. Verificar credenciales en `/opt/saf/config/verification-prod.properties`
2. Verificar conectividad:
   ```bash
   psql -h db-prod.mae.gob.ec -U saf_app -d saf_interconexion -c "SELECT 1"
   ```

### Problema: "The column name active was not found"

**Causa**: Falta columna `active` en SELECT

**Solución**: Ya está corregido en versión 1.0.0+

### Problema: Servicio retorna capas vacías

**Causa**: Usuario `saf_app` no tiene permisos en tablas de capas

**Solución**:
```sql
-- En saf_postgis
GRANT SELECT ON areas_protegidas_snap TO saf_app;
GRANT SELECT ON bosques_protectores TO saf_app;
GRANT SELECT ON patrimonio_forestal_estado TO saf_app;
GRANT SELECT ON vegetacion_protectora TO saf_app;
GRANT SELECT ON reservas_marinas TO saf_app;
```

### Problema: No encuentra servicio de predios

**Causa**: URL incorrecta o servicio no disponible

**Solución**:
1. Verificar URL en configuración
2. Probar conectividad:
   ```bash
   curl http://predios.mae.gob.ec/servicio-soap-predios/PrediosService?wsdl
   ```

---

## 📦 Estructura de Archivos en Servidor

```
/opt/
├── jboss-eap-7.4/
│   ├── bin/
│   │   └── standalone.conf              # Configuración JBoss (agregar variable SAF_CONFIG_PATH)
│   └── standalone/
│       ├── deployments/
│       │   └── saf-verification-service.war
│       └── log/
│           └── server.log               # Logs de aplicación
└── saf/
    └── config/
        ├── saf-env.sh                   # Credenciales (NO versionar)
        └── verification-prod.properties # Configuración del servicio
```

---

## 🔐 Seguridad

### Archivos Sensibles

Proteger archivos con credenciales:

```bash
sudo chmod 600 /opt/saf/config/verification-prod.properties
sudo chmod 600 /opt/saf/config/saf-env.sh
sudo chown jboss:jboss /opt/saf/config/*
```

### NO Versionar

Agregar a `.gitignore`:
```
**/verification-*.properties
**/saf-env.sh
**/*password*
**/*credential*
```

---

## 📝 Checklist de Despliegue

- [ ] PostgreSQL accesible desde servidor MAE
- [ ] Usuario `saf_app` creado con permisos
- [ ] Tabla `saf_validation_layers` poblada con reglas
- [ ] Vistas de capas geográficas creadas en `saf_postgis`
- [ ] Servicio de predios disponible
- [ ] Archivo de configuración creado en `/opt/saf/config/`
- [ ] Variable `SAF_CONFIG_PATH` agregada a `standalone.conf`
- [ ] WAR desplegado en JBoss
- [ ] WSDL accesible
- [ ] Prueba funcional exitosa

---

## 📞 Contacto

Para soporte técnico o preguntas sobre el despliegue, contactar al equipo de desarrollo SAF.

**Versión**: 1.0.0  
**Última actualización**: 2026-01-11
