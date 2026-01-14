package com.saf.verification;

import java.util.List;
import javax.xml.bind.annotation.XmlType;

@XmlType(namespace = "http://saf.com/verification", name = "VerificationVerifyPrediosByIdentifierRequest")
public class VerifyPrediosByIdentifierRequest {
    private String identifierType; // CEDULA o RUC
    private String identifierValue;
    private String verificationType; // BOSQUE_NO_BOSQUE o AREAS_CONSERVACION
    private List<String> layersToCheck;
    private boolean includeIntersectionGeoJSON;

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
}