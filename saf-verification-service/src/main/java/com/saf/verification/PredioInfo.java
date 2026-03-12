package com.saf.verification;

import javax.xml.bind.annotation.XmlType;

/**
 * Información de predio que puede venir directamente en la solicitud
 * para evitar la consulta al servicio externo de predios.
 * 
 * Todos los campos excepto predioId y geometryWKT son opcionales.
 * 
 * @author SAF Team
 * @since 2026-03-11
 */
@XmlType(namespace = "http://saf.com/verification", name = "PredioInfo")
public class PredioInfo {
    private String predioId;          // ID único del predio (requerido)
    private String predioCodigo;      // Código catastral (opcional)
    private String geometryWKT;       // Geometría en formato WKT (requerido)
    private Double areaM2;            // Área en metros cuadrados (opcional, se calcula si no viene)
    private String ownerIdentifier;   // Cédula/RUC del propietario (opcional)
    private String ownerName;         // Nombre del propietario (opcional)
    private String provincia;         // Provincia (opcional)
    private String canton;            // Cantón (opcional)
    private String parroquia;         // Parroquia (opcional)
    private Integer srid;             // SRID de la geometría (opcional, default: 4326)

    // Constructores
    public PredioInfo() {
        this.srid = 4326; // WGS84 por defecto
    }

    public PredioInfo(String predioId, String geometryWKT) {
        this.predioId = predioId;
        this.geometryWKT = geometryWKT;
        this.srid = 4326;
    }

    /**
     * Valida que tenga la información mínima requerida
     */
    public boolean isValid() {
        return predioId != null && !predioId.trim().isEmpty() &&
               geometryWKT != null && !geometryWKT.trim().isEmpty();
    }

    /**
     * Convierte esta información a un objeto Predio
     */
    public Predio toPredio() {
        Predio predio = new Predio();
        predio.setPredioId(this.predioId);
        predio.setPredioCodigo(this.predioCodigo);
        predio.setGeometryWKT(this.geometryWKT);
        predio.setIdentifier(this.ownerIdentifier);
        predio.setOwnerName(this.ownerName);
        predio.setSRID(this.srid != null ? this.srid : 4326);
        
        if (this.areaM2 != null) {
            predio.setAreaM2(this.areaM2);
        }
        
        return predio;
    }

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

    public String getGeometryWKT() {
        return geometryWKT;
    }

    public void setGeometryWKT(String geometryWKT) {
        this.geometryWKT = geometryWKT;
    }

    public Double getAreaM2() {
        return areaM2;
    }

    public void setAreaM2(Double areaM2) {
        this.areaM2 = areaM2;
    }

    public String getOwnerIdentifier() {
        return ownerIdentifier;
    }

    public void setOwnerIdentifier(String ownerIdentifier) {
        this.ownerIdentifier = ownerIdentifier;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getCanton() {
        return canton;
    }

    public void setCanton(String canton) {
        this.canton = canton;
    }

    public String getParroquia() {
        return parroquia;
    }

    public void setParroquia(String parroquia) {
        this.parroquia = parroquia;
    }

    public Integer getSrid() {
        return srid;
    }

    public void setSrid(Integer srid) {
        this.srid = srid;
    }

    @Override
    public String toString() {
        return "PredioInfo{" +
                "predioId='" + predioId + '\'' +
                ", areaM2=" + areaM2 +
                ", provincia='" + provincia + '\'' +
                ", hasGeometry=" + (geometryWKT != null) +
                '}';
    }
}
