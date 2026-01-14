#!/bin/bash
#
# Script de Despliegue SAF Verification Service
# Para servidor del MAE
#
# Uso: ./deploy_mae.sh [ambiente]
#   ambiente: dev | qa | prod (default: qa)
#

set -e

AMBIENTE=${1:-qa}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================="
echo "  SAF Verification Service - Deploy"
echo "  Ambiente: $AMBIENTE"
echo "========================================="

# === Configuración según ambiente ===
case "$AMBIENTE" in
    dev)
        JBOSS_HOME="/opt/jboss-eap-7.4"
        CONFIG_DB_HOST="localhost"
        CONFIG_DB_PORT="5432"
        CONFIG_DB_NAME="saf_interconexion_dev"
        CAPAS_DB_HOST="localhost"
        CAPAS_DB_PORT="5432"
        CAPAS_DB_NAME="saf_postgis_dev"
        PREDIOS_SERVICE_URL="http://localhost:8080/servicio-soap-predios/PrediosService?wsdl"
        ;;
    qa)
        JBOSS_HOME="/opt/jboss-eap-7.4"
        CONFIG_DB_HOST="db-qa.mae.gob.ec"
        CONFIG_DB_PORT="5432"
        CONFIG_DB_NAME="saf_interconexion_qa"
        CAPAS_DB_HOST="db-qa.mae.gob.ec"
        CAPAS_DB_PORT="5432"
        CAPAS_DB_NAME="saf_postgis_qa"
        PREDIOS_SERVICE_URL="http://predios-qa.mae.gob.ec/servicio-soap-predios/PrediosService?wsdl"
        ;;
    prod)
        JBOSS_HOME="/opt/jboss-eap-7.4"
        CONFIG_DB_HOST="db-prod.mae.gob.ec"
        CONFIG_DB_PORT="5432"
        CONFIG_DB_NAME="saf_interconexion"
        CAPAS_DB_HOST="db-prod.mae.gob.ec"
        CAPAS_DB_PORT="5432"
        CAPAS_DB_NAME="saf_postgis"
        PREDIOS_SERVICE_URL="http://predios.mae.gob.ec/servicio-soap-predios/PrediosService?wsdl"
        ;;
    *)
        echo "ERROR: Ambiente desconocido: $AMBIENTE"
        echo "Uso: ./deploy_mae.sh [dev|qa|prod]"
        exit 1
        ;;
esac

# === Variables que deben ser proporcionadas externamente ===
DB_USERNAME="${SAF_DB_USERNAME:-saf_app}"
DB_PASSWORD="${SAF_DB_PASSWORD}"
PREDIOS_USUARIO="${SAF_PREDIOS_USUARIO}"
PREDIOS_CLAVE="${SAF_PREDIOS_CLAVE}"

# Validar credenciales
if [ -z "$DB_PASSWORD" ]; then
    echo "ERROR: Variable SAF_DB_PASSWORD no configurada"
    echo "Configurar con: export SAF_DB_PASSWORD='tu_password'"
    exit 1
fi

if [ -z "$PREDIOS_USUARIO" ] || [ -z "$PREDIOS_CLAVE" ]; then
    echo "ERROR: Credenciales del servicio de predios no configuradas"
    echo "Configurar con:"
    echo "  export SAF_PREDIOS_USUARIO='usuario'"
    echo "  export SAF_PREDIOS_CLAVE='clave'"
    exit 1
fi

echo ""
echo "=== Configuración ==="
echo "JBoss Home: $JBOSS_HOME"
echo "Config DB: $CONFIG_DB_HOST:$CONFIG_DB_PORT/$CONFIG_DB_NAME"
echo "Capas DB: $CAPAS_DB_HOST:$CAPAS_DB_PORT/$CAPAS_DB_NAME"
echo "Predios Service: $PREDIOS_SERVICE_URL"
echo ""

# === 1. Compilar aplicación ===
echo "[1/6] Compilando aplicación..."
cd "$SCRIPT_DIR"
mvn clean package -DskipTests || {
    echo "ERROR: Fallo en compilación"
    exit 1
}

WAR_FILE="target/saf-verification-service-1.0.0.war"
if [ ! -f "$WAR_FILE" ]; then
    echo "ERROR: No se encontró el WAR compilado: $WAR_FILE"
    exit 1
fi

echo "✓ Compilación exitosa: $WAR_FILE"

# === 2. Crear directorio de configuración ===
echo "[2/6] Creando directorio de configuración..."
CONFIG_DIR="/opt/saf/config"
sudo mkdir -p "$CONFIG_DIR"
sudo chown jboss:jboss "$CONFIG_DIR" 2>/dev/null || true

# === 3. Generar archivo de configuración ===
echo "[3/6] Generando archivo de configuración..."
CONFIG_FILE="$CONFIG_DIR/verification-${AMBIENTE}.properties"

cat > "/tmp/verification-${AMBIENTE}.properties" << EOF
# =====================================================
# SAF Verification Service - Configuración $AMBIENTE
# Generado: $(date)
# =====================================================

# === Base de Datos de Configuración ===
db.config.url=jdbc:postgresql://${CONFIG_DB_HOST}:${CONFIG_DB_PORT}/${CONFIG_DB_NAME}
db.config.username=${DB_USERNAME}
db.config.password=${DB_PASSWORD}

# === Base de Datos de Capas Geográficas ===
db.capas.url=jdbc:postgresql://${CAPAS_DB_HOST}:${CAPAS_DB_PORT}/${CAPAS_DB_NAME}
db.capas.username=${DB_USERNAME}
db.capas.password=${DB_PASSWORD}

# === Servicio Externo de Predios ===
predios.service.url=${PREDIOS_SERVICE_URL}
predios.service.usuario=${PREDIOS_USUARIO}
predios.service.clave=${PREDIOS_CLAVE}

# === Configuración de Cache ===
cache.rules.ttl.minutes=5

# === PostGIS ===
postgis.default.srid=32717

# === Logging ===
log.level=INFO
EOF

sudo mv "/tmp/verification-${AMBIENTE}.properties" "$CONFIG_FILE"
sudo chown jboss:jboss "$CONFIG_FILE" 2>/dev/null || true
sudo chmod 600 "$CONFIG_FILE"  # Solo lectura para el usuario jboss

echo "✓ Configuración generada: $CONFIG_FILE"

# === 4. Configurar variable de entorno en JBoss ===
echo "[4/6] Configurando JBoss..."

STANDALONE_CONF="$JBOSS_HOME/bin/standalone.conf"
if [ -f "$STANDALONE_CONF" ]; then
    # Backup del archivo original
    if [ ! -f "$STANDALONE_CONF.backup" ]; then
        sudo cp "$STANDALONE_CONF" "$STANDALONE_CONF.backup"
    fi
    
    # Remover configuración anterior si existe
    sudo sed -i '/# SAF Verification Service Configuration/,/^$/d' "$STANDALONE_CONF"
    
    # Agregar nueva configuración
    sudo bash -c "cat >> $STANDALONE_CONF << 'EOFCONF'

# SAF Verification Service Configuration
JAVA_OPTS=\"\$JAVA_OPTS -DDB_CONFIG_URL=jdbc:postgresql://${CONFIG_DB_HOST}:${CONFIG_DB_PORT}/${CONFIG_DB_NAME}\"
JAVA_OPTS=\"\$JAVA_OPTS -DDB_CONFIG_USERNAME=${DB_USERNAME}\"
JAVA_OPTS=\"\$JAVA_OPTS -DDB_CONFIG_PASSWORD=${DB_PASSWORD}\"
JAVA_OPTS=\"\$JAVA_OPTS -DPREDIOS_SERVICE_URL=${PREDIOS_SERVICE_URL}\"
JAVA_OPTS=\"\$JAVA_OPTS -DPREDIOS_SERVICE_USUARIO=${PREDIOS_USUARIO}\"
JAVA_OPTS=\"\$JAVA_OPTS -DPREDIOS_SERVICE_CLAVE=${PREDIOS_CLAVE}\"
EOFCONF
"
    echo "✓ Configuración de JBoss actualizada"
else
    echo "⚠ Advertencia: No se encontró $STANDALONE_CONF"
fi

# === 5. Desplegar WAR ===
echo "[5/6] Desplegando aplicación..."

DEPLOYMENTS_DIR="$JBOSS_HOME/standalone/deployments"
if [ ! -d "$DEPLOYMENTS_DIR" ]; then
    echo "ERROR: No existe directorio de despliegues: $DEPLOYMENTS_DIR"
    exit 1
fi

# Undeploy versión anterior si existe
OLD_WAR="$DEPLOYMENTS_DIR/saf-verification-service*.war*"
if ls $OLD_WAR 1> /dev/null 2>&1; then
    echo "  - Removiendo versión anterior..."
    sudo rm -f $OLD_WAR
    sleep 3
fi

# Copiar nuevo WAR
sudo cp "$WAR_FILE" "$DEPLOYMENTS_DIR/saf-verification-service.war"
sudo chown jboss:jboss "$DEPLOYMENTS_DIR/saf-verification-service.war" 2>/dev/null || true

# Esperar despliegue
echo "  - Esperando despliegue..."
sleep 10

# Verificar despliegue
if [ -f "$DEPLOYMENTS_DIR/saf-verification-service.war.deployed" ]; then
    echo "✓ Aplicación desplegada exitosamente"
elif [ -f "$DEPLOYMENTS_DIR/saf-verification-service.war.failed" ]; then
    echo "✗ ERROR: Fallo en despliegue"
    echo "Ver: $DEPLOYMENTS_DIR/saf-verification-service.war.failed"
    exit 1
else
    echo "⚠ Despliegue en progreso..."
fi

# === 6. Verificar servicio ===
echo "[6/6] Verificando servicio..."
sleep 5

WSDL_URL="http://localhost:9080/saf-verification-service/VerificationService/VerificationService?wsdl"
if curl -s -f "$WSDL_URL" > /dev/null; then
    echo "✓ Servicio disponible: $WSDL_URL"
else
    echo "⚠ Servicio no responde aún (puede tomar unos segundos más)"
fi

echo ""
echo "========================================="
echo "  ✓ DESPLIEGUE COMPLETADO"
echo "========================================="
echo ""
echo "Servicio: http://localhost:9080/saf-verification-service/VerificationService/VerificationService"
echo "WSDL: $WSDL_URL"
echo "Config: $CONFIG_FILE"
echo ""
echo "Para ver logs:"
echo "  tail -f $JBOSS_HOME/standalone/log/server.log"
echo ""
