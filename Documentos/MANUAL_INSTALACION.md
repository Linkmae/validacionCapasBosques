# Manual de Instalación - Sistema SAF Interconexión

## Fecha de Actualización
13 de enero de 2026

## 📋 Índice

1. [Pre-requisitos del Sistema](#pre-requisitos-del-sistema)
2. [Instalación de Java Development Kit (JDK)](#instalación-de-java-development-kit-jdk)
3. [Instalación de Apache Maven](#instalación-de-apache-maven)
4. [Compilación del Proyecto](#compilación-del-proyecto)
5. [Instalación de PostgreSQL y PostGIS](#instalación-de-postgresql-y-postgis)
6. [Configuración de Base de Datos SAF Interconexión](#configuración-de-base-de-datos-saf-interconexión)
7. [Conexión con PostGIS y Creación de Vistas](#conexión-con-postgis-y-creación-de-vistas)
8. [Verificación de Instalación](#verificación-de-instalación)

---

## 🖥️ Pre-requisitos del Sistema

### Requisitos Mínimos

- **Sistema Operativo**: Ubuntu 20.04+, CentOS 7+, RHEL 7+
- **CPU**: 2 cores mínimo
- **RAM**: 4 GB mínimo
- **Disco**: 20 GB disponible
- **Usuario**: Acceso root o sudo

### Software Requerido

- Java Development Kit (JDK) 11
- Apache Maven 3.6+
- PostgreSQL 12+
- PostGIS 3.0+

---

## ☕ Instalación de Java Development Kit (JDK)

### Opción 1: OpenJDK 11 (Recomendado)

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk -y

# CentOS/RHEL
sudo yum update
sudo yum install java-11-openjdk-devel -y
```

### Verificar Instalación

```bash
java -version
# Debe mostrar: openjdk version "11.0.x"

javac -version
# Debe mostrar: javac 11.0.x
```

### Configurar Variables de Entorno

```bash
# Agregar al archivo ~/.bashrc o /etc/profile
echo 'export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$PATH:$JAVA_HOME/bin' >> ~/.bashrc
source ~/.bashrc
```

---

## 🏗️ Instalación de Apache Maven

### Opción 1: Instalación desde Repositorios

```bash
# Ubuntu/Debian
sudo apt install maven -y

# CentOS/RHEL
sudo yum install maven -y
```

### Opción 2: Instalación Manual

```bash
# Descargar Maven
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz

# Extraer e instalar
sudo tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt/
sudo ln -s /opt/apache-maven-3.9.6 /opt/maven

# Configurar variables de entorno
echo 'export MAVEN_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$PATH:$MAVEN_HOME/bin' >> ~/.bashrc
source ~/.bashrc
```

### Verificar Instalación

```bash
mvn -version
# Debe mostrar: Apache Maven 3.9.6
```

---

## 🔨 Compilación del Proyecto

### Ubicarse en el Directorio del Proyecto

```bash
cd /ruta/al/proyecto/SAF_Services/saf-verification-service
```

### Compilar el Proyecto

```bash
# Limpiar y compilar
mvn clean compile

# Compilar con tests
mvn clean compile test

# Crear paquete WAR
mvn clean package
```

### Verificar Compilación Exitosa

```bash
# Verificar que se creó el archivo WAR
ls -la target/
# Debe mostrar: saf-verification-service-1.0.0.war
```

### Posibles Errores y Soluciones

- **Error de memoria**: `mvn clean compile -Xmx1024m`
- **Error de dependencias**: `mvn clean compile -U` (fuerza actualización)
- **Error de JDK**: Verificar que JAVA_HOME apunta a JDK 11

---

## 🐘 Instalación de PostgreSQL y PostGIS

### Instalar PostgreSQL

```bash
# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib -y

# CentOS/RHEL
sudo yum install postgresql-server postgresql-contrib -y
sudo postgresql-setup initdb
```

### Instalar PostGIS

```bash
# Ubuntu/Debian
sudo apt install postgis postgresql-12-postgis-3 -y

# CentOS/RHEL
sudo yum install postgis30_12 -y
```

### Iniciar y Habilitar Servicios

```bash
# Ubuntu/Debian
sudo systemctl start postgresql
sudo systemctl enable postgresql

# CentOS/RHEL
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### Configurar Usuario y Base de Datos

```bash
# Cambiar a usuario postgres
sudo -u postgres psql

# Dentro de psql:
CREATE USER saf_app WITH PASSWORD 'tu_password_seguro';
ALTER USER saf_app CREATEDB;
\q
```

---

## 🗄️ Configuración de Base de Datos SAF Interconexión

### Crear Bases de Datos

```bash
# Como superusuario postgres
sudo -u postgres psql -f scripts\ base\ datos/01_crear_bases_datos.sql
```

### Crear Tablas del Sistema SAF

```bash
# Conectar a saf_interconexion y crear tablas
sudo -u postgres psql -d saf_interconexion -f scripts\ base\ datos/02_crear_tablas_saf.sql
```

### Insertar Configuración Inicial

```bash
# Insertar datos iniciales de configuración
sudo -u postgres psql -d saf_interconexion -f scripts\ base\ datos/05_datos_iniciales_configuracion.sql
```

### Crear Usuario de Aplicación

```bash
# Crear usuario saf_app con permisos
sudo -u postgres psql -f scripts\ base\ datos/04_crear_usuario_aplicacion.sql
```

### Verificar Creación de Tablas

```sql
-- Conectar a saf_interconexion
sudo -u postgres psql -d saf_interconexion

-- Verificar tablas creadas
SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'saf_%';

-- Verificar configuración
SELECT COUNT(*) as capas_activas FROM saf_validation_layers WHERE active = true;
SELECT COUNT(*) as umbrales FROM saf_validation_thresholds;
```

---

## 🌍 Conexión con PostGIS y Creación de Vistas

### Crear Vistas en Base de Datos PostGIS

```bash
# Ejecutar script de vistas PostGIS
sudo -u postgres psql -d saf_postgis -f scripts\ base\ datos/03_crear_vistas_postgis.sql
```

### Verificar PostGIS

```sql
-- Conectar a saf_postgis
sudo -u postgres psql -d saf_postgis

-- Verificar PostGIS instalado
SELECT PostGIS_Version();

-- Verificar vistas creadas
SELECT table_name FROM information_schema.views WHERE table_schema = 'public';
```

### Configurar Conexión desde la Aplicación

La aplicación se conecta a las bases de datos a través de JBoss EAP usando los siguientes datasources:

- **SAFLogsDS**: Para `saf_interconexion` (logs y configuración)
- **SAFCapasDS**: Para `saf_postgis` (capas geográficas)

### Verificar Conectividad

```sql
-- Probar conexión desde aplicación
-- Usuario: saf_app
-- Base saf_interconexion: SELECT 1;
-- Base saf_postgis: SELECT PostGIS_Version();
```

---

## ✅ Verificación de Instalación

### Verificar Componentes Instalados

```bash
# Java
java -version
javac -version

# Maven
mvn -version

# PostgreSQL
sudo -u postgres psql -c "SELECT version();"

# PostGIS
sudo -u postgres psql -d saf_postgis -c "SELECT PostGIS_Version();"
```

### Verificar Bases de Datos

```sql
-- Conectar a saf_interconexion
sudo -u postgres psql -d saf_interconexion -c "
SELECT 'Tablas SAF:' as info, COUNT(*) as cantidad
FROM pg_tables
WHERE schemaname = 'public' AND tablename LIKE 'saf_%'

UNION ALL

SELECT 'Capas activas:', COUNT(*)
FROM saf_validation_layers
WHERE active = true

UNION ALL

SELECT 'Umbrales configurados:', COUNT(*)
FROM saf_validation_thresholds;
"

-- Conectar a saf_postgis
sudo -u postgres psql -d saf_postgis -c "
SELECT 'Vistas creadas:' as info, COUNT(*) as cantidad
FROM information_schema.views
WHERE table_schema = 'public';
"
```

### Verificar Compilación del Proyecto

```bash
cd /ruta/al/proyecto/SAF_Services/saf-verification-service

# Verificar que existe el WAR
ls -la target/*.war

# Verificar dependencias
mvn dependency:tree
```

---

## 🔧 Solución de Problemas

### Error: "Permission denied" en PostgreSQL

```bash
# Verificar usuario actual
whoami

# Cambiar a postgres si es necesario
sudo -u postgres psql
```

### Error: "FATAL: role does not exist"

```bash
# Crear usuario si no existe
sudo -u postgres createuser --createdb saf_app
sudo -u postgres psql -c "ALTER USER saf_app PASSWORD 'tu_password';"
```

### Error: "PostGIS not found"

```bash
# Verificar instalación de PostGIS
dpkg -l | grep postgis
rpm -qa | grep postgis

# Instalar si falta
sudo apt install postgis postgresql-12-postgis-3
```

### Error de Compilación Maven

```bash
# Limpiar cache de Maven
mvn clean
rm -rf ~/.m2/repository

# Recompilar
mvn compile -U
```

---

## 📞 Soporte

Para soporte técnico, consultar:
- `Documentos/DICCIONARIO_DATOS_SAF.md`: Especificaciones de base de datos
- `CONFIGURACION.md`: Guía de configuración del sistema
- `VALIDACION_IMPLEMENTACION.md`: Detalles de implementación</content>
<parameter name="filePath">/home/linkmaedev/Proyecto_Interconeccion/SAF_Services/Documentos/MANUAL_INSTALACION.md