package com.saf.verification;

import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * Respuesta en formato PLANO (desnormalizado) para validación de predios.
 * Cada registro representa la validación de un predio contra una capa específica.
 * 
 * @author SAF Team
 * @since 2026-03-11
 */
@XmlType(namespace = "http://saf.com/verification", name = "FlatVerificationResponse")
public class FlatVerificationResponse {
    
    private RequestStatus requestStatus;
    private String identifierEcho;
    private String outputFormat = "FLAT";
    private Integer totalRecords;
    private List<FlatValidationRecord> validations;
    
    public FlatVerificationResponse() {
    }
    
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
    
    public String getOutputFormat() {
        return outputFormat;
    }
    
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
    
    public Integer getTotalRecords() {
        return totalRecords;
    }
    
    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }
    
    public List<FlatValidationRecord> getValidations() {
        return validations;
    }
    
    public void setValidations(List<FlatValidationRecord> validations) {
        this.validations = validations;
        this.totalRecords = validations != null ? validations.size() : 0;
    }
}
