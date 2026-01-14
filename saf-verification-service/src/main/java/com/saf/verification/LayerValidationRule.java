package com.saf.verification;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla de validación específica para una capa geográfica.
 * Soporta umbrales escalonados por tamaño de predio.
 */
public class LayerValidationRule {
    private String layerName;
    private String layerTableName;
    private String schemaName;
    private String validationType; // BOSQUE_NO_BOSQUE o AREAS_CONSERVACION
    private Double maxIntersectionPercentage; // Porcentaje máximo permitido de intersección (legacy)
    private Double minIntersectionAreaM2; // Área mínima para considerar intersección significativa
    private String validationMessage; // Mensaje personalizado si falla la validación (legacy)
    private String layerVersion; // Versión de la capa (ej: "2019-08-08")
    private boolean isActive;
    
    // Nuevos campos para soporte de reglas avanzadas
    private String zoneType; // proteccion, recuperacion, restauracion, null si no aplica
    private String messageApproved; // Mensaje cuando pasa validación (orientado a EUDR)
    private String messageRejected; // Mensaje cuando falla validación (orientado a EUDR)
    private List<ThresholdBySize> thresholds; // Umbrales escalonados por tamaño

    public LayerValidationRule() {
        this.thresholds = new ArrayList<>();
    }

    public LayerValidationRule(String layerName, String layerTableName, String validationType) {
        this.layerName = layerName;
        this.layerTableName = layerTableName;
        this.validationType = validationType;
        this.isActive = true;
        this.thresholds = new ArrayList<>();
    }
    
    /**
     * Determina si esta regla usa umbrales escalonados o el sistema legacy.
     * @return true si tiene umbrales configurados
     */
    public boolean hasThresholds() {
        return thresholds != null && !thresholds.isEmpty();
    }
    
    /**
     * Encuentra el umbral aplicable para un tamaño de predio dado.
     * @param predioHectares Tamaño del predio en hectáreas
     * @return El umbral aplicable, o null si no hay umbrales configurados
     */
    public ThresholdBySize findApplicableThreshold(double predioHectares) {
        if (!hasThresholds()) {
            return null;
        }
        
        for (ThresholdBySize threshold : thresholds) {
            if (threshold.appliesTo(predioHectares)) {
                return threshold;
            }
        }
        
        // Si no se encuentra umbral específico, retornar el último (rango más alto)
        return thresholds.get(thresholds.size() - 1);
    }

    // Getters y Setters
    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getLayerTableName() {
        return layerTableName;
    }

    public void setLayerTableName(String layerTableName) {
        this.layerTableName = layerTableName;
    }

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    public Double getMaxIntersectionPercentage() {
        return maxIntersectionPercentage;
    }

    public void setMaxIntersectionPercentage(Double maxIntersectionPercentage) {
        this.maxIntersectionPercentage = maxIntersectionPercentage;
    }

    public Double getMinIntersectionAreaM2() {
        return minIntersectionAreaM2;
    }

    public void setMinIntersectionAreaM2(Double minIntersectionAreaM2) {
        this.minIntersectionAreaM2 = minIntersectionAreaM2;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getLayerVersion() {
        return layerVersion;
    }

    public void setLayerVersion(String layerVersion) {
        this.layerVersion = layerVersion;
    }
    
    public String getZoneType() {
        return zoneType;
    }
    
    public void setZoneType(String zoneType) {
        this.zoneType = zoneType;
    }
    
    public String getMessageApproved() {
        return messageApproved;
    }
    
    public void setMessageApproved(String messageApproved) {
        this.messageApproved = messageApproved;
    }
    
    public String getMessageRejected() {
        return messageRejected;
    }
    
    public void setMessageRejected(String messageRejected) {
        this.messageRejected = messageRejected;
    }
    
    public List<ThresholdBySize> getThresholds() {
        return thresholds;
    }
    
    public void setThresholds(List<ThresholdBySize> thresholds) {
        this.thresholds = thresholds;
    }
    
    public void addThreshold(ThresholdBySize threshold) {
        if (this.thresholds == null) {
            this.thresholds = new ArrayList<>();
        }
        this.thresholds.add(threshold);
    }

    @Override
    public String toString() {
        return "LayerValidationRule{" +
                "layerName='" + layerName + '\'' +
                ", layerTableName='" + layerTableName + '\'' +
                ", schemaName='" + schemaName + '\'' +
                ", validationType='" + validationType + '\'' +
                ", maxIntersectionPercentage=" + maxIntersectionPercentage +
                ", minIntersectionAreaM2=" + minIntersectionAreaM2 +
                ", layerVersion='" + layerVersion + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
