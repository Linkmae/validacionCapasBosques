# Guía de Instalación - SAF Verification Service

## 📋 Índice

1. [Pre-requisitos](#pre-requisitos)
2. [Instalación de Componentes Base](#instalación-de-componentes-base)
3. [Configuración de PostgreSQL y PostGIS](#configuración-de-postgresql-y-postgis)
4. [Instalación de JBoss EAP](#instalación-de-jboss-eap)
5. [Configuración de Base de Datos](#configuración-de-base-de-datos)
6. [Despliegue de la Aplicación](#despliegue-de-la-aplicación)
7. [Verificación del Servicio](#verificación-del-servicio)
8. [Troubleshooting](#troubleshooting)

---

## 🖥️ Pre-requisitos

### Hardware Mínimo

| Componente | Mínimo | Recomendado |
|------------|--------|-------------|
| **CPU** | 2 cores | 4+ cores |
| **RAM** | 4 GB | 8+ GB |
| **Disco** | 20 GB | 50+ GB (con datos geográficos) |
| **Red** | 100 Mbps | 1 Gbps |

### Software Base

- **Sistema Operativo**: 
  - Ubuntu 20.04+ / CentOS 7+ / RHEL 7+
  - Windows Server 2016+ (no recomendado para producción)
  
- **Usuario**: Acceso root o sudo

---

## 📦 Instalación de Componentes Base

### 1. Actualizar Sistema

```bash
# Ubuntu/Debian
sudo apt update && sudo apt upgrade -y

# CentOS/RHEL
sudo yum update -y
```

### 2. Instalar Java Development Kit (JDK)

#### Opción A: OpenJDK 11 (Recomendado)

```bash
# Ubuntu/Debian
sudo apt install openjdk-11-jdk -y

# CentOS/RHEL
sudo yum install java-11-openjdk-devel -y
```

#### Opción B: Oracle JDK 8/11

```bash
# Descargar desde Oracle
wget https://download.oracle.com/java/11/archive/jdk-11.0.x_linux-x64_bin.tar.gz

# Extraer
sudo mkdir -p /usr/lib/jvm
sudo tar -xzf jdk-11.0.x_linux-x64_bin.tar.gz -C /usr/lib/jvm/

# Configurar JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/jdk-11.0.x' | sudo tee -a /etc/profile
echo 'export PATH=$PATH:$JAVA_HOME/bin' | sudo tee -a /etc/profile
source /etc/profile
```

#### Verificar Instalación

```bash
java -version
# Debe mostrar: openjdk version "11.0.x" o similar

javac -version
# Debe mostrar: javac 11.0.x
```

### 3. Instalar Maven

```bash
# Ubuntu/Debian
sudo apt install maven -y

# CentOS/RHEL
sudo yum install maven -y

# O instalar manualmente
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
sudo tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt/
sudo ln -s /opt/apache-maven-3.9.6 /opt/maven

# Configurar Maven
echo 'export MAVEN_HOME=/opt/maven' | sudo tee -a /etc/profile
echo 'export PATH=$PATH:$MAVEN_HOME/bin' | sudo tee -a /etc/profile
source /etc/profile
```

#### Verificar

```bash
mvn -version
# Apache Maven 3.9.6
```

---

## 🐘 Configuración de PostgreSQL y PostGIS

### 1. Instalar PostgreSQL 14

#### Ubuntu/Debian

```bash
# Agregar repositorio oficial
sudo sh -c 'echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'
wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo apt-key add -

# Instalar
sudo apt update
sudo apt install postgresql-14 postgresql-contrib-14 -y
```

#### CentOS/RHEL

```bash
# Agregar repositorio
sudo yum install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm

# Instalar
sudo yum install -y postgresql14-server postgresql14-contrib

# Inicializar
sudo /usr/pgsql-14/bin/postgresql-14-setup initdb

# Habilitar y arrancar
sudo systemctl enable postgresql-14
sudo systemctl start postgresql-14
```

### 2. Instalar PostGIS

```bash
# Ubuntu/Debian
sudo apt install postgresql-14-postgis-3 -y

# CentOS/RHEL
sudo yum install postgis34_14 -y
```

### 3. Configurar PostgreSQL

#### Editar pg_hba.conf para permitir conexiones

```bash
# Ubicación del archivo
# Ubuntu: /etc/postgresql/14/main/pg_hba.conf
# CentOS: /var/lib/pgsql/14/data/pg_hba.conf

sudo vi /etc/postgresql/14/main/pg_hba.conf
```

Agregar o modificar estas líneas:

```conf
# TYPE  DATABASE        USER            ADDRESS                 METHOD
local   all             all                                     peer
host    all             all             127.0.0.1/32            md5
host    all             all             ::1/128                 md5
host    all             saf_app         0.0.0.0/0               md5
```

#### Editar postgresql.conf para escuchar en red

```bash
sudo vi /etc/postgresql/14/main/postgresql.conf
```

Modificar:

```conf
listen_addresses = '*'          # O la IP específica del servidor
max_connections = 100
shared_buffers = 256MB          # Ajustar según RAM disponible
```

#### Reiniciar PostgreSQL

```bash
sudo systemctl restart postgresql
```

### 4. Crear Usuario y Bases de Datos

```bash
# Conectar como postgres
sudo -u postgres psql

# Dentro de psql:
```

```sql
-- Crear usuario
CREATE USER saf_app WITH PASSWORD 'saf_app_2026';

-- Crear base de datos de configuración
CREATE DATABASE saf_interconexion OWNER saf_app;

-- Crear base de datos de capas geográficas
CREATE DATABASE saf_postgis OWNER saf_app;

-- Salir
\q
```

### 5. Habilitar PostGIS

```bash
# Conectar a saf_postgis
sudo -u postgres psql -d saf_postgis
```

```sql
-- Habilitar extensión PostGIS
CREATE EXTENSION postgis;
CREATE EXTENSION postgis_topology;

-- Verificar versión
SELECT PostGIS_Version();

-- Debe mostrar algo como: "3.3 USE_GEOS=1 USE_PROJ=1 USE_STATS=1"

\q
```

---

## 🔴 Instalación de JBoss EAP

### 1. Descargar JBoss EAP 7.4

```bash
# Opción A: Desde Red Hat (requiere suscripción)
# https://access.redhat.com/downloads/

# Opción B: WildFly (versión community)
wget https://github.com/wildfly/wildfly/releases/download/26.1.3.Final/wildfly-26.1.3.Final.tar.gz

# Para esta guía usaremos JBoss EAP 7.4
# Asumir que tienes el archivo: jboss-eap-7.4.0.zip
```

### 2. Instalar JBoss

```bash
# Crear directorio de instalación
sudo mkdir -p /opt

# Extraer
sudo unzip jboss-eap-7.4.0.zip -d /opt/

# Renombrar para facilitar
sudo mv /opt/jboss-eap-7.4 /opt/jboss-eap-7.4

# Crear usuario jboss
sudo useradd -r -s /bin/bash jboss

# Cambiar propietario
sudo chown -R jboss:jboss /opt/jboss-eap-7.4
```

### 3. Configurar Variables de Entorno

```bash
# Editar perfil del usuario jboss
sudo vi /home/jboss/.bashrc
```

Agregar:

```bash
export JBOSS_HOME=/opt/jboss-eap-7.4
export PATH=$PATH:$JBOSS_HOME/bin
```

Aplicar:

```bash
sudo -u jboss bash
source ~/.bashrc
```

### 4. Crear Usuario Administrativo

```bash
cd /opt/jboss-eap-7.4/bin
sudo -u jboss ./add-user.sh
```

Responder:
- Type of user: `a` (Management User)
- Username: `admin`
- Password: `admin123` (usar password seguro)
- Groups: (dejar vacío)
- Is this correct: `yes`

### 5. Configurar DataSources

#### Editar standalone.xml

```bash
sudo vi /opt/jboss-eap-7.4/standalone/configuration/standalone.xml
```

Buscar la sección `<datasources>` y agregar:

```xml
<datasources>
    <!-- DataSource para logs y configuración -->
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
            <exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter"/>
        </validation>
    </datasource>

    <!-- DataSource para capas geográficas -->
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
            <exception-sorter class-name="org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter"/>
        </validation>
    </datasource>
</datasources>
```

En la sección `<drivers>` agregar:

```xml
<drivers>
    <driver name="postgresql" module="org.postgresql">
        <xa-datasource-class>org.postgresql.xa.PGXADataSource</xa-datasource-class>
    </driver>
</drivers>
```

#### Instalar Driver PostgreSQL

```bash
# Crear estructura de módulo
sudo mkdir -p /opt/jboss-eap-7.4/modules/system/layers/base/org/postgresql/main

# Descargar driver
cd /opt/jboss-eap-7.4/modules/system/layers/base/org/postgresql/main
sudo wget https://jdbc.postgresql.org/download/postgresql-42.6.0.jar

# Crear module.xml
sudo vi module.xml
```

Contenido de `module.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.5" name="org.postgresql">
    <resources>
        <resource-root path="postgresql-42.6.0.jar"/>
    </resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
    </dependencies>
</module>
```

Cambiar propietario:

```bash
sudo chown -R jboss:jboss /opt/jboss-eap-7.4/modules/system/layers/base/org/postgresql
```

### 6. Configurar Puertos

Editar `standalone.xml` si necesitas cambiar puertos:

```xml
<socket-binding-group name="standard-sockets" default-interface="public" port-offset="${jboss.socket.binding.port-offset:0}">
    <socket-binding name="http" port="${jboss.http.port:9080}"/>
    <socket-binding name="https" port="${jboss.https.port:9443}"/>
    <!-- ... -->
</socket-binding-group>
```

### 7. Crear Servicio Systemd

```bash
sudo vi /etc/systemd/system/jboss.service
```

Contenido:

```ini
[Unit]
Description=JBoss EAP 7.4
After=network.target

[Service]
Type=simple
User=jboss
Group=jboss
ExecStart=/opt/jboss-eap-7.4/bin/standalone.sh -b 0.0.0.0
ExecStop=/opt/jboss-eap-7.4/bin/jboss-cli.sh --connect command=:shutdown
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

Habilitar y arrancar:

```bash
sudo systemctl daemon-reload
sudo systemctl enable jboss
sudo systemctl start jboss
```

Verificar:

```bash
sudo systemctl status jboss

# Ver logs
sudo journalctl -u jboss -f
```

---

## 🗄️ Configuración de Base de Datos

### ✨ NUEVO: Scripts SQL Organizados

**El sistema ahora incluye scripts SQL organizados para instalación simplificada.**

📁 **Ubicación**: `database_scripts/`

Los scripts deben ejecutarse en orden:

```bash
cd SAF_Services/database_scripts

# 1. Estructura de tablas
psql -U postgres -d saf_interconexion -f 01_schema_saf_interconexion.sql

# 2. Datos de capas de validación (9 capas con mensajes EUDR)
psql -U postgres -d saf_interconexion -f 02_data_validation_layers.sql

# 3. Umbrales escalonados (31 umbrales para 7 capas)
psql -U postgres -d saf_interconexion -f 03_data_validation_thresholds.sql

# 4. Vistas de consulta
psql -U postgres -d saf_interconexion -f 04_views_saf_interconexion.sql

# 5. Vistas espaciales (solo si hay capas PostGIS cargadas)
psql -U postgres -d saf_postgis -f 05_views_saf_postgis.sql
```

**📋 Ver documentación completa**: `database_scripts/README.md`

---

### Sistema de Validación Implementado

El nuevo sistema incluye:

✅ **9 Capas de Validación**:
- Bosque - No Bosque (capa principal EUDR)
- Sistema Nacional de Áreas Protegidas (SNAP)
- Bosques Protectores (BVP - Protección)
- Vegetación Protectora (BVP - Restauración)
- Patrimonio Forestal del Estado
- Reservas de Biosfera
- Programa SocioBosque
- Áreas de Protección Hídrica
- Zona Intangible Tagaeri-Taromenane
- Zona de Recarga Hídrica

✅ **31 Umbrales Escalonados**:
- Bosque - No Bosque: 13 umbrales (50% a 1%)
- SNAP, BVP, SocioBosque, etc.: 3 umbrales cada uno (10%, 5%, 3%)

✅ **Mensajes EUDR**:
- Mensajes específicos de aprobación/rechazo
- Cumplimiento de requisitos EUDR y legislación nacional

---

### 1. Verificación Post-Instalación

Después de ejecutar los scripts, verificar:

```bash
psql -U postgres -d saf_interconexion
```

```sql
-- Verificar capas configuradas
SELECT 
    layer_key,
    layer_display_name,
    validation_type,
    CASE 
        WHEN message_approved IS NOT NULL THEN '✓ CON MENSAJES EUDR'
        ELSE '⚠ SIN MENSAJES'
    END as estado
FROM saf_validation_layers
WHERE active = true
ORDER BY validation_type, layer_key;

-- Debe mostrar 9 capas activas

-- Verificar umbrales
SELECT 
    COUNT(*) as total_umbrales,
    COUNT(DISTINCT layer_id) as capas_con_umbrales
FROM saf_validation_thresholds;

-- Debe mostrar: 31 umbrales en 7 capas

-- Ver distribución de umbrales
SELECT * FROM v_layers_summary ORDER BY validation_type;

\q
```

---

### 2. Tablas de Logs (Ya incluidas en scripts)

Las tablas de logs están incluidas en `01_schema_saf_interconexion.sql`:

- `saf_request_logs` - Logs de peticiones al servicio
- `saf_predio_logs` - Logs detallados por predio y capa

---

### 3. Vistas Espaciales en PostGIS

**IMPORTANTE**: El script `05_views_saf_postgis.sql` requiere que las capas espaciales estén cargadas previamente.

Si las capas espaciales están en esquemas diferentes (ejemplo: `h_demarcacion`), ajustar las vistas según corresponda:

```bash
psql -h localhost -U saf_app -d saf_postgis
```

```sql
-- Vista para SNAP
CREATE OR REPLACE VIEW areas_protegidas_snap AS
SELECT 
    gid,
    geom,
    nam as nombre,
    cat as categoria,
    are as area_ha
FROM fa210_snap_a_08082019;  -- Tabla real del MAE

-- Vista para Patrimonio Forestal
CREATE OR REPLACE VIEW patrimonio_forestal_estado AS
SELECT 
    gid,
    geom,
    nombre,
    provincia
FROM hc001_pfe_a_11072018;  -- Tabla real del MAE

-- Vista para Bosques y Vegetación Protectora
CREATE OR REPLACE VIEW bosques_protectores AS
SELECT 
    gid,
    geom,
    nombre,
    tipo
FROM hc000_bosque_vegetacion_protectora
WHERE tipo = 'Bosque Protector';

CREATE OR REPLACE VIEW vegetacion_protectora AS
SELECT 
    gid,
    geom,
    nombre,
    tipo
FROM hc000_bosque_vegetacion_protectora
WHERE tipo = 'Vegetación Protectora';

-- Vista para Reservas Marinas
CREATE OR REPLACE VIEW reservas_marinas AS
SELECT 
    gid,
    geom,
    nombre
FROM hc002_reserva_biosfera_a;  -- Tabla real del MAE

-- Verificar que las vistas funcionan
SELECT COUNT(*) as total FROM areas_protegidas_snap;
SELECT COUNT(*) as total FROM patrimonio_forestal_estado;
SELECT COUNT(*) as total FROM bosques_protectores;
SELECT COUNT(*) as total FROM vegetacion_protectora;
SELECT COUNT(*) as total FROM reservas_marinas;

\q
```

---

## 🚀 Despliegue de la Aplicación

### 1. Obtener Código Fuente

```bash
# Clonar repositorio o copiar código
cd /home/jboss
git clone <repo-url> saf-verification-service

# O copiar desde zip
unzip saf-verification-service.zip
```

### 2. Configurar Credenciales

```bash
# Crear directorio de configuración
sudo mkdir -p /opt/saf/config
sudo chown jboss:jboss /opt/saf/config

# Crear archivo de variables de entorno
sudo -u jboss vi /opt/saf/config/saf-env.sh
```

Contenido:

```bash
#!/bin/bash
export DB_CONFIG_USERNAME="saf_app"
export DB_CONFIG_PASSWORD="saf_app_2026"
export DB_CONFIG_URL="jdbc:postgresql://localhost:5432/saf_interconexion"
export PREDIOS_SERVICE_USUARIO="usuario_servicio_predios"
export PREDIOS_SERVICE_CLAVE="clave_servicio_predios"
export PREDIOS_SERVICE_URL="http://predios-server/servicio-soap-predios/PrediosService?wsdl"
```

Proteger:

```bash
sudo chmod 600 /opt/saf/config/saf-env.sh
```

### 3. Compilar Aplicación

```bash
cd /home/jboss/saf-verification-service
source /opt/saf/config/saf-env.sh

# Compilar
mvn clean package -DskipTests
```

### 4. Desplegar con Script Automatizado

```bash
# Ejecutar script de despliegue
./deploy_mae.sh qa  # o 'dev' o 'prod'
```

O manual:

```bash
# Copiar WAR
sudo cp target/saf-verification-service-1.0.0.war \
     /opt/jboss-eap-7.4/standalone/deployments/saf-verification-service.war

# Cambiar propietario
sudo chown jboss:jboss \
     /opt/jboss-eap-7.4/standalone/deployments/saf-verification-service.war

# Esperar despliegue (~10-30 segundos)
sleep 15

# Verificar estado
ls -lh /opt/jboss-eap-7.4/standalone/deployments/saf-verification-service.war.*
```

Debe aparecer: `saf-verification-service.war.deployed`

---

## ✅ Verificación del Servicio

### 1. Verificar Logs de JBoss

```bash
tail -f /opt/jboss-eap-7.4/standalone/log/server.log
```

Buscar líneas como:

```
INFO  [com.saf.verification.LayerValidationConfig] Cargada regla: areas_protegidas_snap (2019-08-08) - Active: true
INFO  [com.saf.verification.LayerValidationConfig] Total de reglas cargadas: 6
INFO  [org.jboss.as.server] WFLYSRV0010: Deployed "saf-verification-service.war"
```

### 2. Verificar WSDL

```bash
curl http://localhost:9080/saf-verification-service/VerificationService/VerificationService?wsdl
```

Debe retornar el WSDL completo.

### 3. Prueba Funcional

```bash
curl -X POST http://localhost:9080/saf-verification-service/VerificationService/VerificationService \
  -H "Content-Type: text/xml" \
  -d '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ver="http://saf.com/verification">
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

### 4. Verificar Conexión a Base de Datos

```bash
# Verificar logs en BD
psql -h localhost -U saf_app -d saf_interconexion -c "SELECT COUNT(*) FROM saf_request_logs;"
```

---

## 🔧 Troubleshooting

### Problema: "Address already in use: bind"

**Causa**: Puerto 9080 ya está en uso.

**Solución**:

```bash
# Ver qué proceso usa el puerto
sudo lsof -i :9080

# O cambiar puerto en standalone.xml
sudo vi /opt/jboss-eap-7.4/standalone/configuration/standalone.xml
# Buscar: <socket-binding name="http" port="${jboss.http.port:9080}"/>
# Cambiar a puerto libre, ej: 9081
```

### Problema: "JDBC driver not found"

**Causa**: Driver PostgreSQL no instalado correctamente.

**Solución**:

```bash
# Verificar que existe
ls -l /opt/jboss-eap-7.4/modules/system/layers/base/org/postgresql/main/

# Debe tener:
# - postgresql-42.6.0.jar
# - module.xml

# Si falta, reinstalar siguiendo pasos de instalación de driver
```

### Problema: "Error cargando reglas desde BD"

**Causa**: No puede conectarse a saf_interconexion.

**Solución**:

```bash
# Verificar credenciales
psql -h localhost -U saf_app -d saf_interconexion -c "SELECT 1"

# Verificar datasource en standalone.xml
sudo vi /opt/jboss-eap-7.4/standalone/configuration/standalone.xml
# Buscar: SAFLogsDS
# Verificar URL, usuario y password
```

### Problema: "No se encontraron predios"

**Causa**: Servicio externo de predios no disponible.

**Solución**:

```bash
# Verificar URL del servicio
curl http://predios-server/servicio-soap-predios/PrediosService?wsdl

# Si no responde, verificar:
# 1. URL correcta en configuración
# 2. Servicio de predios arrancado
# 3. Firewall permite conexión
```

### Problema: "layerNotLoaded: true"

**Causa**: Tabla de capa no existe en PostGIS.

**Solución**:

```bash
# Verificar que existen las vistas
psql -h localhost -U saf_app -d saf_postgis -c "\dv"

# Debe listar:
# - areas_protegidas_snap
# - bosques_protectores
# - patrimonio_forestal_estado
# - vegetacion_protectora
# - reservas_marinas

# Si faltan, crearlas según paso "Crear Vistas de Capas Geográficas"
```

### Problema: JBoss no arranca

**Causa**: Error en configuración.

**Solución**:

```bash
# Ver logs detallados
sudo journalctl -u jboss -n 100 --no-pager

# Verificar sintaxis XML
xmllint --noout /opt/jboss-eap-7.4/standalone/configuration/standalone.xml

# Si hay error, restaurar backup
sudo cp /opt/jboss-eap-7.4/standalone/configuration/standalone_xml_history/standalone.last.xml \
        /opt/jboss-eap-7.4/standalone/configuration/standalone.xml
```

---

## 📋 Checklist Final de Instalación

- [ ] Java JDK 8/11 instalado y verificado
- [ ] Maven 3.6+ instalado
- [ ] PostgreSQL 14+ instalado y corriendo
- [ ] PostGIS habilitado en saf_postgis
- [ ] Usuario saf_app creado con permisos
- [ ] Bases de datos saf_interconexion y saf_postgis creadas
- [ ] Tablas de configuración y logs creadas
- [ ] Datos de reglas insertados en saf_validation_layers
- [ ] Vistas de capas geográficas creadas
- [ ] JBoss EAP 7.4 instalado
- [ ] DataSources configurados en standalone.xml
- [ ] Driver PostgreSQL instalado en JBoss
- [ ] Servicio jboss habilitado en systemd
- [ ] Aplicación compilada exitosamente
- [ ] WAR desplegado en JBoss
- [ ] WSDL accesible desde navegador
- [ ] Prueba funcional exitosa
- [ ] Logs verificados sin errores críticos

---

## 📚 Siguientes Pasos

1. **Configurar Firewall**:
   ```bash
   sudo firewall-cmd --permanent --add-port=9080/tcp
   sudo firewall-cmd --reload
   ```

2. **Configurar SSL/TLS** para HTTPS (ver documentación de JBoss)

3. **Monitoreo**: Configurar herramientas como Prometheus, Grafana, o New Relic

4. **Backups**: Configurar respaldos automáticos de bases de datos

5. **Alta Disponibilidad**: Configurar cluster de JBoss si se requiere

---

## 📞 Soporte

Para asistencia técnica:
- Documentación completa en `GUIA_PROGRAMADOR.md`
- Guía de despliegue en `DEPLOY_MAE.md`
- Resumen ejecutivo en `RESUMEN_EJECUTIVO.md`

---

**Autor**: Equipo SAF  
**Versión**: 1.0.0  
**Última actualización**: Enero 2026
