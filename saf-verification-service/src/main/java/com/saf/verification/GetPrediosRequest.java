package com.saf.verification;

import javax.xml.bind.annotation.XmlType;

@XmlType(name = "VerificationGetPrediosRequestType", namespace = "http://saf.com/verification")
public class GetPrediosRequest {
    private String identifierType;
    private String identifierValue;

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
}