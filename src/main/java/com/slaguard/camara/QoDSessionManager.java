package com.slaguard.camara;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.QoDSession;
import com.slaguard.model.RemediationAction;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class QoDSessionManager {

    @Inject
    QoDService qodService;

    /**
     * Scheduled task to check and update active QoD sessions
     */
    @Scheduled(every = "1m", delayed = "1m")
    @Transactional
    public void monitorSessions() {
        List<QoDSession> activeSessions = QoDSession.list(
                "status", QoDSession.SessionStatus.ACTIVE);

        for (QoDSession session : activeSessions) {
            try {
                updateSessionStatus(session);
            } catch (Exception e) {
                Log.errorf("Error updating session %s: %s", session.sessionId, e.getMessage(), e);
            }
        }
    }

    /**
     * Update session status from CAMARA API
     */
    @Transactional
    public void updateSessionStatus(QoDSession session) {
        // Check if session has expired
        if (session.expiresAt != null && LocalDateTime.now().isAfter(session.expiresAt)) {
            session.status = QoDSession.SessionStatus.EXPIRED;
            session.qosStatus = QoDSession.QoSStatus.EXPIRED;
            session.endedAt = LocalDateTime.now();
            session.persist();
            Log.infof("Session %s expired at %s", session.sessionId, session.expiresAt);
            return;
        }

        // Query CAMARA for current status
        QoDSession updated = qodService.getSessionStatus(session.sessionId);
        if (updated != null) {
            session.qosStatus = updated.qosStatus;
            session.persist();
        }
    }

    /**
     * Revoke an active session
     */
    @Transactional
    public boolean revokeSession(QoDSession session) {
        boolean success = qodService.revokeSession(session.sessionId);
        if (success) {
            session.status = QoDSession.SessionStatus.CANCELLED;
            session.qosStatus = QoDSession.QoSStatus.REVOKED;
            session.endedAt = LocalDateTime.now();
            session.persist();
            Log.infof("Session %s revoked successfully", session.sessionId);
        }
        return success;
    }

    /**
     * Create a new session for a remediation action
     */
    @Transactional
    public QoDSession createSessionForRemediation(NetworkSlice slice, RemediationAction action,
                                                  QoDSession.QoSProfile profile, int durationMinutes) {
        QoDSession session = qodService.createSessionForSlice(slice, profile, durationMinutes);

        if (session != null) {
            session.slice = slice;
            session.remediationAction = action;
            session.persist();

            // Update remediation action with session reference
            action.externalReference = session.sessionId;
            action.expiresAt = session.expiresAt;
            action.persist();

            Log.infof("Created session %s for remediation %s", session.sessionId, action.actionId);
        }

        return session;
    }

    /**
     * Get active sessions for a slice
     */
    public List<QoDSession> getActiveSessionsForSlice(NetworkSlice slice) {
        return QoDSession.list(
                "slice = ?1 and status = ?2",
                slice, QoDSession.SessionStatus.ACTIVE);
    }

    /**
     * Get all active sessions
     */
    public List<QoDSession> getAllActiveSessions() {
        return QoDSession.list("status", QoDSession.SessionStatus.ACTIVE);
    }

    /**
     * Clean up expired sessions
     */
    @Scheduled(every = "5m", delayed = "5m")
    @Transactional
    public void cleanupExpiredSessions() {
        List<QoDSession> expiredSessions = QoDSession.list(
                "status = ?1 and expiresAt < ?2",
                QoDSession.SessionStatus.ACTIVE, LocalDateTime.now());

        for (QoDSession session : expiredSessions) {
            session.status = QoDSession.SessionStatus.EXPIRED;
            session.qosStatus = QoDSession.QoSStatus.EXPIRED;
            session.endedAt = LocalDateTime.now();
            session.persist();
            Log.debugf("Marked session %s as expired", session.sessionId);
        }

        if (!expiredSessions.isEmpty()) {
            Log.infof("Cleaned up %d expired sessions", expiredSessions.size());
        }
    }
}
