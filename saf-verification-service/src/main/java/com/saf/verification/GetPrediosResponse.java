package com.saf.verification;

import java.util.List;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "VerificationGetPrediosResponseType", namespace = "http://saf.com/verification")
public class GetPrediosResponse {
    private RequestStatus requestStatus;
    private List<Predio> predios;

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public List<Predio> getPredios() {
        return predios;
    }

    public void setPredios(List<Predio> predios) {
        this.predios = predios;
    }
}