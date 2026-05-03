package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * CAMARA QoD session information response.
 * Extends CreateSession with session-specific fields.
 */
public class CamaraSessionInfo extends CamaraCreateSession {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("startedAt")
    private Long startedAt; // Epoch seconds

    @JsonProperty("expiresAt")
    private Long expiresAt; // Epoch seconds

    @JsonProperty("qosStatus")
    private QosStatus qosStatus;

    @JsonProperty("messages")
    private List<SessionMessage> messages;

    public CamaraSessionInfo() {
        super();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public QosStatus getQosStatus() {
        return qosStatus;
    }

    public void setQosStatus(QosStatus qosStatus) {
        this.qosStatus = qosStatus;
    }

    public List<SessionMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<SessionMessage> messages) {
        this.messages = messages;
    }
}
