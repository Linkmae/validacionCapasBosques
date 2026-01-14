package com.saf.verification;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Configuración de reglas de validación de capas geográficas.
 * Carga las reglas desde la base de datos con caché de 5 minutos.
 */
public class LayerValidationConfig {
    
    private static final Logger logger = Logger.getLogger(LayerValidationConfig.class.getName());
    
    // Cache de reglas con TTL de 5 minutos
    private static final Map<String, List<LayerValidationRule>> VALIDATION_RULES_CACHE = new ConcurrentHashMap<>();
    private static volatile long lastCacheRefresh = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutos
    
    /**
     * Asegura que el caché esté cargado y actualizado
     */
    private static synchronized void ensureCacheLoaded() {
        long currentTime = System.currentTimeMillis();
        if (VALIDATION_RULES_CACHE.isEmpty() || (currentTime - lastCacheRefresh) > CACHE_TTL_MS) {
            logger.info("Recargando reglas de validación desde base de datos...");
            loadRulesFromDatabase();
            lastCacheRefresh = currentTime;
        }
    }
    
    /**
     * Carga las reglas desde la tabla saf_validation_layers y sus umbrales asociados
     */
    private static void loadRulesFromDatabase() {
        Connection conn = null;
        PreparedStatement psLayers = null;
        PreparedStatement psThresholds = null;
        ResultSet rsLayers = null;
        ResultSet rsThresholds = null;
        
        try {
            conn = getJDBCConnection();
            
            // 1. Cargar capas
            String sqlLayers = "SELECT id, layer_key, table_name, schema_name, layer_display_name, " +
                        "validation_type, max_intersection_percentage, min_intersection_area_m2, " +
                        "validation_message, active, version, zone_type, message_approved, message_rejected " +
                        "FROM saf_validation_layers " +
                        "WHERE active = true " +
                        "ORDER BY validation_type, layer_key";
            
            psLayers = conn.prepareStatement(sqlLayers);
            rsLayers = psLayers.executeQuery();
            
            // Limpiar caché actual
            VALIDATION_RULES_CACHE.clear();
            Map<Integer, LayerValidationRule> rulesByLayerId = new HashMap<>();
            
            while (rsLayers.next()) {
                int layerId = rsLayers.getInt("id");
                String validationType = rsLayers.getString("validation_type");
                
                LayerValidationRule rule = new LayerValidationRule();
                rule.setLayerName(rsLayers.getString("layer_key"));
                rule.setLayerTableName(rsLayers.getString("table_name"));
                rule.setSchemaName(rsLayers.getString("schema_name"));
                rule.setValidationType(validationType);
                rule.setMaxIntersectionPercentage(rsLayers.getDouble("max_intersection_percentage"));
                rule.setMinIntersectionAreaM2(rsLayers.getDouble("min_intersection_area_m2"));
                rule.setValidationMessage(rsLayers.getString("validation_message"));
                rule.setLayerVersion(rsLayers.getString("version"));
                rule.setActive(rsLayers.getBoolean("active"));
                rule.setZoneType(rsLayers.getString("zone_type"));
                rule.setMessageApproved(rsLayers.getString("message_approved"));
                rule.setMessageRejected(rsLayers.getString("message_rejected"));
                
                VALIDATION_RULES_CACHE
                    .computeIfAbsent(validationType, k -> new ArrayList<>())
                    .add(rule);
                
                rulesByLayerId.put(layerId, rule);
                    
                logger.info("Cargada regla: " + rule.getLayerName() + " (" + rule.getLayerVersion() + ") - Active: " + rule.isActive());
            }
            
            // 2. Cargar umbrales para cada capa
            String sqlThresholds = "SELECT layer_id, min_hectares, max_hectares, max_percentage, description " +
                        "FROM saf_validation_thresholds " +
                        "ORDER BY layer_id, min_hectares";
            
            psThresholds = conn.prepareStatement(sqlThresholds);
            rsThresholds = psThresholds.executeQuery();
            
            int thresholdsCount = 0;
            while (rsThresholds.next()) {
                int layerId = rsThresholds.getInt("layer_id");
                LayerValidationRule rule = rulesByLayerId.get(layerId);
                
                if (rule != null) {
                    ThresholdBySize threshold = new ThresholdBySize();
                    threshold.setMinHectares(rsThresholds.getDouble("min_hectares"));
                    
                    double maxHa = rsThresholds.getDouble("max_hectares");
                    threshold.setMaxHectares(rsThresholds.wasNull() ? null : maxHa);
                    
                    threshold.setMaxPercentage(rsThresholds.getDouble("max_percentage"));
                    threshold.setDescription(rsThresholds.getString("description"));
                    
                    rule.addThreshold(threshold);
                    thresholdsCount++;
                }
            }
            
            logger.info("Total de reglas cargadas: " + VALIDATION_RULES_CACHE.values().stream()
                .mapToInt(List::size).sum() + " con " + thresholdsCount + " umbrales");
                
        } catch (Exception e) {
            logger.severe("Error cargando reglas desde BD: " + e.getMessage());
            e.printStackTrace();
            logger.info("Cargando reglas hardcodeadas como fallback...");
            loadHardcodedFallback();
        } finally {
            try {
                if (rsThresholds != null) rsThresholds.close();
                if (psThresholds != null) psThresholds.close();
                if (rsLayers != null) rsLayers.close();
                if (psLayers != null) psLayers.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                logger.warning("Error cerrando conexión: " + e.getMessage());
            }
        }
    }
    
    /**
     * Fallback: carga reglas hardcodeadas si falla la BD
     */
    private static void loadHardcodedFallback() {
        VALIDATION_RULES_CACHE.clear();
        initializeBosqueNoBosqueRules();
        initializeAreasConservacionRules();
    }
    
    /**
     * Reglas hardcodeadas para BOSQUE_NO_BOSQUE
     */
    private static void initializeBosqueNoBosqueRules() {
        List<LayerValidationRule> bosqueRules = new ArrayList<>();
        
        LayerValidationRule rule = new LayerValidationRule();
        rule.setLayerName("bosque_no_bosque");
        rule.setLayerTableName("bosque_no_bosque");
        rule.setSchemaName("public");
        rule.setValidationType("BOSQUE_NO_BOSQUE");
        rule.setMaxIntersectionPercentage(0.0);
        rule.setMinIntersectionAreaM2(10.0);
        rule.setValidationMessage("El predio no debe intersectar con la capa de bosque");
        rule.setLayerVersion("prueba-v1");
        rule.setActive(true);
        
        bosqueRules.add(rule);
        VALIDATION_RULES_CACHE.put("BOSQUE_NO_BOSQUE", bosqueRules);
    }
    
    /**
     * Reglas hardcodeadas para AREAS_CONSERVACION
     */
    private static void initializeAreasConservacionRules() {
        List<LayerValidationRule> conservacionRules = new ArrayList<>();
        
        // SNAP
        LayerValidationRule snap = new LayerValidationRule();
        snap.setLayerName("areas_protegidas_snap");
        snap.setLayerTableName("areas_protegidas_snap");
        snap.setSchemaName("public");
        snap.setValidationType("AREAS_CONSERVACION");
        snap.setMaxIntersectionPercentage(0.0);
        snap.setMinIntersectionAreaM2(10.0);
        snap.setValidationMessage("Intersección con Área Protegida SNAP");
        snap.setLayerVersion("2019-08-08");
        snap.setActive(true);
        conservacionRules.add(snap);
        
        // Patrimonio Forestal
        LayerValidationRule pfe = new LayerValidationRule();
        pfe.setLayerName("patrimonio_forestal_estado");
        pfe.setLayerTableName("patrimonio_forestal_estado");
        pfe.setSchemaName("public");
        pfe.setValidationType("AREAS_CONSERVACION");
        pfe.setMaxIntersectionPercentage(0.0);
        pfe.setMinIntersectionAreaM2(10.0);
        pfe.setValidationMessage("Intersección con Patrimonio Forestal del Estado");
        pfe.setLayerVersion("2018-07-11");
        pfe.setActive(true);
        conservacionRules.add(pfe);
        
        // Bosques Protectores
        LayerValidationRule bosquesProtectores = new LayerValidationRule();
        bosquesProtectores.setLayerName("bosques_protectores");
        bosquesProtectores.setLayerTableName("bosques_protectores");
        bosquesProtectores.setSchemaName("public");
        bosquesProtectores.setValidationType("AREAS_CONSERVACION");
        bosquesProtectores.setMaxIntersectionPercentage(0.0);
        bosquesProtectores.setMinIntersectionAreaM2(10.0);
        bosquesProtectores.setValidationMessage("Intersección con Bosque Protector");
        bosquesProtectores.setLayerVersion("2019-08-07");
        bosquesProtectores.setActive(true);
        conservacionRules.add(bosquesProtectores);
        
        // Vegetación Protectora
        LayerValidationRule vegetacion = new LayerValidationRule();
        vegetacion.setLayerName("vegetacion_protectora");
        vegetacion.setLayerTableName("vegetacion_protectora");
        vegetacion.setSchemaName("public");
        vegetacion.setValidationType("AREAS_CONSERVACION");
        vegetacion.setMaxIntersectionPercentage(5.0);
        vegetacion.setMinIntersectionAreaM2(10.0);
        vegetacion.setValidationMessage("Intersección con Vegetación Protectora (máximo 5%)");
        vegetacion.setLayerVersion("2019-08-07");
        vegetacion.setActive(true);
        conservacionRules.add(vegetacion);
        
        // Reservas Marinas
        LayerValidationRule reservas = new LayerValidationRule();
        reservas.setLayerName("reservas_marinas");
        reservas.setLayerTableName("reservas_marinas");
        reservas.setSchemaName("public");
        reservas.setValidationType("AREAS_CONSERVACION");
        reservas.setMaxIntersectionPercentage(0.0);
        reservas.setMinIntersectionAreaM2(10.0);
        reservas.setValidationMessage("Intersección con Reserva de Biosfera");
        reservas.setLayerVersion("2019");
        reservas.setActive(true);
        conservacionRules.add(reservas);
        
        VALIDATION_RULES_CACHE.put("AREAS_CONSERVACION", conservacionRules);
    }
    
    /**
     * Obtiene reglas para un tipo de validación
     */
    public static List<LayerValidationRule> getRulesForType(String validationType) {
        ensureCacheLoaded();
        return VALIDATION_RULES_CACHE.getOrDefault(validationType, Collections.emptyList());
    }
    
    /**
     * Fuerza recarga del caché (útil para testing o cambios urgentes)
     */
    public static void forceReload() {
        logger.info("Forzando recarga de reglas de validación...");
        lastCacheRefresh = 0;
        ensureCacheLoaded();
    }
    
    /**
     * Obtiene nombres de tablas configuradas para un tipo de validación
     */
    public static List<String> getLayerTableNames(String validationType) {
        ensureCacheLoaded();
        List<String> tableNames = new ArrayList<>();
        List<LayerValidationRule> rules = VALIDATION_RULES_CACHE.get(validationType);
        if (rules != null) {
            for (LayerValidationRule rule : rules) {
                tableNames.add(rule.getLayerTableName());
            }
        }
        return tableNames;
    }
    
    /**
     * Valida si una intersección cumple con las reglas (LEGACY - mantenido para compatibilidad)
     */
    public static boolean isValidIntersection(LayerValidationRule rule, double intersectionAreaM2, double predioTotalAreaM2) {
        // Si el área de intersección es menor al mínimo, se considera sin intersección significativa
        if (intersectionAreaM2 < rule.getMinIntersectionAreaM2()) {
            return true; // No hay intersección significativa
        }

        // Calcular porcentaje de intersección
        double intersectionPercentage = (intersectionAreaM2 / predioTotalAreaM2) * 100.0;

        // Validar contra el máximo permitido
        return intersectionPercentage <= rule.getMaxIntersectionPercentage();
    }
    
    /**
     * NUEVO: Valida una intersección usando umbrales escalonados por tamaño de predio.
     * 
     * @param rule Regla de validación con umbrales configurados
     * @param intersectionAreaM2 Área de intersección en metros cuadrados
     * @param predioTotalAreaM2 Área total del predio en metros cuadrados
     * @return ValidationResult con el resultado y mensaje apropiado
     */
    public static ValidationResult validateIntersection(LayerValidationRule rule, 
                                                       double intersectionAreaM2, 
                                                       double predioTotalAreaM2) {
        // 1. Si no hay intersección significativa
        if (intersectionAreaM2 < rule.getMinIntersectionAreaM2()) {
            String message = rule.getMessageApproved() != null 
                ? rule.getMessageApproved() 
                : "Intersección insignificante (< " + rule.getMinIntersectionAreaM2() + " m²). APROBADO";
            return new ValidationResult(true, message, 0.0, 0.0, "Área insignificante");
        }
        
        // 2. Calcular tamaño del predio en hectáreas
        double predioHectares = predioTotalAreaM2 / 10000.0;
        
        // 3. Calcular porcentaje de intersección
        double percentage = (intersectionAreaM2 / predioTotalAreaM2) * 100.0;
        
        // 4. Determinar si usar umbrales escalonados o regla legacy
        if (rule.hasThresholds()) {
            // NUEVO: Sistema de umbrales escalonados
            ThresholdBySize applicableThreshold = rule.findApplicableThreshold(predioHectares);
            
            if (applicableThreshold == null) {
                logger.warning("No se encontró umbral aplicable para " + predioHectares + " ha. Usando legacy.");
                return validateLegacy(rule, percentage);
            }
            
            boolean passes = percentage <= applicableThreshold.getMaxPercentage();
            String rangeDesc = formatThresholdRange(applicableThreshold);
            
            String message;
            if (passes) {
                message = rule.getMessageApproved() != null 
                    ? rule.getMessageApproved() 
                    : String.format("Intersección dentro del rango permitido (%.2f%% <= %.1f%%) para predios %s. APROBADO", 
                        percentage, applicableThreshold.getMaxPercentage(), rangeDesc);
            } else {
                message = rule.getMessageRejected() != null 
                    ? rule.getMessageRejected() 
                    : String.format("RECHAZADO: Intersección detectada %.2f%% supera el máximo %.1f%% para predios %s", 
                        percentage, applicableThreshold.getMaxPercentage(), rangeDesc);
            }
            
            return new ValidationResult(passes, message, applicableThreshold.getMaxPercentage(), 
                                      percentage, rangeDesc);
        } else {
            // LEGACY: Sistema de porcentaje fijo
            return validateLegacy(rule, percentage);
        }
    }
    
    /**
     * Validación legacy usando porcentaje fijo (para reglas sin umbrales)
     */
    private static ValidationResult validateLegacy(LayerValidationRule rule, double percentage) {
        boolean passes = percentage <= rule.getMaxIntersectionPercentage();
        
        String message;
        if (passes) {
            message = String.format("Intersección dentro del rango permitido (%.2f%% <= %.1f%%). APROBADO", 
                percentage, rule.getMaxIntersectionPercentage());
        } else {
            message = rule.getValidationMessage() != null 
                ? "RECHAZADO: " + rule.getValidationMessage() + ". Intersección detectada: " + 
                  String.format("%.2f%%", percentage) + " (Máximo: " + rule.getMaxIntersectionPercentage() + "%)"
                : String.format("RECHAZADO: Intersección %.2f%% supera el máximo %.1f%%", 
                    percentage, rule.getMaxIntersectionPercentage());
        }
        
        return new ValidationResult(passes, message, rule.getMaxIntersectionPercentage(), 
                                  percentage, "Sistema fijo");
    }
    
    /**
     * Formatea el rango de tamaño de un umbral para mostrar al usuario
     */
    private static String formatThresholdRange(ThresholdBySize threshold) {
        if (threshold.getMaxHectares() == null) {
            return String.format(">%.0f ha", threshold.getMinHectares());
        } else if (threshold.getMinHectares() == 0) {
            return String.format("≤%.0f ha", threshold.getMaxHectares());
        } else {
            return String.format("%.0f-%.0f ha", threshold.getMinHectares(), threshold.getMaxHectares());
        }
    }
    
    /**
     * Obtiene conexión JDBC directa a saf_interconexion
     * Lee configuración desde archivo externo o variables de entorno
     */
    private static Connection getJDBCConnection() throws Exception {
        // Leer desde archivo de configuración externo o variables de entorno
        String url = getConfigValue("DB_CONFIG_URL", "jdbc:postgresql://localhost:5432/saf_interconexion");
        String user = getConfigValue("DB_CONFIG_USERNAME", "saf_app");
        String password = getConfigValue("DB_CONFIG_PASSWORD", "saf_app_2026");
        
        return DriverManager.getConnection(url, user, password);
    }
    
    /**
     * Obtiene valor de configuración con prioridad:
     * 1. Variable de entorno
     * 2. Propiedad del sistema (-D)
     * 3. Valor por defecto
     */
    private static String getConfigValue(String key, String defaultValue) {
        // 1. Variable de entorno
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        
        // 2. Propiedad del sistema (ej: -DDB_CONFIG_URL=...)
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.isEmpty()) {
            return sysProp;
        }
        
        // 3. Valor por defecto
        return defaultValue;
    }
}
