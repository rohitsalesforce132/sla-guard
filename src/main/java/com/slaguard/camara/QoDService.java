package com.slaguard.camara;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.QoDSession;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.UUID;

@ApplicationScoped
public class QoDService {

    @ConfigProperty(name = "sla-guard.camara.base-url")
    String camaraBaseUrl;

    @ConfigProperty(name = "sla-guard.camara.api-key")
    String apiKey;

    @Inject
    QoDSessionManager sessionManager;

    private Client client = ClientBuilder.newClient();

    /**
     * Create a new CAMARA Quality on Demand session
     */
    public QoDSession createSession(String deviceId, String deviceIp, String appServerIp,
                                    String qosProfile, int durationMinutes) {
        Log.infof("Creating CAMARA QoD session: device=%s, profile=%s, duration=%d min",
                deviceId, qosProfile, durationMinutes);

        try {
            CreateSessionRequest request = new CreateSessionRequest();
            request.device = new DeviceInfo();
            request.device.ipv4Addr = deviceIp;
            request.applicationServer = new ApplicationServerInfo();
            request.applicationServer.ipv4Addr = appServerIp;
            request.qosProfile = qosProfile;
            request.duration = durationMinutes;

            Response response = client.target(camaraBaseUrl)
                    .path("/qod/sessions")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-API-Key", apiKey)
                    .post(Entity.entity(request, MediaType.APPLICATION_JSON));

            if (response.getStatus() == 201 || response.getStatus() == 200) {
                CreateSessionResponse sessionResponse = response.readEntity(CreateSessionResponse.class);
                QoDSession session = convertToQoDSession(sessionResponse);
                Log.infof("CAMARA QoD session created: %s", session.sessionId);
                return session;
            } else {
                Log.errorf("CAMARA QoD session creation failed: %d - %s",
                        response.getStatus(), response.readEntity(String.class));
                return null;
            }

        } catch (Exception e) {
            Log.errorf("Error creating CAMARA QoD session: %s", e.getMessage(), e);
            // For demo, return a mock session
            return createMockSession(deviceId, deviceIp, appServerIp, qosProfile, durationMinutes);
        }
    }

    /**
     * Get session status from CAMARA
     */
    public QoDSession getSessionStatus(String sessionId) {
        Log.debugf("Getting CAMARA QoD session status: %s", sessionId);

        try {
            Response response = client.target(camaraBaseUrl)
                    .path("/qod/sessions/" + sessionId)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-API-Key", apiKey)
                    .get();

            if (response.getStatus() == 200) {
                CreateSessionResponse sessionResponse = response.readEntity(CreateSessionResponse.class);
                return convertToQoDSession(sessionResponse);
            } else {
                Log.warnf("Failed to get session status: %d", response.getStatus());
                return null;
            }

        } catch (Exception e) {
            Log.errorf("Error getting session status: %s", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Revoke a CAMARA QoD session
     */
    public boolean revokeSession(String sessionId) {
        Log.infof("Revoking CAMARA QoD session: %s", sessionId);

        try {
            Response response = client.target(camaraBaseUrl)
                    .path("/qod/sessions/" + sessionId)
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("X-API-Key", apiKey)
                    .delete();

            boolean success = response.getStatus() == 204 || response.getStatus() == 200;
            Log.infof("Session %s revoked: %s", sessionId, success);
            return success;

        } catch (Exception e) {
            Log.errorf("Error revoking session: %s", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create a QoD session for a network slice
     */
    public QoDSession createSessionForSlice(NetworkSlice slice, QoDSession.QoSProfile profile,
                                            int durationMinutes) {
        return createSession(
                slice.deviceId,
                slice.deviceIpAddress,
                slice.applicationServerIp,
                profile.name(),
                durationMinutes
        );
    }

    private QoDSession convertToQoDSession(CreateSessionResponse response) {
        QoDSession session = new QoDSession();
        session.sessionId = response.sessionId;
        session.deviceIpv4Addr = response.device != null ? response.device.ipv4Addr : null;
        session.applicationServerIpv4Addr = response.applicationServer != null ?
                response.applicationServer.ipv4Addr : null;
        session.qosProfile = QoDSession.QoSProfile.valueOf(response.qosProfile);
        session.qosStatus = QoDSession.QoSStatus.GRANTED;
        session.durationMinutes = response.duration;
        session.startedAt = LocalDateTime.now();
        session.expiresAt = LocalDateTime.now().plusMinutes(response.duration);
        session.status = QoDSession.SessionStatus.ACTIVE;
        return session;
    }

    /**
     * Create a mock session for demo/testing
     */
    private QoDSession createMockSession(String deviceId, String deviceIp, String appServerIp,
                                         String qosProfile, int durationMinutes) {
        QoDSession session = new QoDSession();
        session.sessionId = "QOD-MOCK-" + UUID.randomUUID().toString().substring(0, 8);
        session.deviceIpv4Addr = deviceIp;
        session.applicationServerIpv4Addr = appServerIp;
        session.qosProfile = QoDSession.QoSProfile.valueOf(qosProfile);
        session.qosStatus = QoDSession.QoSStatus.GRANTED;
        session.durationMinutes = durationMinutes;
        session.startedAt = LocalDateTime.now();
        session.expiresAt = LocalDateTime.now().plusMinutes(durationMinutes);
        session.status = QoDSession.SessionStatus.ACTIVE;
        session.camaraResponse = "MOCK - No real CAMARA API available";
        return session;
    }

    // CAMARA API request/response models

    public static class CreateSessionRequest {
        public DeviceInfo device;
        public ApplicationServerInfo applicationServer;
        public String qosProfile;
        public int duration;
    }

    public static class DeviceInfo {
        public String phoneNumber;
        public String ipv4Addr;
        public String ipv6Addr;
    }

    public static class ApplicationServerInfo {
        public String ipv4Addr;
        public String ipv6Addr;
    }

    public static class CreateSessionResponse {
        public String sessionId;
        public DeviceInfo device;
        public ApplicationServerInfo applicationServer;
        public String qosProfile;
        public String qosStatus;
        public int duration;
        public String startedAt;
        public String expiresAt;
    }
}
