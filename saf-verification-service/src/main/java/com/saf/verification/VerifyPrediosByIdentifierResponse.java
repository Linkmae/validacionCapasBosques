package com.saf.verification;

import java.util.List;
import javax.xml.bind.annotation.XmlType;

@XmlType(namespace = "http://saf.com/verification", name = "VerificationVerifyPrediosByIdentifierResponse")
public class VerifyPrediosByIdentifierResponse {
    private RequestStatus requestStatus;
    private String identifierEcho;
    private List<PredioVerification> predioVerifications;
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

    public List<PredioVerification> getPredioVerifications() {
        return predioVerifications;
    }

    public void setPredioVerifications(List<PredioVerification> predioVerifications) {
        this.predioVerifications = predioVerifications;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }
}