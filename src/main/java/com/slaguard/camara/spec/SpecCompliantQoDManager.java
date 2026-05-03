package com.slaguard.camara.spec;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.RemediationAction;
import com.slaguard.model.SLA;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spec-compliant CAMARA QoD Session Manager.
 * Wraps CamaraQoDClient with business logic, retry, and SLA-Guard integration.
 */
@ApplicationScoped
public class SpecCompliantQoDManager {

    @Inject
    @RestClient
    CamaraQoDClient qodClient;

    @ConfigProperty(name = "camara.qod.default-duration", defaultValue = "3600")
    int defaultDuration;

    @ConfigProperty(name = "camara.qod.max-retries", defaultValue = "3")
    int maxRetries;

    @ConfigProperty(name = "camara.qod.retry-delay-ms", defaultValue = "1000")
    int retryDelayMs;

    @ConfigProperty(name = "camara.qod.callback-base-url", defaultValue = "https://sla-guard.example.com")
    String callbackBaseUrl;

    /**
     * Create a QoD session for a network slice with SLA context.
     * Maps SLA breach severity to CAMARA QoS profiles.
     */
    public CamaraSessionInfo createSession(NetworkSlice slice, SLA sla, String qosProfile) {
        CamaraCreateSession request = new CamaraCreateSession();

        // Device identifier
        CamaraDevice device = new CamaraDevice();
        if (slice.deviceIpAddress != null && slice.deviceIpAddress.contains(".")) {
            DeviceIpv4Addr ipv4 = new DeviceIpv4Addr();
            ipv4.setPublicAddress(slice.deviceIpAddress);
            device.setIpv4Address(ipv4);
        } else if (slice.deviceIpAddress != null && slice.deviceIpAddress.contains(":")) {
            device.setIpv6Address(slice.deviceIpAddress);
        }
        if (slice.deviceId != null && slice.deviceId.startsWith("+")) {
            device.setPhoneNumber(slice.deviceId);
        }
        request.setDevice(device);

        // Application server
        CamaraApplicationServer appServer = new CamaraApplicationServer();
        appServer.setIpv4Address(slice.applicationServerIp);
        request.setApplicationServer(appServer);

        // QoS profile and duration
        request.setQosProfile(qosProfile);
        request.setDuration(defaultDuration);

        // Webhook for notifications
        WebhookConfig webhook = new WebhookConfig();
        webhook.setNotificationUrl(callbackBaseUrl + "/api/v1/camara/qod/callback");
        request.setWebhook(webhook);

        return executeWithRetry(() -> qodClient.createSession(request), "createSession");
    }

    /**
     * Create a session with full control over parameters.
     */
    public CamaraSessionInfo createSession(CamaraCreateSession request) {
        return executeWithRetry(() -> qodClient.createSession(request), "createSession");
    }

    /**
     * Get current session status from CAMARA.
     */
    public CamaraSessionInfo getSessionStatus(String sessionId) {
        return executeWithRetry(() -> qodClient.getSession(sessionId), "getSession");
    }

    /**
     * Terminate an active QoD session.
     */
    public void terminateSession(String sessionId) {
        executeWithRetry(() -> {
            qodClient.deleteSession(sessionId);
            return null;
        }, "deleteSession");
        Log.infof("CAMARA QoD session terminated: %s", sessionId);
    }

    /**
     * Handle incoming notification from CAMARA QoD server.
     * Processes QOS_STATUS_CHANGED events.
     */
    public void handleNotification(CamaraEventNotification notification) {
        if (notification == null || notification.getEvent() == null) {
            Log.warn("Received null CAMARA notification");
            return;
        }

        CamaraEvent event = notification.getEvent();
        Log.infof("CAMARA notification: type=%s, id=%s", event.getEventType(), event.getEventId());

        if (event.getEventType() == EventType.QOS_STATUS_CHANGED && event instanceof QosStatusChangedEvent) {
            QosStatusChangedDetail detail = ((QosStatusChangedEvent) event).getEventDetail();
            Log.infof("QoS status changed: session=%s, status=%s, info=%s",
                    detail.getSessionId(), detail.getQosStatus(), detail.getStatusInfo());

            if (detail.getQosStatus() == EventQosStatus.UNAVAILABLE) {
                Log.warnf("QoD session %s became UNAVAILABLE — statusInfo: %s",
                        detail.getSessionId(), detail.getStatusInfo());
                // SLA-Guard can trigger re-remediation here
            }
        }
    }

    /**
     * Escalate a session: terminate old, create new with higher QoS profile.
     */
    public CamaraSessionInfo escalateSession(String sessionId, String higherProfile) {
        Log.infof("Escalating session %s to profile %s", sessionId, higherProfile);

        // Get existing session details
        CamaraSessionInfo existing = getSessionStatus(sessionId);
        if (existing == null) {
            throw new CamaraQoDException("Session not found: " + sessionId, 404);
        }

        // Terminate old session
        terminateSession(sessionId);

        // Create new session with higher profile
        CamaraCreateSession request = new CamaraCreateSession();
        request.setDevice(existing.getDevice());
        request.setApplicationServer(existing.getApplicationServer());
        request.setDevicePorts(existing.getDevicePorts());
        request.setApplicationServerPorts(existing.getApplicationServerPorts());
        request.setQosProfile(higherProfile);
        request.setDuration(existing.getDuration());
        request.setWebhook(existing.getWebhook());

        return createSession(request);
    }

    /**
     * List available QoS profiles.
     */
    public List<QosProfile> listQosProfiles() {
        return executeWithRetry(() -> qodClient.listQosProfiles(null, null), "listQosProfiles");
    }

    /**
     * Get a specific QoS profile by name.
     */
    public QosProfile getQosProfile(String name) {
        return executeWithRetry(() -> qodClient.getQosProfile(name), "getQosProfile");
    }

    /**
     * Map SLA breach severity to CAMARA QoS profile.
     */
    public String mapBreachToProfile(String severity) {
        switch (severity.toUpperCase()) {
            case "CRITICAL":
            case "EMERGENCY":
                return "QOS_E"; // Emergency — lowest latency
            case "MAJOR":
                return "QOS_S"; // Signaling — very high priority
            case "WARNING":
                return "QOS_M"; // Medium — high priority
            case "MINOR":
                return "QOS_L"; // Low — medium priority
            default:
                return "QOS_B"; // Background — best effort
        }
    }

    /**
     * Execute an API call with exponential backoff retry.
     */
    private <T> T executeWithRetry(ApiCall<T> call, String operation) {
        CamaraQoDException lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return call.execute();
            } catch (Exception e) {
                lastError = new CamaraQoDException(
                        operation + " failed: " + e.getMessage(), 0);
                Log.warnf("CAMARA %s attempt %d/%d failed: %s",
                        operation, attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        long delay = (long) retryDelayMs * (1L << (attempt - 1));
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CamaraQoDException("Interrupted during retry", 0);
                    }
                }
            }
        }

        throw lastError != null ? lastError
                : new CamaraQoDException(operation + " failed after " + maxRetries + " retries", 0);
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute();
    }

    /**
     * Domain exception for CAMARA QoD API errors.
     */
    public static class CamaraQoDException extends RuntimeException {
        private final int statusCode;

        public CamaraQoDException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() { return statusCode; }
    }
}
