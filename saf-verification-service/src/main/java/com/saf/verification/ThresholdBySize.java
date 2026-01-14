package com.saf.verification;

/**
 * Umbral de validación basado en el tamaño del predio.
 * 
 * Permite definir porcentajes máximos de intersección diferentes
 * según el tamaño del predio en hectáreas.
 * 
 * Ejemplo: Para Bosque - No Bosque:
 * - Predios ≤1ha: máximo 50% de intersección
 * - Predios 1-2ha: máximo 25% de intersección
 * - Predios >50ha: máximo 1% de intersección
 * 
 * @author SAF Team
 * @since 2026-01-11
 */
public class ThresholdBySize {
    private Integer id;
    private Double minHectares;      // Tamaño mínimo (inclusivo)
    private Double maxHectares;      // Tamaño máximo (exclusivo), null = sin límite
    private Double maxPercentage;    // Porcentaje máximo permitido
    private String description;
    
    public ThresholdBySize() {
    }
    
    public ThresholdBySize(Double minHectares, Double maxHectares, Double maxPercentage) {
        this.minHectares = minHectares;
        this.maxHectares = maxHectares;
        this.maxPercentage = maxPercentage;
    }
    
    /**
     * Verifica si un tamaño de predio cae dentro de este umbral.
     * 
     * @param predioHectares Tamaño del predio en hectáreas
     * @return true si el tamaño está en el rango de este umbral
     */
    public boolean appliesTo(double predioHectares) {
        boolean aboveMin = predioHectares >= minHectares;
        boolean belowMax = (maxHectares == null) || (predioHectares < maxHectares);
        return aboveMin && belowMax;
    }
    
    // Getters y Setters
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Double getMinHectares() {
        return minHectares;
    }
    
    public void setMinHectares(Double minHectares) {
        this.minHectares = minHectares;
    }
    
    public Double getMaxHectares() {
        return maxHectares;
    }
    
    public void setMaxHectares(Double maxHectares) {
        this.maxHectares = maxHectares;
    }
    
    public Double getMaxPercentage() {
        return maxPercentage;
    }
    
    public void setMaxPercentage(Double maxPercentage) {
        this.maxPercentage = maxPercentage;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        String maxStr = (maxHectares == null) ? "∞" : String.format("%.1f", maxHectares);
        return String.format("Threshold[%.1f-% s ha: max %.1f%%]", 
            minHectares, maxStr, maxPercentage);
    }
}
