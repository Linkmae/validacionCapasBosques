package com.saf.verification;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(namespace = "http://saf.com/verification", name = "VerificationVerifyPrediosByIdentifierResponse")
public class VerifyPrediosByIdentifierResponse {
    private RequestStatus requestStatus;
    private String identifierEcho;
    private String outputFormat = "MASTER_DETAIL"; // Indica el formato de respuesta
    private List<PredioVerification> predioVerifications;
    private List<FlatValidationRecord> validations;
    private Integer totalRecords;
    private Summary summary;

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getIdentifierEcho() {
        return identifierEcho;
    }

    public void setIdentifierEcho(String identifierEcho) {
        this.identifierEcho = identifierEcho;
    }

    @XmlElement(name = "outputFormat")
    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public List<PredioVerification> getPredioVerifications() {
        return predioVerifications;
    }

    public void setPredioVerifications(List<PredioVerification> predioVerifications) {
        this.predioVerifications = predioVerifications;
    }

    @XmlElement(name = "validations")
    public List<FlatValidationRecord> getValidations() {
        return validations;
    }

    public void setValidations(List<FlatValidationRecord> validations) {
        this.validations = validations;
        this.totalRecords = validations != null ? validations.size() : 0;
    }

    @XmlElement(name = "totalRecords")
    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }
}