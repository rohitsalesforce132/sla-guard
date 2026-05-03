package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request to create a CAMARA QoD session.
 */
public class CamaraCreateSession {

    @JsonProperty("device")
    private CamaraDevice device;

    @JsonProperty("applicationServer")
    private CamaraApplicationServer applicationServer;

    @JsonProperty("devicePorts")
    private PortsSpec devicePorts;

    @JsonProperty("applicationServerPorts")
    private PortsSpec applicationServerPorts;

    @JsonProperty("qosProfile")
    private String qosProfile;

    @JsonProperty("duration")
    private Integer duration = 86400; // Default 24 hours in seconds

    @JsonProperty("webhook")
    private WebhookConfig webhook;

    public CamaraCreateSession() {
    }

    public CamaraDevice getDevice() {
        return device;
    }

    public void setDevice(CamaraDevice device) {
        this.device = device;
    }

    public CamaraApplicationServer getApplicationServer() {
        return applicationServer;
    }

    public void setApplicationServer(CamaraApplicationServer applicationServer) {
        this.applicationServer = applicationServer;
    }

    public PortsSpec getDevicePorts() {
        return devicePorts;
    }

    public void setDevicePorts(PortsSpec devicePorts) {
        this.devicePorts = devicePorts;
    }

    public PortsSpec getApplicationServerPorts() {
        return applicationServerPorts;
    }

    public void setApplicationServerPorts(PortsSpec applicationServerPorts) {
        this.applicationServerPorts = applicationServerPorts;
    }

    public String getQosProfile() {
        return qosProfile;
    }

    public void setQosProfile(String qosProfile) {
        this.qosProfile = qosProfile;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        if (duration != null && (duration < 1 || duration > 86400)) {
            throw new IllegalArgumentException("Duration must be between 1 and 86400 seconds");
        }
        this.duration = duration;
    }

    public WebhookConfig getWebhook() {
        return webhook;
    }

    public void setWebhook(WebhookConfig webhook) {
        this.webhook = webhook;
    }

    public boolean isValid() {
        return device != null && device.isValid() &&
               applicationServer != null && applicationServer.isValid() &&
               qosProfile != null && !qosProfile.isEmpty();
    }
}
