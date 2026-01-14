package com.saf.verification;

public class RequestStatus {
    private String code;
    private String errorType;
    private String message;

    public RequestStatus() {
    }

    public RequestStatus(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public RequestStatus(String code, String errorType, String message) {
        this.code = code;
        this.errorType = errorType;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}