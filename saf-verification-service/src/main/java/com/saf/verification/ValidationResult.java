package com.saf.verification;

/**
 * Resultado de una validación de intersección.
 * 
 * Encapsula tanto el resultado booleano (aprobado/rechazado)
 * como el mensaje descriptivo correspondiente.
 * 
 * @author SAF Team
 * @since 2026-01-11
 */
public class ValidationResult {
    private boolean passed;
    private String message;
    private Double appliedThreshold;     // Umbral aplicado para este caso
    private Double calculatedPercentage; // Porcentaje real calculado
    private String thresholdRangeDescription; // "≤1 ha", "1-2 ha", etc.
    
    public ValidationResult() {
    }
    
    public ValidationResult(boolean passed, String message) {
        this.passed = passed;
        this.message = message;
    }
    
    public ValidationResult(boolean passed, String message, Double appliedThreshold, 
                           Double calculatedPercentage, String thresholdRangeDescription) {
        this.passed = passed;
        this.message = message;
        this.appliedThreshold = appliedThreshold;
        this.calculatedPercentage = calculatedPercentage;
        this.thresholdRangeDescription = thresholdRangeDescription;
    }
    
    // Getters y Setters
    
    public boolean isPassed() {
        return passed;
    }
    
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Double getAppliedThreshold() {
        return appliedThreshold;
    }
    
    public void setAppliedThreshold(Double appliedThreshold) {
        this.appliedThreshold = appliedThreshold;
    }
    
    public Double getCalculatedPercentage() {
        return calculatedPercentage;
    }
    
    public void setCalculatedPercentage(Double calculatedPercentage) {
        this.calculatedPercentage = calculatedPercentage;
    }
    
    public String getThresholdRangeDescription() {
        return thresholdRangeDescription;
    }
    
    public void setThresholdRangeDescription(String thresholdRangeDescription) {
        this.thresholdRangeDescription = thresholdRangeDescription;
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult[passed=%b, threshold=%.1f%%, calculated=%.2f%%, message=%s]",
            passed, appliedThreshold, calculatedPercentage, message);
    }
}
