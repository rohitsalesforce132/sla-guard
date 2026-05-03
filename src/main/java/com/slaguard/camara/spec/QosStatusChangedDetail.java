package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * QoS Status Changed Event detail from CAMARA QoD API callbacks.
 */
public class QosStatusChangedDetail {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("qosStatus")
    private EventQosStatus qosStatus;

    @JsonProperty("statusInfo")
    private StatusInfo statusInfo;

    public QosStatusChangedDetail() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public EventQosStatus getQosStatus() { return qosStatus; }
    public void setQosStatus(EventQosStatus qosStatus) { this.qosStatus = qosStatus; }

    public StatusInfo getStatusInfo() { return statusInfo; }
    public void setStatusInfo(StatusInfo statusInfo) { this.statusInfo = statusInfo; }
}
