package com.saf.verification;

import javax.xml.bind.annotation.XmlType;

/**
 * Registro individual en formato plano.
 * Representa una validación de un predio contra una capa específica.
 * Los datos del predio se repiten en cada registro (desnormalizado).
 * 
 * @author SAF Team
 * @since 2026-03-11
 */
@XmlType(namespace = "http://saf.com/verification", name = "FlatValidationRecord")
public class FlatValidationRecord {
    
    // === Información del Predio ===
    private String predioId;
    private String predioCodigo;
    private Double predioAreaM2;
    private Double predioHectares;
    private String predioOwnerCedula;
    private String predioOwnerName;
    private String predioEstadoGeneral; // APROBADO, RECHAZADO, ADVERTENCIA
    
    // === Información de la Capa ===
    private String layerId;
    private String layerName;
    private String wmsLayerName;
    
    // === Resultado de la Validación ===
    private Boolean intersects;
    private Double intersectionPercentage;
    private Double intersectionAreaM2;
    private String intersectionGeoJSON;
    private Boolean validationPassed;
    private String validationMessage;
    private Double maxAllowedPercentage;
    private Boolean layerNotLoaded;
    
    public FlatValidationRecord() {}
    
    // Getters y Setters
    
    public String getPredioId() {
        return predioId;
    }
    
    public void setPredioId(String predioId) {
        this.predioId = predioId;
    }
    
    public String getPredioCodigo() {
        return predioCodigo;
    }
    
    public void setPredioCodigo(String predioCodigo) {
        this.predioCodigo = predioCodigo;
    }
    
    public Double getPredioAreaM2() {
        return predioAreaM2;
    }
    
    public void setPredioAreaM2(Double predioAreaM2) {
        this.predioAreaM2 = predioAreaM2;
    }
    
    public Double getPredioHectares() {
        return predioHectares;
    }
    
    public void setPredioHectares(Double predioHectares) {
        this.predioHectares = predioHectares;
    }
    
    public String getPredioOwnerCedula() {
        return predioOwnerCedula;
    }
    
    public void setPredioOwnerCedula(String predioOwnerCedula) {
        this.predioOwnerCedula = predioOwnerCedula;
    }
    
    public String getPredioOwnerName() {
        return predioOwnerName;
    }
    
    public void setPredioOwnerName(String predioOwnerName) {
        this.predioOwnerName = predioOwnerName;
    }
    
    public String getPredioEstadoGeneral() {
        return predioEstadoGeneral;
    }
    
    public void setPredioEstadoGeneral(String predioEstadoGeneral) {
        this.predioEstadoGeneral = predioEstadoGeneral;
    }
    
    public String getLayerId() {
        return layerId;
    }
    
    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }
    
    public String getLayerName() {
        return layerName;
    }
    
    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }
    
    public String getWmsLayerName() {
        return wmsLayerName;
    }
    
    public void setWmsLayerName(String wmsLayerName) {
        this.wmsLayerName = wmsLayerName;
    }
    
    public Boolean getIntersects() {
        return intersects;
    }
    
    public void setIntersects(Boolean intersects) {
        this.intersects = intersects;
    }
    
    public Double getIntersectionPercentage() {
        return intersectionPercentage;
    }
    
    public void setIntersectionPercentage(Double intersectionPercentage) {
        this.intersectionPercentage = intersectionPercentage;
    }
    
    public Double getIntersectionAreaM2() {
        return intersectionAreaM2;
    }
    
    public void setIntersectionAreaM2(Double intersectionAreaM2) {
        this.intersectionAreaM2 = intersectionAreaM2;
    }
    
    public String getIntersectionGeoJSON() {
        return intersectionGeoJSON;
    }
    
    public void setIntersectionGeoJSON(String intersectionGeoJSON) {
        this.intersectionGeoJSON = intersectionGeoJSON;
    }
    
    public Boolean getValidationPassed() {
        return validationPassed;
    }
    
    public void setValidationPassed(Boolean validationPassed) {
        this.validationPassed = validationPassed;
    }
    
    public String getValidationMessage() {
        return validationMessage;
    }
    
    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
    
    public Double getMaxAllowedPercentage() {
        return maxAllowedPercentage;
    }
    
    public void setMaxAllowedPercentage(Double maxAllowedPercentage) {
        this.maxAllowedPercentage = maxAllowedPercentage;
    }
    
    public Boolean getLayerNotLoaded() {
        return layerNotLoaded;
    }
    
    public void setLayerNotLoaded(Boolean layerNotLoaded) {
        this.layerNotLoaded = layerNotLoaded;
    }
}
