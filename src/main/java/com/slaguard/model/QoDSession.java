package com.slaguard.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "qod_sessions")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QoDSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slice_id")
    public NetworkSlice slice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remediation_action_id")
    public RemediationAction remediationAction;

    @Column(length = 100)
    public String devicePhoneNumber;

    @Column(length = 50)
    public String deviceIpv4Addr;

    @Column(length = 100)
    public String deviceIpv6Addr;

    @Column(length = 50)
    public String applicationServerIpv4Addr;

    @Column(length = 100)
    public String applicationServerIpv6Addr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public QoSProfile qosProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public QoSStatus qosStatus;

    @Column(nullable = false)
    public Integer durationMinutes;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime startedAt;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime expiresAt;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SessionStatus status;

    @Column(length = 1000)
    public String camaraResponse;

    @Column
    public Boolean slaRestored; // Did SLA recover after this session?

    @PrePersist
    public void onCreate() {
        if (sessionId == null) {
            sessionId = "QOD-" + System.currentTimeMillis();
        }
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (expiresAt == null && durationMinutes != null) {
            expiresAt = startedAt.plusMinutes(durationMinutes);
        }
        if (status == null) {
            status = SessionStatus.ACTIVE;
        }
        if (qosStatus == null) {
            qosStatus = QoSStatus.GRANTED;
        }
    }

    public enum QoSProfile {
        QOS_E,    // Emergency - highest priority
        QOS_S,    // Signaling - very high priority
        QOS_M,    // Medium - high priority
        QOS_L,    // Low - medium priority
        QOS_B     // Background - best effort
    }

    public enum QoSStatus {
        GRANTED,
        PENDING,
        REJECTED,
        REVOKED,
        EXPIRED
    }

    public enum SessionStatus {
        ACTIVE,
        COMPLETED,
        CANCELLED,
        FAILED,
        EXPIRED
    }
}
