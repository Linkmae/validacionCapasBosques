package com.saf.verification;

public class Predio {
    private String predioId;
    private String predioCodigo;
    private String identifier;
    private String ownerName;
    private double areaM2;
    private int srid;
    private String geometryWKT;
    private String geometryGeoJSON;

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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public double getAreaM2() {
        return areaM2;
    }

    public void setAreaM2(double areaM2) {
        this.areaM2 = areaM2;
    }

    public void setArea(double area) {
        this.areaM2 = area;
    }

    public int getSRID() {
        return srid;
    }

    public void setSRID(int srid) {
        this.srid = srid;
    }

    public String getGeometryWKT() {
        return geometryWKT;
    }

    public void setGeometryWKT(String geometryWKT) {
        this.geometryWKT = geometryWKT;
    }

    public void setGeometry(String geometry) {
        this.geometryWKT = geometry;
    }

    public String getGeometryGeoJSON() {
        return geometryGeoJSON;
    }

    public void setGeometryGeoJSON(String geometryGeoJSON) {
        this.geometryGeoJSON = geometryGeoJSON;
    }
}