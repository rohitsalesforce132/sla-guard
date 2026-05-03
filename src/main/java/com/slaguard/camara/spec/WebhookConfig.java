package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Webhook configuration for CAMARA QoD session notifications.
 */
public class WebhookConfig {

    @JsonProperty("notificationUrl")
    private String notificationUrl;

    @JsonProperty("notificationAuthToken")
    private String notificationAuthToken;

    public WebhookConfig() {
    }

    public WebhookConfig(String notificationUrl) {
        this.notificationUrl = notificationUrl;
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }

    public void setNotificationUrl(String notificationUrl) {
        this.notificationUrl = notificationUrl;
    }

    public String getNotificationAuthToken() {
        return notificationAuthToken;
    }

    public void setNotificationAuthToken(String notificationAuthToken) {
        this.notificationAuthToken = notificationAuthToken;
    }

    public boolean isValid() {
        return notificationUrl != null && !notificationUrl.isEmpty() &&
               (notificationAuthToken == null || (notificationAuthToken.length() >= 20 && notificationAuthToken.length() <= 256));
    }
}
