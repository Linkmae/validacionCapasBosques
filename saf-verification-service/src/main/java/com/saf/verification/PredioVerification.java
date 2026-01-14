package com.saf.verification;

import java.util.List;

public class PredioVerification {
    private String predioId;           // Código del área (codigoArea del servicio externo)
    private String predioCodigo;       // Código alternativo del predio
    private String predioOwnerCedula;  // Cédula del propietario
    private String predioOwnerName;    // Nombre/razón social del propietario
    private double predioAreaM2;
    private int predioSRID;
    private String predioGeometryGeoJSON;
    private List<LayerResult> layersResults;

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

    public double getPredioAreaM2() {
        return predioAreaM2;
    }

    public void setPredioAreaM2(double predioAreaM2) {
        this.predioAreaM2 = predioAreaM2;
    }

    public int getPredioSRID() {
        return predioSRID;
    }

    public void setPredioSRID(int predioSRID) {
        this.predioSRID = predioSRID;
    }

    public String getPredioGeometryGeoJSON() {
        return predioGeometryGeoJSON;
    }

    public void setPredioGeometryGeoJSON(String predioGeometryGeoJSON) {
        this.predioGeometryGeoJSON = predioGeometryGeoJSON;
    }

    public List<LayerResult> getLayersResults() {
        return layersResults;
    }

    public void setLayersResults(List<LayerResult> layersResults) {
        this.layersResults = layersResults;
    }
}