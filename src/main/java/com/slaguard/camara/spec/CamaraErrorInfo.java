package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error response from CAMARA QoD API.
 */
public class CamaraErrorInfo {

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    public CamaraErrorInfo() {
    }

    public CamaraErrorInfo(Integer status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "CamaraErrorInfo{" +
                "status=" + status +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
