package com.saf.verification;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(namespace = "http://saf.com/verification", name = "VerificationVerifyPrediosByIdentifierRequest")
public class VerifyPrediosByIdentifierRequest {
    private String identifierType; // CEDULA o RUC
    private String identifierValue;
    private String verificationType; // BOSQUE_NO_BOSQUE o AREAS_CONSERVACION
    private List<String> layersToCheck;
    private boolean includeIntersectionGeoJSON;
    
    // ===== NUEVOS CAMPOS OPCIONALES (2026-03-11) =====
    // Si prediosData viene lleno, se usa esta información directamente
    // sin consultar al servicio externo de predios
    private List<PredioInfo> prediosData;
    
    // Formato de salida: "MASTER_DETAIL" (default) o "FLAT"
    private String outputFormat;

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getIdentifierValue() {
        return identifierValue;
    }

    public void setIdentifierValue(String identifierValue) {
        this.identifierValue = identifierValue;
    }

    public String getVerificationType() {
        return verificationType;
    }

    public void setVerificationType(String verificationType) {
        this.verificationType = verificationType;
    }

    public List<String> getLayersToCheck() {
        return layersToCheck;
    }

    public void setLayersToCheck(List<String> layersToCheck) {
        this.layersToCheck = layersToCheck;
    }

    public boolean isIncludeIntersectionGeoJSON() {
        return includeIntersectionGeoJSON;
    }

    public void setIncludeIntersectionGeoJSON(boolean includeIntersectionGeoJSON) {
        this.includeIntersectionGeoJSON = includeIntersectionGeoJSON;
    }

    public List<PredioInfo> getPrediosData() {
        return prediosData;
    }

    public void setPrediosData(List<PredioInfo> prediosData) {
        this.prediosData = prediosData;
    }

    /**
     * Verifica si se proporcionó información de predios directamente
     * @return true si hay datos de predios y no está vacío
     */
    public boolean hasDirectPredioData() {
        return prediosData != null && !prediosData.isEmpty();
    }
    
    @XmlElement(name = "outputFormat")
    public String getOutputFormat() {
        return outputFormat;
    }
    
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
    
    /**
     * Obtiene el formato de salida solicitado.
     * Si no se especifica, retorna "MASTER_DETAIL" por defecto.
     * @return "MASTER_DETAIL" o "FLAT"
     */
    public String getOutputFormatOrDefault() {
        if (outputFormat == null || outputFormat.trim().isEmpty()) {
            return "MASTER_DETAIL";
        }
        return outputFormat.toUpperCase().trim();
    }
    
    /**
     * Verifica si el formato solicitado es FLAT
     * @return true si el formato es FLAT
     */
    public boolean isFlatFormat() {
        return "FLAT".equals(getOutputFormatOrDefault());
    }
}