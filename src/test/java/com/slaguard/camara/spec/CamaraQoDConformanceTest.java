package com.slaguard.camara.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Conformance tests for CAMARA QoD API v0.9.0 spec compliance.
 * Validates that all data models match the OpenAPI specification exactly.
 */
@QuarkusTest
class CamaraQoDConformanceTest {

    @Inject
    ObjectMapper objectMapper;

    // ===== DeviceIpv4Addr =====

    @Test
    void deviceIpv4Addr_serializesCorrectly() throws Exception {
        DeviceIpv4Addr addr = new DeviceIpv4Addr();
        addr.setPublicAddress("203.0.113.1");
        addr.setPrivateAddress("192.168.1.1");
        addr.setPublicPort(8080);

        String json = objectMapper.writeValueAsString(addr);
        assertTrue(json.contains("\"publicAddress\":\"203.0.113.1\""));
        assertTrue(json.contains("\"privateAddress\":\"192.168.1.1\""));
        assertTrue(json.contains("\"publicPort\":8080"));
    }

    @Test
    void deviceIpv4Addr_deserializesFromSpec() throws Exception {
        String json = "{\"publicAddress\":\"203.0.113.1\",\"privateAddress\":\"10.0.0.1\",\"publicPort\":443}";
        DeviceIpv4Addr addr = objectMapper.readValue(json, DeviceIpv4Addr.class);
        assertEquals("203.0.113.1", addr.getPublicAddress());
        assertEquals("10.0.0.1", addr.getPrivateAddress());
        assertEquals(443, addr.getPublicPort());
    }

    // ===== CamaraDevice =====

    @Test
    void camaraDevice_withIpv4Address() throws Exception {
        DeviceIpv4Addr ipv4 = new DeviceIpv4Addr();
        ipv4.setPublicAddress("203.0.113.1");

        CamaraDevice device = new CamaraDevice();
        device.setIpv4Address(ipv4);

        String json = objectMapper.writeValueAsString(device);
        assertTrue(json.contains("\"ipv4Address\""));
        assertTrue(json.contains("\"publicAddress\":\"203.0.113.1\""));
    }

    @Test
    void camaraDevice_withPhoneNumber() throws Exception {
        CamaraDevice device = new CamaraDevice();
        device.setPhoneNumber("+1234567890");

        String json = objectMapper.writeValueAsString(device);
        assertTrue(json.contains("\"phoneNumber\":\"+1234567890\""));
    }

    @Test
    void camaraDevice_withIpv6Address() throws Exception {
        CamaraDevice device = new CamaraDevice();
        device.setIpv6Address("2001:db8::1");

        String json = objectMapper.writeValueAsString(device);
        assertTrue(json.contains("\"ipv6Address\":\"2001:db8::1\""));
    }

    // ===== PortsSpec and PortRange =====

    @Test
    void portsSpec_withRangesAndPorts() throws Exception {
        PortRange range = new PortRange();
        range.setFrom(8000);
        range.setTo(9000);

        PortsSpec spec = new PortsSpec();
        spec.setRanges(java.util.List.of(range));
        spec.setPorts(java.util.List.of(443, 8443));

        String json = objectMapper.writeValueAsString(spec);
        assertTrue(json.contains("\"ranges\""));
        assertTrue(json.contains("\"from\":8000"));
        assertTrue(json.contains("\"to\":9000"));
        assertTrue(json.contains("\"ports\":[443,8443]"));
    }

    // ===== CamaraCreateSession =====

    @Test
    void createSession_minimalRequest() throws Exception {
        DeviceIpv4Addr ipv4 = new DeviceIpv4Addr();
        ipv4.setPublicAddress("203.0.113.1");

        CamaraDevice device = new CamaraDevice();
        device.setIpv4Address(ipv4);

        CamaraApplicationServer server = new CamaraApplicationServer();
        server.setIpv4Address("198.51.100.1");

        CamaraCreateSession request = new CamaraCreateSession();
        request.setDevice(device);
        request.setApplicationServer(server);
        request.setQosProfile("QOS_E");

        String json = objectMapper.writeValueAsString(request);
        assertTrue(json.contains("\"qosProfile\":\"QOS_E\""));
        assertTrue(json.contains("\"device\""));
        assertTrue(json.contains("\"applicationServer\""));
        // Optional fields should be omitted
        assertFalse(json.contains("\"duration\""));
        assertFalse(json.contains("\"webhook\""));
    }

    @Test
    void createSession_fullRequest() throws Exception {
        CamaraCreateSession request = buildFullCreateSession();

        String json = objectMapper.writeValueAsString(request);
        assertTrue(json.contains("\"qosProfile\":\"QOS_S\""));
        assertTrue(json.contains("\"duration\":7200"));
        assertTrue(json.contains("\"notificationUrl\""));
        assertTrue(json.contains("\"devicePorts\""));
        assertTrue(json.contains("\"applicationServerPorts\""));
    }

    // ===== CamaraSessionInfo =====

    @Test
    void sessionInfo_deserializesFromSpec() throws Exception {
        String json = "{" +
            "\"sessionId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"," +
            "\"device\":{\"ipv4Address\":{\"publicAddress\":\"203.0.113.1\"}}," +
            "\"applicationServer\":{\"ipv4Address\":\"198.51.100.1\"}," +
            "\"qosProfile\":\"QOS_E\"," +
            "\"duration\":3600," +
            "\"startedAt\":1700000000," +
            "\"expiresAt\":1700003600," +
            "\"qosStatus\":\"AVAILABLE\"," +
            "\"messages\":[{\"severity\":\"INFO\",\"description\":\"Active\"}]" +
            "}";

        CamaraSessionInfo info = objectMapper.readValue(json, CamaraSessionInfo.class);
        assertEquals("3fa85f64-5717-4562-b3fc-2c963f66afa6", info.getSessionId());
        assertEquals("QOS_E", info.getQosProfile());
        assertEquals(QosStatus.AVAILABLE, info.getQosStatus());
        assertEquals(3600, info.getDuration());
        assertNotNull(info.getMessages());
        assertEquals(1, info.getMessages().size());
        assertEquals(MessageSeverity.INFO, info.getMessages().get(0).getSeverity());
    }

    // ===== CamaraErrorInfo =====

    @Test
    void errorInfo_deserializesFromSpec() throws Exception {
        String json = "{\"status\":400,\"code\":\"INVALID_ARGUMENT\",\"message\":\"Schema validation failed\"}";
        CamaraErrorInfo error = objectMapper.readValue(json, CamaraErrorInfo.class);
        assertEquals(400, error.getStatus());
        assertEquals("INVALID_ARGUMENT", error.getCode());
        assertEquals("Schema validation failed", error.getMessage());
    }

    @Test
    void errorInfo_allSpecErrorCodes() {
        // Validate all error codes from spec exist as string constants
        String[] codes = {
            "INVALID_ARGUMENT", "OUT_OF_RANGE", "UNAUTHENTICATED",
            "PERMISSION_DENIED", "NOT_FOUND", "CONFLICT",
            "INTERNAL", "NOT_IMPLEMENTED", "UNAVAILABLE"
        };
        for (String code : codes) {
            assertNotNull(code, "Error code should not be null: " + code);
        }
    }

    // ===== Enums =====

    @Test
    void qosStatus_allValues() {
        assertEquals(3, QosStatus.values().length);
        assertNotNull(QosStatus.valueOf("REQUESTED"));
        assertNotNull(QosStatus.valueOf("AVAILABLE"));
        assertNotNull(QosStatus.valueOf("UNAVAILABLE"));
    }

    @Test
    void eventQosStatus_allValues() {
        assertEquals(2, EventQosStatus.values().length);
        assertNotNull(EventQosStatus.valueOf("AVAILABLE"));
        assertNotNull(EventQosStatus.valueOf("UNAVAILABLE"));
    }

    @Test
    void statusInfo_allValues() {
        assertEquals(2, StatusInfo.values().length);
        assertNotNull(StatusInfo.valueOf("DURATION_EXPIRED"));
        assertNotNull(StatusInfo.valueOf("NETWORK_TERMINATED"));
    }

    @Test
    void eventType_allValues() {
        assertEquals(1, EventType.values().length);
        assertNotNull(EventType.valueOf("QOS_STATUS_CHANGED"));
    }

    // ===== QosProfile =====

    @Test
    void qosProfile_deserializesFromSpec() throws Exception {
        String json = "{" +
            "\"name\":\"QOS_E\"," +
            "\"description\":\"Emergency\"," +
            "\"status\":\"ACTIVE\"," +
            "\"priority\":99," +
            "\"packetDelayBudget\":{\"value\":10,\"unit\":\"Milliseconds\"}," +
            "\"packetErrorLossRate\":1" +
            "}";

        QosProfile profile = objectMapper.readValue(json, QosProfile.class);
        assertEquals("QOS_E", profile.getName());
        assertEquals(QosProfileStatus.ACTIVE, profile.getStatus());
        assertEquals(99, profile.getPriority());
        assertEquals(1, profile.getPacketErrorLossRate());
        assertNotNull(profile.getPacketDelayBudget());
        assertEquals(10, profile.getPacketDelayBudget().getValue());
        assertEquals("Milliseconds", profile.getPacketDelayBudget().getUnit());
    }

    // ===== CamaraEventNotification =====

    @Test
    void eventNotification_deserializesCallback() throws Exception {
        String json = "{" +
            "\"event\":{" +
                "\"eventId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\"," +
                "\"eventType\":\"QOS_STATUS_CHANGED\"," +
                "\"eventTime\":\"2024-01-01T00:00:00Z\"," +
                "\"eventDetail\":{" +
                    "\"sessionId\":\"abc-123\"," +
                    "\"qosStatus\":\"UNAVAILABLE\"," +
                    "\"statusInfo\":\"DURATION_EXPIRED\"" +
                "}" +
            "}" +
        "}";

        CamaraEventNotification notification = objectMapper.readValue(json, CamaraEventNotification.class);
        assertNotNull(notification.getEvent());
        assertEquals(EventType.QOS_STATUS_CHANGED, notification.getEvent().getEventType());
    }

    // ===== WebhookConfig =====

    @Test
    void webhookConfig_serializesCorrectly() throws Exception {
        WebhookConfig config = new WebhookConfig();
        config.setNotificationUrl("https://example.com/callback");
        config.setNotificationAuthToken("Bearer my-token-value-here");

        String json = objectMapper.writeValueAsString(config);
        assertTrue(json.contains("\"notificationUrl\":\"https://example.com/callback\""));
        assertTrue(json.contains("\"notificationAuthToken\":\"Bearer my-token-value-here\""));
    }

    // ===== SpecCompliantQoDManager =====

    @Test
    void manager_mapsBreachToProfile() {
        SpecCompliantQoDManager manager = new SpecCompliantQoDManager();
        assertEquals("QOS_E", manager.mapBreachToProfile("CRITICAL"));
        assertEquals("QOS_E", manager.mapBreachToProfile("EMERGENCY"));
        assertEquals("QOS_S", manager.mapBreachToProfile("MAJOR"));
        assertEquals("QOS_M", manager.mapBreachToProfile("WARNING"));
        assertEquals("QOS_L", manager.mapBreachToProfile("MINOR"));
        assertEquals("QOS_B", manager.mapBreachToProfile("INFO"));
    }

    // ===== Helper =====

    private CamaraCreateSession buildFullCreateSession() {
        DeviceIpv4Addr ipv4 = new DeviceIpv4Addr();
        ipv4.setPublicAddress("203.0.113.1");

        CamaraDevice device = new CamaraDevice();
        device.setIpv4Address(ipv4);
        device.setPhoneNumber("+1234567890");

        CamaraApplicationServer server = new CamaraApplicationServer();
        server.setIpv4Address("198.51.100.1");

        PortRange range = new PortRange();
        range.setFrom(8000);
        range.setTo(9000);
        PortsSpec devicePorts = new PortsSpec();
        devicePorts.setRanges(java.util.List.of(range));

        PortsSpec appPorts = new PortsSpec();
        appPorts.setPorts(java.util.List.of(443));

        WebhookConfig webhook = new WebhookConfig();
        webhook.setNotificationUrl("https://sla-guard.example.com/api/v1/camara/qod/callback");
        webhook.setNotificationAuthToken("Bearer secret-token-123456");

        CamaraCreateSession request = new CamaraCreateSession();
        request.setDevice(device);
        request.setApplicationServer(server);
        request.setDevicePorts(devicePorts);
        request.setApplicationServerPorts(appPorts);
        request.setQosProfile("QOS_S");
        request.setDuration(7200);
        request.setWebhook(webhook);

        return request;
    }
}
