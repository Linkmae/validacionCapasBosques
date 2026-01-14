package com.saf.verification;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Gestiona operaciones de BD para logs y actualizaciones.
 */
public class DatabaseManager {

    private static final Logger log = Logger.getLogger(DatabaseManager.class.getName());

    private DataSource logsDS;
    private DataSource capasDS;

    public DatabaseManager(DataSource logsDS, DataSource capasDS) {
        this.logsDS = logsDS;
        this.capasDS = capasDS;
    }

    public void logRequest(VerifyPrediosByIdentifierRequest request, VerifyPrediosByIdentifierResponse response) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = logsDS.getConnection();
            
            // Calcular métricas
            int totalPredios = response.getPredioVerifications() != null ? response.getPredioVerifications().size() : 0;
            int totalLayers = 0;
            int layersNotLoaded = 0;
            int layersWithIntersection = 0;
            
            if (response.getPredioVerifications() != null) {
                for (PredioVerification pv : response.getPredioVerifications()) {
                    if (pv.getLayersResults() != null) {
                        totalLayers += pv.getLayersResults().size();
                        for (LayerResult lr : pv.getLayersResults()) {
                            if (lr.isLayerNotLoaded()) layersNotLoaded++;
                            if (lr.isIntersects()) layersWithIntersection++;
                        }
                    }
                }
            }
            
            String sql = "INSERT INTO saf_request_logs " +
                        "(request_id, identifier_type, identifier_value, verification_type, " +
                        "status_code, error_type, status_message, total_predios, " +
                        "predios_procesados, predios_exitosos, total_layers_checked, " +
                        "layers_not_loaded, layers_with_intersection, response_timestamp) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
            
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, response.getIdentifierEcho());
            stmt.setString(2, request.getIdentifierType());
            stmt.setString(3, request.getIdentifierValue());
            stmt.setString(4, request.getVerificationType());
            stmt.setString(5, response.getRequestStatus().getCode());
            stmt.setString(6, response.getRequestStatus().getErrorType());
            stmt.setString(7, response.getRequestStatus().getMessage());
            stmt.setInt(8, totalPredios);
            stmt.setInt(9, totalPredios);
            stmt.setInt(10, totalPredios);
            stmt.setInt(11, totalLayers);
            stmt.setInt(12, layersNotLoaded);
            stmt.setInt(13, layersWithIntersection);
            
            stmt.executeUpdate();
            
            log.info("Request logged: " + response.getIdentifierEcho() + 
                    " - Predios: " + totalPredios + ", Layers: " + totalLayers);

        } catch (Exception e) {
            log.severe("ERROR: No se pudo guardar log de request - " + e.getMessage());
            // No lanzar excepción para no interrumpir el flujo
        } finally {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    public void logPredioDetails(String requestId, PredioVerification predio) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = logsDS.getConnection();
            
            String sql = "INSERT INTO saf_predio_logs " +
                        "(request_id, predio_id, predio_codigo, owner_cedula, owner_name, predio_area_m2, " +
                        "layer_name, layer_table_name, layer_not_loaded, intersects, " +
                        "intersection_area_m2, intersection_percentage, validation_passed, validation_message) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            stmt = conn.prepareStatement(sql);
            
            if (predio.getLayersResults() != null) {
                for (LayerResult layer : predio.getLayersResults()) {
                    stmt.setString(1, requestId);
                    stmt.setString(2, predio.getPredioId());
                    stmt.setString(3, predio.getPredioCodigo());
                    stmt.setString(4, predio.getPredioOwnerCedula());
                    stmt.setString(5, predio.getPredioOwnerName());
                    stmt.setDouble(6, predio.getPredioAreaM2());
                    stmt.setString(7, layer.getLayerName());
                    stmt.setString(8, layer.getLayerId());
                    stmt.setBoolean(9, layer.isLayerNotLoaded());
                    stmt.setBoolean(10, layer.isIntersects());
                    stmt.setDouble(11, layer.getIntersectionAreaM2());
                    stmt.setDouble(12, layer.getPercentage());
                    stmt.setBoolean(13, layer.isValidationPassed());
                    stmt.setString(14, layer.getValidationMessage());
                    
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
            log.info("Predio details logged: " + predio.getPredioId() + 
                    " (" + (predio.getLayersResults() != null ? predio.getLayersResults().size() : 0) + " layers)");

        } catch (Exception e) {
            log.severe("ERROR: No se pudo guardar detalles del predio - " + e.getMessage());
        } finally {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    public void logError(String requestId, String errorType, Exception e) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = logsDS.getConnection();
            String sql = "INSERT INTO saf_error_logs (request_id, error_type, error_message, stack_trace, timestamp) " +
                        "VALUES (?, ?, ?, ?, NOW())";
            stmt = conn.prepareStatement(sql);
            
            stmt.setString(1, requestId);
            stmt.setString(2, errorType);
            stmt.setString(3, e.getMessage());
            stmt.setString(4, getStackTraceString(e));
            stmt.executeUpdate();
            
            log.info("Error logged successfully: " + requestId);

        } catch (Exception logEx) {
            log.severe("ERROR: No se pudo guardar log de error - " + logEx.getMessage());
            // No lanzar excepción
        } finally {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }
    
    private String getStackTraceString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        if (sb.length() > 4000) {
            return sb.substring(0, 4000); // Limitar tamaño
        }
        return sb.toString();
    }
    
    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Ignorar
            }
        }
    }

    public String getConfigValue(String key) {
        String sql = "SELECT parameter_value FROM config_parameters WHERE parameter_key = ? AND is_active = true";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = logsDS.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, key);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String value = rs.getString("parameter_value");
                log.info("Config loaded: " + key + " = " + value);
                return value;
            } else {
                log.warning("Config key not found: " + key);
            }
        } catch (SQLException e) {
            log.severe("ERROR al leer configuración '" + key + "': " + e.getMessage());
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            closeQuietly(conn);
        }
        return null;
    }

    public Map<String, Object> calculateIntersection(String predioWkt, String capaSchemaTabla) {
        String query = "SELECT " +
            "CASE WHEN ST_Area(intersection_geom) > 0 THEN true ELSE false END AS intersects, " +
            "ST_Area(ST_Transform(intersection_geom, 4326)::geography) AS area_m2, " +
            "ST_AsGeoJSON(ST_Transform(intersection_geom, 4326)) AS geojson " +
            "FROM ( " +
            "    SELECT ST_Union(ST_Intersection(ST_GeomFromText(?, 4326), geom)) AS intersection_geom " +
            "    FROM " + capaSchemaTabla + " " +
            "    WHERE ST_Intersects(ST_GeomFromText(?, 4326), geom) " +
            ") AS subquery " +
            "WHERE intersection_geom IS NOT NULL";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = capasDS.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, predioWkt);
            stmt.setString(2, predioWkt);

            rs = stmt.executeQuery();
            if (rs.next()) {
                Map<String, Object> result = new HashMap<>();
                result.put("intersects", rs.getBoolean("intersects"));
                result.put("area_m2", rs.getDouble("area_m2"));
                result.put("geojson", rs.getString("geojson"));
                return result;
            }

        } catch (Exception e) {
            log.severe("ERROR calculando intersección con " + capaSchemaTabla + ": " + e.getMessage());
            
            // Si la tabla no existe, retornar un resultado especial
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                Map<String, Object> result = new HashMap<>();
                result.put("table_not_found", true);
                result.put("intersects", false);
                result.put("area_m2", 0.0);
                result.put("geojson", null);
                return result;
            }
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            closeQuietly(conn);
        }
        return null;
    }
}