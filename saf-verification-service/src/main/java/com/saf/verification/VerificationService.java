package com.saf.verification;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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

    @WebMethod(operationName = "verifyPrediosByIdentifierFlat")
    @WebResult(name = "flatVerificationResponse")
    public FlatVerificationResponse verifyPrediosByIdentifierFlat(
            @WebParam(name = "request") VerifyPrediosByIdentifierRequest request) {
        
        // Forzar formato FLAT
        request.setOutputFormat("FLAT");
        
        // Llamar al servicio principal
        VerifyPrediosByIdentifierResponse masterDetailResponse = verifyPrediosByIdentifier(request);
        
        // Convertir a formato plano
        return convertToFlatResponse(masterDetailResponse, request);
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
            } catch (Throwable e) {
                System.err.println("[" + requestId + "] ERROR: Fallo al inicializar servicios - " + e.getMessage());
                response.setRequestStatus(new RequestStatus("500", "ERROR_INICIALIZACION", "Error al inicializar componentes internos"));
                logErrorSafe(requestId, "INIT_ERROR", new Exception(e));
                return response;
            }

            // ===== NUEVA LÓGICA: Verificar modo de operación =====
            List<Predio> prediosToProcess = new ArrayList<>();
            
            if (request.hasDirectPredioData()) {
                // MODO DIRECTO: Usar información de predios proporcionada
                System.out.println("[" + requestId + "] 🔵 Modo DIRECTO: Procesando " + 
                    request.getPrediosData().size() + " predios con información proporcionada");
                
                prediosToProcess = processPrediosFromDirectData(request, requestId);
                
                if (prediosToProcess.isEmpty()) {
                    System.err.println("[" + requestId + "] ERROR: No hay predios válidos en la información directa");
                    response.setRequestStatus(new RequestStatus("400", "ERROR_VALIDACION", 
                        "La información de predios proporcionada es inválida o está vacía"));
                    return response;
                }
            } else {
                // MODO EXTERNO: Consultar servicio externo de predios (comportamiento original)
                System.out.println("[" + requestId + "] 🔵 Modo EXTERNO: Consultando servicio de predios para identificador: " + 
                    request.getIdentifierValue());
                
                GetPrediosResponse prediosResponse = null;
                try {
                    prediosResponse = prediosClient.getPredios(request.getIdentifierType(), request.getIdentifierValue());
                    System.out.println("[" + requestId + "] Servicio externo respondió con " + 
                        (prediosResponse.getPredios() != null ? prediosResponse.getPredios().size() : 0) + " predios");
                } catch (Throwable e) {
                    System.err.println("[" + requestId + "] ERROR: Fallo al conectar con servicio externo - " + e.getMessage());
                    e.printStackTrace();
                    response.setRequestStatus(new RequestStatus("503", "ERROR_SERVICIO_EXTERNO", 
                        "No se pudo conectar con el servicio de predios. Verifique que esté disponible."));
                    logErrorSafe(requestId, "EXTERNAL_SERVICE_ERROR", new Exception(e));
                    return response;
                }

                // Validar respuesta del servicio externo
                if (prediosResponse == null || prediosResponse.getPredios() == null || prediosResponse.getPredios().isEmpty()) {
                    System.out.println("[" + requestId + "] INFO: No se encontraron predios para el identificador");
                    response.setRequestStatus(new RequestStatus("404", "NO_ENCONTRADO", 
                        "No se encontraron predios para el identificador proporcionado"));
                    return response;
                }
                
                prediosToProcess = prediosResponse.getPredios();
            }

            // Procesar cada predio con manejo de errores individuales
            List<PredioVerification> verifications = new ArrayList<>();
            int prediosExitosos = 0;
            int prediosFallidos = 0;
            
            for (Predio predio : prediosToProcess) {
                try {
                    System.out.println("[" + requestId + "] Procesando predio: " + predio.getPredioId());
                    PredioVerification verification = processPredio(
                        predio,
                        request.getVerificationType(),
                        request.getLayersToCheck(),
                        request.isIncludeIntersectionGeoJSON());
                    verifications.add(verification);
                    prediosExitosos++;
                    
                    // Loggear detalles del predio
                    try {
                        dbManager.logPredioDetails(requestId, verification);
                    } catch (Exception logEx) {
                        System.err.println("[" + requestId + "] ADVERTENCIA: No se pudo guardar detalles del predio - " + logEx.getMessage());
                    }
                } catch (Throwable e) {
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
            response.setOutputFormat(request.getOutputFormatOrDefault());

            // Compatibilidad: si solicitan FLAT en la operación estándar,
            // incluir también los registros planos en el mismo response.
            if (request.isFlatFormat()) {
                FlatVerificationResponse flatResponse = convertToFlatResponse(response, request);
                response.setValidations(flatResponse.getValidations());
                response.setTotalRecords(flatResponse.getTotalRecords());
                // Evitar payload duplicado cuando se solicita formato plano
                response.setPredioVerifications(null);
            }

            // Loggear en BD con manejo de errores
            try {
                dbManager.logRequest(request, response);
            } catch (Exception e) {
                System.err.println("[" + requestId + "] ADVERTENCIA: No se pudo guardar log en BD - " + e.getMessage());
                // No fallar la operación por error de logging
            }

            return response;

        } catch (Throwable e) {
            // Captura global de errores no manejados
            System.err.println("[" + requestId + "] ERROR CRÍTICO no manejado: " + e.getMessage());
            e.printStackTrace();
            response.setRequestStatus(new RequestStatus("500", "ERROR_INTERNO", "Error interno del servidor: " + e.getMessage()));
            logErrorSafe(requestId, "CRITICAL_ERROR", new Exception(e));
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

        private PredioVerification processPredio(
            Predio predio,
            String verificationType,
            List<String> layersToCheck,
            boolean includeIntersectionGeoJSON) {
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
        // IMPORTANTE: Solo filtrar si layersToCheck tiene elementos NO VACÍOS
        List<LayerValidationRule> rulesToApply = rules;
        boolean hasValidLayersFilter = false;
        if (layersToCheck != null && !layersToCheck.isEmpty()) {
            // Verificar que al menos un elemento no sea nulo o vacío
            for (String layerToCheck : layersToCheck) {
                if (layerToCheck != null && !layerToCheck.trim().isEmpty()) {
                    hasValidLayersFilter = true;
                    break;
                }
            }
        }
        
        if (hasValidLayersFilter) {
            rulesToApply = new ArrayList<>();
            for (LayerValidationRule rule : rules) {
                for (String layerToCheck : layersToCheck) {
                    if (layerToCheck != null && !layerToCheck.trim().isEmpty() &&
                        (layerToCheck.equals(rule.getLayerTableName()) || 
                         layerToCheck.equals(rule.getLayerName()))) {
                        rulesToApply.add(rule);
                        break; // No agregar la misma regla dos veces
                    }
                }
            }
            System.out.println("Aplicando " + rulesToApply.size() + " reglas filtradas de " + rules.size() + " disponibles");
        }

        // Normalizar área del predio usando cálculo PostGIS (m²) para evitar inconsistencias
        normalizePredioAreaUsingPostGIS(predio);

        // Calcular intersecciones con validación
        List<LayerResult> results = new ArrayList<>();
        for (LayerValidationRule rule : rulesToApply) {
            if (!rule.isActive()) {
                System.out.println("Saltando capa inactiva: " + rule.getLayerName());
                continue;
            }
            
            System.out.println("Verificando intersección con capa: " + rule.getLayerName() + " (tabla: " + rule.getLayerTableName() + ")");
            LayerResult result = calculateIntersectionWithValidation(predio, rule, includeIntersectionGeoJSON);
            results.add(result);
        }

        PredioVerification verification = new PredioVerification();
        verification.setPredioId(predio.getPredioId());
        verification.setPredioCodigo(predio.getPredioCodigo());
        verification.setPredioOwnerCedula(predio.getIdentifier());
        verification.setPredioOwnerName(predio.getOwnerName());
        verification.setPredioAreaM2(predio.getAreaM2());
        verification.setPredioAreaHa(predio.getAreaM2() / 10000.0);
        verification.setPredioAreaReportedM2(predio.getReportedAreaM2());
        verification.setPredioAreaReportedHa(predio.getReportedAreaM2() != null ? predio.getReportedAreaM2() / 10000.0 : null);
        verification.setAreaConsistencyWarning(predio.getAreaConsistencyWarning());
        verification.setPredioSRID(predio.getSRID());
        verification.setPredioGeometryGeoJSON(predio.getGeometryGeoJSON());
        verification.setLayersResults(results);

        return verification;
    }

    /**
     * Calcula intersección y aplica reglas de validación
     */
        private LayerResult calculateIntersectionWithValidation(
            Predio predio,
            LayerValidationRule rule,
            boolean includeIntersectionGeoJSON) {
        LayerResult result = new LayerResult();
        result.setLayerId(rule.getLayerTableName());
        result.setLayerName(rule.getLayerName());
        result.setWmsLayerName(getWmsLayerName(rule.getLayerTableName()));
        result.setMaxAllowedPercentage(rule.getMaxIntersectionPercentage());

        try {
            String schemaAndTable = buildQualifiedLayerTableName(rule);

            // Calcular intersección usando PostGIS
            Map<String, Object> interResult = dbManager.calculateIntersection(
                predio.getGeometryWKT(), 
                schemaAndTable
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

                double predioAreaM2 = predio.getAreaM2();
                if (predioAreaM2 <= 0) {
                    result.setPercentage(0);
                    result.setValidationPassed(false);
                    result.setValidationMessage("ERROR: Área del predio inválida (<= 0 m²). No se puede calcular porcentaje de intersección.");
                    System.err.println("  → ERROR: Área del predio inválida para cálculo de intersección. Predio=" + predio.getPredioId());
                    return result;
                }

                // Unificar unidades en hectáreas para cálculo y logging (1 Ha = 10,000 m²)
                double interAreaHa = interArea / 10000.0;
                double predioAreaHa = predioAreaM2 / 10000.0;
                
                // Calcular porcentaje
                double percentage = (interAreaHa / predioAreaHa) * 100.0;
                result.setPercentage(percentage);
                
                // NUEVO: Usar validateIntersection() que soporta umbrales escalonados
                ValidationResult validation = LayerValidationConfig.validateIntersection(
                    rule, interArea, predioAreaM2);
                
                result.setValidationPassed(validation.isPassed());
                result.setValidationMessage(validation.getMessage());
                
                if (validation.getAppliedThreshold() != null) {
                    result.setMaxAllowedPercentage(validation.getAppliedThreshold());
                } else {
                    result.setMaxAllowedPercentage(rule.getMaxIntersectionPercentage());
                }
                
                // GeoJSON solo si está habilitado
                if (includeIntersectionGeoJSON) {
                    result.setIntersectionGeoJSON((String) interResult.get("geojson"));
                }
                
                System.out.println("  → Intersección: " + String.format("%.2f", interArea) + " m² (" + 
                    String.format("%.4f", interAreaHa) + " Ha) / Predio: " +
                    String.format("%.2f", predioAreaM2) + " m² (" +
                    String.format("%.4f", predioAreaHa) + " Ha) => " +
                    String.format("%.2f", percentage) + "% - Validación: " + 
                    (validation.isPassed() ? "PASA" : "FALLA") + 
                    " [Umbral: " + validation.getThresholdRangeDescription() + "]");
            } else {
                // Sin intersección
                result.setIntersects(false);
                result.setIntersectionAreaM2(0);
                result.setPercentage(0);
                result.setValidationPassed(true); // Sin intersección = válido

                double predioAreaM2 = predio.getAreaM2();
                double predioAreaHa = predioAreaM2 / 10000.0;
                
                // Usar mensaje aprobado si está configurado
                String approvedMessage = rule.getMessageApproved();
                if (approvedMessage != null && !approvedMessage.isEmpty()) {
                    result.setValidationMessage(approvedMessage);
                } else {
                    result.setValidationMessage("Sin intersección detectada. APROBADO");
                }
                
                System.out.println("  → Sin intersección (Predio: " +
                    String.format("%.2f", predioAreaM2) + " m² / " +
                    String.format("%.4f", predioAreaHa) + " Ha)");
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

    /**
     * Recalcula el área del predio con PostGIS y la normaliza en m².
     * Registra la diferencia frente al valor reportado por fuente externa.
     */
    private void normalizePredioAreaUsingPostGIS(Predio predio) {
        if (predio == null || predio.getGeometryWKT() == null || predio.getGeometryWKT().trim().isEmpty()) {
            return;
        }

        double reportedAreaM2 = predio.getAreaM2();
        predio.setReportedAreaM2(reportedAreaM2 > 0 ? reportedAreaM2 : null);
        predio.setAreaConsistencyWarning(null);
        try {
            double calculatedAreaM2 = calculateAreaFromWKT(predio.getGeometryWKT());
            if (calculatedAreaM2 <= 0) {
                System.err.println("ADVERTENCIA: Área calculada con PostGIS inválida (<= 0). Predio=" + predio.getPredioId());
                return;
            }

            predio.setAreaM2(calculatedAreaM2);

            double calculatedHa = calculatedAreaM2 / 10000.0;
            if (reportedAreaM2 > 0) {
                double reportedHa = reportedAreaM2 / 10000.0;
                double deltaM2 = calculatedAreaM2 - reportedAreaM2;
                double deltaHa = deltaM2 / 10000.0;
                double areaFactor = calculatedAreaM2 / reportedAreaM2;

                if (isSignificantAreaDifference(areaFactor)) {
                    predio.setAreaConsistencyWarning(
                        "ADVERTENCIA: El área reportada por la fuente externa difiere del área calculada con PostGIS. " +
                        "Reportada=" + String.format("%.2f", reportedAreaM2) + " m² (" + String.format("%.4f", reportedHa) + " Ha), " +
                        "Calculada=" + String.format("%.2f", calculatedAreaM2) + " m² (" + String.format("%.4f", calculatedHa) + " Ha), " +
                        "factor=" + String.format("%.2f", areaFactor) + "x. Para la validación se usa el área calculada con PostGIS."
                    );
                }

                System.out.println("Predio " + predio.getPredioId() +
                    " área normalizada con PostGIS. Reportada=" + String.format("%.2f", reportedAreaM2) + " m² (" +
                    String.format("%.4f", reportedHa) + " Ha), Calculada=" + String.format("%.2f", calculatedAreaM2) + " m² (" +
                    String.format("%.4f", calculatedHa) + " Ha), Δ=" + String.format("%.2f", deltaM2) + " m² (" +
                    String.format("%.4f", deltaHa) + " Ha), factor=" + String.format("%.2f", areaFactor) + "x respecto al área reportada");
            } else {
                System.out.println("Predio " + predio.getPredioId() +
                    " sin área reportada válida. Se usa área PostGIS=" + String.format("%.2f", calculatedAreaM2) + " m² (" +
                    String.format("%.4f", calculatedHa) + " Ha)");
            }
        } catch (Exception e) {
            System.err.println("ADVERTENCIA: No se pudo recalcular área con PostGIS para predio " +
                predio.getPredioId() + ". Se mantiene área reportada=" + reportedAreaM2 + " m². Error: " + e.getMessage());
        }
    }

    private boolean isSignificantAreaDifference(double areaFactor) {
        return areaFactor >= 1.05 || areaFactor <= 0.95;
    }

    /**
     * Construye nombre calificado de capa (schema.tabla) para consultas PostGIS.
     * Si la tabla ya viene calificada, se respeta tal cual.
     */
    private String buildQualifiedLayerTableName(LayerValidationRule rule) {
        String tableName = rule.getLayerTableName();
        String schemaName = rule.getSchemaName();

        if (tableName == null || tableName.trim().isEmpty()) {
            return tableName;
        }

        String normalizedTable = tableName.trim();
        if (normalizedTable.contains(".")) {
            return normalizedTable;
        }

        if (schemaName == null || schemaName.trim().isEmpty()) {
            return normalizedTable;
        }

        return schemaName.trim() + "." + normalizedTable;
    }

    /**
     * Procesa predios desde información directa proporcionada en el request
     * @param request Request con información de predios
     * @param requestId ID de la solicitud para logging
     * @return Lista de predios validados y listos para procesar
     */
    private List<Predio> processPrediosFromDirectData(VerifyPrediosByIdentifierRequest request, String requestId) {
        List<Predio> predios = new ArrayList<>();
        
        for (PredioInfo predioInfo : request.getPrediosData()) {
            try {
                // Validar información mínima
                if (!predioInfo.isValid()) {
                    System.err.println("[" + requestId + "] ADVERTENCIA: PredioInfo inválido - " + predioInfo.getPredioId() + 
                        " (falta predioId o geometryWKT)");
                    continue;
                }
                
                // Convertir PredioInfo a Predio
                Predio predio = predioInfo.toPredio();
                
                // Si no viene el área, intentar calcularla desde la geometría
                if (predioInfo.getAreaM2() == null || predioInfo.getAreaM2() == 0) {
                    try {
                        double calculatedArea = calculateAreaFromWKT(predio.getGeometryWKT());
                        predio.setAreaM2(calculatedArea);
                        double calculatedAreaHa = calculatedArea / 10000.0;
                        System.out.println("[" + requestId + "] Área calculada para predio " + predio.getPredioId() + 
                            ": " + String.format("%.2f", calculatedArea) + " m² (" +
                            String.format("%.4f", calculatedAreaHa) + " Ha)");
                    } catch (Exception e) {
                        System.err.println("[" + requestId + "] ERROR: No se pudo calcular área para predio " + 
                            predio.getPredioId() + " - " + e.getMessage());
                        // Continuar sin área
                        predio.setAreaM2(0);
                    }
                }
                
                predios.add(predio);
                System.out.println("[" + requestId + "] Predio cargado desde datos directos: " + predio.getPredioId() + 
                    " (" + String.format("%.2f", predio.getAreaM2()) + " m²)");
                
            } catch (Exception e) {
                System.err.println("[" + requestId + "] ERROR procesando PredioInfo " + 
                    (predioInfo != null ? predioInfo.getPredioId() : "null") + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return predios;
    }
    
    /**
     * Calcula el área de una geometría WKT en metros cuadrados
     * @param wkt Geometría en formato WKT (asume EPSG:4326)
     * @return Área en metros cuadrados
     */
    private double calculateAreaFromWKT(String wkt) throws Exception {
        if (wkt == null || wkt.trim().isEmpty()) {
            throw new IllegalArgumentException("WKT no puede estar vacío");
        }
        
        // Usar PostGIS para calcular el área
        // La geometría viene en WGS84 (4326), convertir a geography para cálculo preciso
        String sql = "SELECT ST_Area(ST_GeomFromText(?, 4326)::geography) AS area_m2";
        
        java.sql.Connection conn = null;
        java.sql.PreparedStatement stmt = null;
        java.sql.ResultSet rs = null;
        
        try {
            conn = capasDS.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, wkt);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("area_m2");
            } else {
                throw new Exception("No se pudo calcular el área");
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception e) {}
            if (stmt != null) try { stmt.close(); } catch (Exception e) {}
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }
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
    
    /**
     * Convierte una respuesta en formato maestro-detalle a formato plano (FLAT).
     * Cada registro representa un predio + capa específica.
     * 
     * @param masterDetailResponse Respuesta original en formato jerárquico
     * @param request Request original (para obtener información adicional)
     * @return FlatVerificationResponse con registros desnormalizados
     */
    private FlatVerificationResponse convertToFlatResponse(
            VerifyPrediosByIdentifierResponse masterDetailResponse,
            VerifyPrediosByIdentifierRequest request) {
        
        FlatVerificationResponse flatResponse = new FlatVerificationResponse();
        
        // Copiar status y metadata
        flatResponse.setRequestStatus(masterDetailResponse.getRequestStatus());
        flatResponse.setIdentifierEcho(masterDetailResponse.getIdentifierEcho());
        flatResponse.setOutputFormat("FLAT");
        
        // Convertir predios y capas a registros planos
        List<FlatValidationRecord> flatRecords = new ArrayList<>();
        
        if (masterDetailResponse.getPredioVerifications() != null) {
            for (PredioVerification predioVerif : masterDetailResponse.getPredioVerifications()) {
                // Calcular estado general del predio (agregado de todas las capas)
                String predioEstadoGeneral = calculatePredioOverallStatus(predioVerif);
                
                // Crear un registro por cada capa validada
                if (predioVerif.getLayersResults() != null) {
                    for (LayerResult layerResult : predioVerif.getLayersResults()) {
                        FlatValidationRecord record = new FlatValidationRecord();
                        
                        // === Información del Predio (se repite en cada registro) ===
                        record.setPredioId(predioVerif.getPredioId());
                        record.setPredioCodigo(predioVerif.getPredioCodigo());
                        record.setPredioAreaM2(predioVerif.getPredioAreaM2());
                        
                        // Calcular hectáreas
                        record.setPredioHectares(predioVerif.getPredioAreaM2() / 10000.0);
                        
                        record.setPredioOwnerCedula(predioVerif.getPredioOwnerCedula());
                        record.setPredioOwnerName(predioVerif.getPredioOwnerName());
                        record.setPredioEstadoGeneral(predioEstadoGeneral);
                        
                        // === Información de la Capa ===
                        record.setLayerId(layerResult.getLayerId());
                        record.setLayerName(layerResult.getLayerName());
                        record.setWmsLayerName(layerResult.getWmsLayerName());
                        
                        // === Resultado de la Validación ===
                        record.setIntersects(layerResult.isIntersects());
                        record.setIntersectionPercentage(layerResult.getPercentage());
                        record.setIntersectionAreaM2(layerResult.getIntersectionAreaM2());
                        record.setIntersectionGeoJSON(layerResult.getIntersectionGeoJSON());
                        record.setValidationPassed(layerResult.isValidationPassed());
                        record.setValidationMessage(layerResult.getValidationMessage());
                        record.setMaxAllowedPercentage(layerResult.getMaxAllowedPercentage());
                        record.setLayerNotLoaded(layerResult.isLayerNotLoaded());
                        
                        flatRecords.add(record);
                    }
                }
            }
        }
        
        flatResponse.setValidations(flatRecords);
        
        return flatResponse;
    }
    
    /**
     * Calcula el estado general de un predio basado en todas sus validaciones de capas.
     * 
     * @param predioVerif Verificación del predio con resultados de todas las capas
     * @return "APROBADO", "RECHAZADO" o "ADVERTENCIA"
     */
    private String calculatePredioOverallStatus(PredioVerification predioVerif) {
        if (predioVerif.getLayersResults() == null || predioVerif.getLayersResults().isEmpty()) {
            return "SIN_DATOS";
        }
        
        boolean hasRejection = false;
        boolean hasWarning = false;
        boolean hasError = false;
        
        for (LayerResult layerResult : predioVerif.getLayersResults()) {
            // Si la capa no pudo cargarse, es error
            if (layerResult.isLayerNotLoaded()) {
                hasError = true;
                continue;
            }
            
            // Si la validación falló, es rechazo
            if (!layerResult.isValidationPassed()) {
                hasRejection = true;
            }
            
            // Si hay intersección pero se aprobó, es advertencia
            if (layerResult.isIntersects() && layerResult.isValidationPassed()) {
                hasWarning = true;
            }
        }
        
        // Prioridad: ERROR > RECHAZADO > ADVERTENCIA > APROBADO
        if (hasError) {
            return "ERROR";
        } else if (hasRejection) {
            return "RECHAZADO";
        } else if (hasWarning) {
            return "ADVERTENCIA";
        } else {
            return "APROBADO";
        }
    }
}