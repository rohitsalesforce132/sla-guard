package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Session message with severity and description.
 */
public class SessionMessage {

    @JsonProperty("severity")
    private MessageSeverity severity;

    @JsonProperty("description")
    private String description;

    public SessionMessage() {
    }

    public SessionMessage(MessageSeverity severity, String description) {
        this.severity = severity;
        this.description = description;
    }

    public MessageSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(MessageSeverity severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
