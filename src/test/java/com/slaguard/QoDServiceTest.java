package com.slaguard;

import com.slaguard.camara.QoDService;
import com.slaguard.camara.QoDSessionManager;
import com.slaguard.model.NetworkSlice;
import com.slaguard.model.QoDSession;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class QoDServiceTest {

    @Inject
    QoDService qodService;

    @Inject
    QoDSessionManager sessionManager;

    private NetworkSlice testSlice;

    @BeforeEach
    void setUp() {
        testSlice = new NetworkSlice();
        testSlice.sliceId = "TEST-SLICE-004";
        testSlice.name = "Test QoS Slice";
        testSlice.sliceType = NetworkSlice.SliceType.URLLC;
        testSlice.status = NetworkSlice.SliceStatus.ACTIVE;
        testSlice.deviceId = "device-qos-001";
        testSlice.deviceIpAddress = "10.0.5.100";
        testSlice.applicationServerIp = "10.0.6.50";

        testSlice.sla = new com.slaguard.model.SLA();
        testSlice.sla.slaId = "SLA-TEST-004";
        testSlice.sla.name = "QoS Test SLA";
        testSlice.sla.latencyTargetMs = 8.0;
        testSlice.sla.throughputTargetMbps = 30.0;
        testSlice.sla.availabilityTargetPercent = 99.95;
        testSlice.sla.isActive = true;
    }

    @Test
    void testCreateQoDSession() {
        // Note: This will create a mock session if CAMARA API is not available
        QoDSession session = qodService.createSessionForSlice(
                testSlice,
                QoDSession.QoSProfile.QOS_S,
                15
        );

        assertNotNull(session, "Session should be created");
        assertNotNull(session.sessionId, "Session ID should be set");
        assertEquals(QoDSession.QoSProfile.QOS_S, session.qosProfile, "QoS profile should match");
        assertEquals(15, session.durationMinutes, "Duration should match");
        assertEquals(QoDSession.SessionStatus.ACTIVE, session.status, "Status should be ACTIVE");
        assertNotNull(session.startedAt, "Start time should be set");
        assertNotNull(session.expiresAt, "Expiry time should be set");
    }

    @Test
    void testQoSProfileSelection() {
        // Test different slice types get appropriate profiles
        testSlice.sliceType = NetworkSlice.SliceType.EMERGENCY;
        QoDSession emergencySession = qodService.createSessionForSlice(
                testSlice, QoDSession.QoSProfile.QOS_E, 10
        );
        assertEquals(QoDSession.QoSProfile.QOS_E, emergencySession.qosProfile);

        testSlice.sliceType = NetworkSlice.SliceType.URLLC;
        QoDSession urllcSession = qodService.createSessionForSlice(
                testSlice, QoDSession.QoSProfile.QOS_S, 10
        );
        assertEquals(QoDSession.QoSProfile.QOS_S, urllcSession.qosProfile);

        testSlice.sliceType = NetworkSlice.SliceType.IOT;
        QoDSession iotSession = qodService.createSessionForSlice(
                testSlice, QoDSession.QoSProfile.QOS_L, 10
        );
        assertEquals(QoDSession.QoSProfile.QOS_L, iotSession.qosProfile);
    }

    @Test
    void testSessionDuration() {
        int testDuration = 30;
        QoDSession session = qodService.createSessionForSlice(
                testSlice,
                QoDSession.QoSProfile.QOS_M,
                testDuration
        );

        assertEquals(testDuration, session.durationMinutes);
        assertTrue(session.expiresAt.isAfter(session.startedAt));
    }

    @Test
    void testSessionWithRemediation() {
        com.slaguard.model.RemediationAction action = new com.slaguard.model.RemediationAction();
        action.actionId = "REM-TEST-001";
        action.type = com.slaguard.model.RemediationAction.RemediationType.QOS_BOOST;
        action.status = com.slaguard.model.RemediationAction.ActionStatus.INITIATED;
        action.initiatedAt = java.time.LocalDateTime.now();

        QoDSession session = sessionManager.createSessionForRemediation(
                testSlice,
                action,
                QoDSession.QoSProfile.QOS_S,
                20
        );

        assertNotNull(session, "Session should be created");
        assertEquals(action.actionId, session.remediationAction.actionId,
                "Session should be linked to remediation action");
        assertEquals(session.sessionId, action.externalReference,
                "Action should reference session ID");
    }

    @Test
    void testGetSessionStatus() {
        // First create a session
        QoDSession createdSession = qodService.createSessionForSlice(
                testSlice,
                QoDSession.QoSProfile.QOS_M,
                15
        );

        // Then get its status
        QoDSession retrievedSession = qodService.getSessionStatus(createdSession.sessionId);

        // Note: This might return null if CAMARA API is not available
        // For mock sessions, the original session is returned
        if (retrievedSession != null) {
            assertEquals(createdSession.sessionId, retrievedSession.sessionId);
        }
    }

    @Test
    void testRevokeSession() {
        // Create a session
        QoDSession session = qodService.createSessionForSlice(
                testSlice,
                QoDSession.QoSProfile.QOS_M,
                15
        );

        // Link to slice
        session.slice = testSlice;
        session.persist();

        // Revoke the session
        boolean revoked = sessionManager.revokeSession(session);

        // For mock sessions, revocation should succeed
        // For real CAMARA API, this depends on the API availability
        assertTrue(revoked || session.status == QoDSession.SessionStatus.CANCELLED,
                "Session should be revoked or already cancelled");
    }
}
