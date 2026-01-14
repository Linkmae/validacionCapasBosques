package com.saf.verification;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.sql.DataSource;
import javax.xml.ws.WebServiceException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Servicio SOAP para verificación de intersecciones de predios con capas forestales.
 */
@Stateless
@WebService(serviceName = "VerificationService", targetNamespace = "http://saf.com/verification")
public class VerificationService {

    @Resource(lookup = "java:jboss/datasources/SAFLogsDS")
    private DataSource logsDS;

    @Resource(lookup = "java:jboss/datasources/SAFCapasDS")
    private DataSource capasDS;

    private ConfigManager configManager;
    private PrediosClient prediosClient;
    private DatabaseManager dbManager;

    public VerificationService() {
        // Inicialización diferida
        this.configManager = new ConfigManager();
    }

    private void initializeIfNeeded() {
        if (this.prediosClient == null) {
            this.dbManager = new DatabaseManager(logsDS, capasDS);
            
            // Leer configuración del servicio de predios desde BD
            String prediosUrl = dbManager.getConfigValue("predios_service_url");
            String prediosUsuario = dbManager.getConfigValue("predios_service_usuario");
            String prediosClave = dbManager.getConfigValue("predios_service_clave");
            
            // Valores por defecto si no están en BD
            if (prediosUrl == null) {
                prediosUrl = "http://localhost:8080/servicio-soap-predios/PrediosService?wsdl";
            }
            if (prediosUsuario == null) {
                prediosUsuario = "1750702068";
            }
            if (prediosClave == null) {
                prediosClave = "1234";
            }
            
            this.prediosClient = new PrediosClient(prediosUrl, prediosUsuario, prediosClave);
            System.out.println("PrediosClient inicializado: URL=" + prediosUrl + ", Usuario=" + prediosUsuario);
        }
    }

    @WebMethod(operationName = "verifyPrediosByIdentifier")
    @WebResult(name = "verificationResponse")
    public VerifyPrediosByIdentifierResponse verifyPrediosByIdentifier(
            @WebParam(name = "request") VerifyPrediosByIdentifierRequest request) {

        VerifyPrediosByIdentifierResponse response = new VerifyPrediosByIdentifierResponse();
        String requestId = generateRequestId();
        
        try {
            System.out.println("[" + requestId + "] Iniciando verificación para: " + request.getIdentifierValue());
            
            // Validar request
            if (request == null || request.getIdentifierValue() == null || request.getIdentifierValue().isEmpty()) {
                System.err.println("[" + requestId + "] ERROR: Request inválido o identifierValue vacío");
                response.setRequestStatus(new RequestStatus("400", "ERROR_VALIDACION", "El identificador es requerido"));
                return response;
            }

            // Inicializar servicios con manejo de errores
            try {
                initializeIfNeeded();
            } catch (Exception e) {
                System.err.println("[" + requestId + "] ERROR: Fallo al inicializar servicios - " + e.getMessage());
                response.setRequestStatus(new RequestStatus("500", "ERROR_INICIALIZACION", "Error al inicializar componentes internos"));
                logErrorSafe(requestId, "INIT_ERROR", e);
                return response;
            }

            // Obtener predios del servicio externo con manejo de errores
            GetPrediosResponse prediosResponse = null;
            try {
                System.out.println("[" + requestId + "] Consultando servicio externo de predios...");
                prediosResponse = prediosClient.getPredios(request.getIdentifierType(), request.getIdentifierValue());
                System.out.println("[" + requestId + "] Servicio externo respondió con " + 
                    (prediosResponse.getPredios() != null ? prediosResponse.getPredios().size() : 0) + " predios");
            } catch (Exception e) {
                System.err.println("[" + requestId + "] ERROR: Fallo al conectar con servicio externo - " + e.getMessage());
                e.printStackTrace();
                response.setRequestStatus(new RequestStatus("503", "ERROR_SERVICIO_EXTERNO", 
                    "No se pudo conectar con el servicio de predios. Verifique que esté disponible."));
                logErrorSafe(requestId, "EXTERNAL_SERVICE_ERROR", e);
                return response;
            }

            // Validar respuesta del servicio externo
            if (prediosResponse == null || prediosResponse.getPredios() == null || prediosResponse.getPredios().isEmpty()) {
                System.out.println("[" + requestId + "] INFO: No se encontraron predios para el identificador");
                response.setRequestStatus(new RequestStatus("404", "NO_ENCONTRADO", "No se encontraron predios para el identificador proporcionado"));
                return response;
            }

            // Procesar cada predio con manejo de errores individuales
            List<PredioVerification> verifications = new ArrayList<>();
            int prediosExitosos = 0;
            int prediosFallidos = 0;
            
            for (Predio predio : prediosResponse.getPredios()) {
                try {
                    System.out.println("[" + requestId + "] Procesando predio: " + predio.getPredioId());
                    PredioVerification verification = processPredio(predio, request.getVerificationType(), request.getLayersToCheck());
                    verifications.add(verification);
                    prediosExitosos++;
                    
                    // Loggear detalles del predio
                    try {
                        dbManager.logPredioDetails(requestId, verification);
                    } catch (Exception logEx) {
                        System.err.println("[" + requestId + "] ADVERTENCIA: No se pudo guardar detalles del predio - " + logEx.getMessage());
                    }
                } catch (Exception e) {
                    System.err.println("[" + requestId + "] ERROR: Fallo al procesar predio " + predio.getPredioId() + " - " + e.getMessage());
                    prediosFallidos++;
                    // Continuar con el siguiente predio
                }
            }

            System.out.println("[" + requestId + "] Procesamiento completado: " + prediosExitosos + " exitosos, " + prediosFallidos + " fallidos");

            // Crear respuesta exitosa
            response.setRequestStatus(new RequestStatus("0", "OK", "Verificación completada exitosamente"));
            response.setIdentifierEcho(request.getIdentifierType() + ":" + request.getIdentifierValue());
            response.setPredioVerifications(verifications);
            response.setSummary(createSummary(verifications));

            // Loggear en BD con manejo de errores
            try {
                dbManager.logRequest(request, response);
            } catch (Exception e) {
                System.err.println("[" + requestId + "] ADVERTENCIA: No se pudo guardar log en BD - " + e.getMessage());
                // No fallar la operación por error de logging
            }

            return response;

        } catch (Exception e) {
            // Captura global de errores no manejados
            System.err.println("[" + requestId + "] ERROR CRÍTICO no manejado: " + e.getMessage());
            e.printStackTrace();
            response.setRequestStatus(new RequestStatus("500", "ERROR_INTERNO", "Error interno del servidor: " + e.getMessage()));
            logErrorSafe(requestId, "CRITICAL_ERROR", e);
            return response;
        }
    }
    
    private String generateRequestId() {
        return "REQ-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
    
    private void logErrorSafe(String requestId, String errorType, Exception e) {
        try {
            if (dbManager != null) {
                dbManager.logError(requestId, errorType, e);
            }
        } catch (Exception logEx) {
            System.err.println("[" + requestId + "] ADVERTENCIA: No se pudo registrar error en BD - " + logEx.getMessage());
        }
    }

    private PredioVerification processPredio(Predio predio, String verificationType, List<String> layersToCheck) {
        // Determinar tipo de validación (por defecto AREAS_CONSERVACION si no se especifica)
        String validationType = (verificationType != null && !verificationType.isEmpty()) 
            ? verificationType.toUpperCase() 
            : "AREAS_CONSERVACION";

        System.out.println("Procesando predio " + predio.getPredioId() + " con tipo de validación: " + validationType);

        // Obtener reglas de validación según el tipo
        List<LayerValidationRule> rules = LayerValidationConfig.getRulesForType(validationType);
        
        if (rules.isEmpty()) {
            System.err.println("ADVERTENCIA: No hay reglas configuradas para tipo: " + validationType);
        }

        // Filtrar reglas si se especificaron capas específicas
        List<LayerValidationRule> rulesToApply = rules;
        if (layersToCheck != null && !layersToCheck.isEmpty()) {
            rulesToApply = new ArrayList<>();
            for (LayerValidationRule rule : rules) {
                if (layersToCheck.contains(rule.getLayerTableName()) || 
                    layersToCheck.contains(rule.getLayerName())) {
                    rulesToApply.add(rule);
                }
            }
            System.out.println("Aplicando " + rulesToApply.size() + " reglas filtradas de " + rules.size() + " disponibles");
        }

        // Calcular intersecciones con validación
        List<LayerResult> results = new ArrayList<>();
        for (LayerValidationRule rule : rulesToApply) {
            if (!rule.isActive()) {
                System.out.println("Saltando capa inactiva: " + rule.getLayerName());
                continue;
            }
            
            System.out.println("Verificando intersección con capa: " + rule.getLayerName() + " (tabla: " + rule.getLayerTableName() + ")");
            LayerResult result = calculateIntersectionWithValidation(predio, rule);
            results.add(result);
        }

        PredioVerification verification = new PredioVerification();
        verification.setPredioId(predio.getPredioId());
        verification.setPredioCodigo(predio.getPredioCodigo());
        verification.setPredioOwnerCedula(predio.getIdentifier());
        verification.setPredioOwnerName(predio.getOwnerName());
        verification.setPredioAreaM2(predio.getAreaM2());
        verification.setPredioSRID(predio.getSRID());
        verification.setPredioGeometryGeoJSON(predio.getGeometryGeoJSON());
        verification.setLayersResults(results);

        return verification;
    }

    /**
     * Calcula intersección y aplica reglas de validación
     */
    private LayerResult calculateIntersectionWithValidation(Predio predio, LayerValidationRule rule) {
        LayerResult result = new LayerResult();
        result.setLayerId(rule.getLayerTableName());
        result.setLayerName(rule.getLayerName());
        result.setWmsLayerName(getWmsLayerName(rule.getLayerTableName()));
        result.setMaxAllowedPercentage(rule.getMaxIntersectionPercentage());

        try {
            // Calcular intersección usando PostGIS
            Map<String, Object> interResult = dbManager.calculateIntersection(
                predio.getGeometryWKT(), 
                rule.getLayerTableName()
            );

            // Verificar si la tabla no existe
            if (interResult != null && interResult.containsKey("table_not_found") && 
                (Boolean) interResult.get("table_not_found")) {
                result.setLayerNotLoaded(true);
                result.setIntersects(false);
                result.setIntersectionAreaM2(0);
                result.setPercentage(0);
                result.setValidationPassed(false);
                result.setValidationMessage("ADVERTENCIA: La capa '" + rule.getLayerName() + 
                    "' no ha sido cargada en PostGIS. No se pudo realizar la validación.");
                System.out.println("  → Capa no cargada en base de datos");
                return result;
            }

            if (interResult != null && (Boolean) interResult.get("intersects")) {
                result.setIntersects(true);
                double interArea = (Double) interResult.get("area_m2");
                result.setIntersectionAreaM2(interArea);
                
                // Calcular porcentaje
                double percentage = (interArea / predio.getAreaM2()) * 100.0;
                result.setPercentage(percentage);
                
                // NUEVO: Usar validateIntersection() que soporta umbrales escalonados
                ValidationResult validation = LayerValidationConfig.validateIntersection(
                    rule, interArea, predio.getAreaM2());
                
                result.setValidationPassed(validation.isPassed());
                result.setValidationMessage(validation.getMessage());
                
                if (validation.getAppliedThreshold() != null) {
                    result.setMaxAllowedPercentage(validation.getAppliedThreshold());
                } else {
                    result.setMaxAllowedPercentage(rule.getMaxIntersectionPercentage());
                }
                
                // GeoJSON solo si está habilitado
                result.setIntersectionGeoJSON((String) interResult.get("geojson"));
                
                System.out.println("  → Intersección: " + String.format("%.2f", interArea) + " m² (" + 
                    String.format("%.2f", percentage) + "%) - Validación: " + 
                    (validation.isPassed() ? "PASA" : "FALLA") + 
                    " [Umbral: " + validation.getThresholdRangeDescription() + "]");
            } else {
                // Sin intersección
                result.setIntersects(false);
                result.setIntersectionAreaM2(0);
                result.setPercentage(0);
                result.setValidationPassed(true); // Sin intersección = válido
                
                // Usar mensaje aprobado si está configurado
                String approvedMessage = rule.getMessageApproved();
                if (approvedMessage != null && !approvedMessage.isEmpty()) {
                    result.setValidationMessage(approvedMessage);
                } else {
                    result.setValidationMessage("Sin intersección detectada. APROBADO");
                }
                
                System.out.println("  → Sin intersección");
            }
            
        } catch (Exception e) {
            System.err.println("ERROR calculando intersección para capa " + rule.getLayerName() + ": " + e.getMessage());
            e.printStackTrace();
            
            // En caso de error, marcar como no validado
            result.setIntersects(false);
            result.setIntersectionAreaM2(0);
            result.setPercentage(0);
            result.setValidationPassed(false);
            result.setValidationMessage("ERROR al calcular intersección: " + e.getMessage());
        }

        return result;
    }

    private String getLayerName(String capa) {
        // Mapear nombres legibles
        switch (capa) {
            case "h_demarcacion.fa210_snap_a": return "SNAP";
            case "public.hc00_bvp_a_07082019": return "Bosques y Vegetación Protectora";
            // ... otros
            default: return capa;
        }
    }

    private String getWmsLayerName(String capa) {
        // Mapear a nombres WMS
        switch (capa) {
            case "h_demarcacion.fa210_snap_a": return "saf:car_forestal"; // Ajustar según corresponda
            // ... otros
            default: return capa;
        }
    }

    private Summary createSummary(List<PredioVerification> verifications) {
        // Calcular totales
        return new Summary(); // Placeholder
    }
}