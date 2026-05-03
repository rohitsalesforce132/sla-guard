package com.slaguard.camara;

import java.time.LocalDateTime;

/**
 * CAMARA API data models
 * These models follow the CAMARA Quality on Demand API specification
 */
public class CamaraModels {

    /**
     * Request to create a QoD session
     */
    public static class CreateSessionRequest {
        public DeviceIdentifier device;
        public ApplicationServerIdentifier applicationServer;
        public String qosProfile;
        public int duration; // Duration in minutes

        public CreateSessionRequest() {}

        public CreateSessionRequest(String deviceIp, String appServerIp, String qosProfile, int duration) {
            this.device = new DeviceIdentifier();
            this.device.ipv4Address = deviceIp;
            this.applicationServer = new ApplicationServerIdentifier();
            this.applicationServer.ipv4Address = appServerIp;
            this.qosProfile = qosProfile;
            this.duration = duration;
        }
    }

    /**
     * Device identifier (phone number or IP address)
     */
    public static class DeviceIdentifier {
        public String phoneNumber; // E.164 format
        public String ipv4Address;
        public String ipv6Address;

        public boolean isValid() {
            return phoneNumber != null || ipv4Address != null || ipv6Address != null;
        }
    }

    /**
     * Application server identifier
     */
    public static class ApplicationServerIdentifier {
        public String ipv4Address;
        public String ipv6Address;

        public boolean isValid() {
            return ipv4Address != null || ipv6Address != null;
        }
    }

    /**
     * Response for QoD session creation
     */
    public static class CreateSessionResponse {
        public String sessionId;
        public DeviceIdentifier device;
        public ApplicationServerIdentifier applicationServer;
        public String qosProfile;
        public String qosStatus;
        public int duration;
        public LocalDateTime startedAt;
        public LocalDateTime expiresAt;
        public String correlationId;
    }

    /**
     * QoD session status response
     */
    public static class SessionStatusResponse {
        public String sessionId;
        public String qosStatus; // GRANTED, PENDING, REJECTED, REVOKED, EXPIRED
        public String qosProfile;
        public LocalDateTime startedAt;
        public LocalDateTime expiresAt;
        public LocalDateTime endedAt;
        public String statusReason;
    }

    /**
     * Error response from CAMARA API
     */
    public static class ErrorResponse {
        public String error;
        public String message;
        public int statusCode;
        public String requestId;
        public LocalDateTime timestamp;
    }

    /**
     * Available QoS profiles
     */
    public enum QoSProfile {
        QOS_E("Emergency", "Highest priority for emergency services"),
        QOS_S("Signaling", "Very high priority for control plane traffic"),
        QOS_M("Medium", "High priority for premium services"),
        QOS_L("Low", "Medium priority for standard services"),
        QOS_B("Background", "Best effort for non-critical traffic");

        private final String name;
        private final String description;

        QoSProfile(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * QoS status values
     */
    public enum QoSStatus {
        GRANTED,    // QoS has been granted
        PENDING,    // Request is pending
        REJECTED,   // Request was rejected
        REVOKED,    // QoS was revoked
        EXPIRED     // Session expired
    }

    /**
     * Webhook notification for session events
     */
    public static class SessionEvent {
        public String sessionId;
        public String eventType; // SESSION_GRANTED, SESSION_REVOKED, SESSION_EXPIRED
        public String qosStatus;
        public LocalDateTime eventTime;
        public String reason;
    }
}
