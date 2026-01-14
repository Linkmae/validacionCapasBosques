package com.saf.verification;

public class LayerResult {
    private String layerId;
    private String layerName;
    private String wmsLayerName;
    private boolean intersects;
    private double intersectionAreaM2;
    private double percentage;
    private String intersectionGeoJSON;
    private boolean validationPassed; // Nueva: indica si cumple la regla de validación
    private String validationMessage; // Nueva: mensaje de validación
    private Double maxAllowedPercentage; // Nueva: porcentaje máximo permitido
    private boolean layerNotLoaded; // Nueva: indica si la capa no está cargada en PostGIS

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

    public boolean isIntersects() {
        return intersects;
    }

    public void setIntersects(boolean intersects) {
        this.intersects = intersects;
    }

    public double getIntersectionAreaM2() {
        return intersectionAreaM2;
    }

    public void setIntersectionAreaM2(double intersectionAreaM2) {
        this.intersectionAreaM2 = intersectionAreaM2;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public String getIntersectionGeoJSON() {
        return intersectionGeoJSON;
    }

    public void setIntersectionGeoJSON(String intersectionGeoJSON) {
        this.intersectionGeoJSON = intersectionGeoJSON;
    }

    public boolean isValidationPassed() {
        return validationPassed;
    }

    public void setValidationPassed(boolean validationPassed) {
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

    public boolean isLayerNotLoaded() {
        return layerNotLoaded;
    }

    public void setLayerNotLoaded(boolean layerNotLoaded) {
        this.layerNotLoaded = layerNotLoaded;
    }
}