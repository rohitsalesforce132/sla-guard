package com.slaguard.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "remediation_actions")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemediationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String actionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slice_id")
    public NetworkSlice slice;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "remediationAction")
    public SLABreach breach;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RemediationType type;

    @Column(nullable = false)
    public String description;

    @Column(length = 5000)
    public String actionDetails;

    @Column(length = 1000)
    public String externalReference; // e.g., CAMARA session ID, TMF order ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ActionStatus status;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime initiatedAt;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime completedAt;

    @Column
    public Long executionTimeSeconds;

    @Column(length = 2000)
    public String resultMessage;

    @Column(length = 5000)
    public String errorMessage;

    @Column
    public Boolean successful;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime expiresAt; // For temporary actions like QoS boost

    @Column(length = 2000)
    public String rollbackAction; // How to undo this action

    @PrePersist
    public void onCreate() {
        if (actionId == null) {
            actionId = "REM-" + System.currentTimeMillis();
        }
        if (initiatedAt == null) {
            initiatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ActionStatus.INITIATED;
        }
    }

    public enum RemediationType {
        QOS_BOOST,          // CAMARA QoD session
        SLICE_RESIZE,       // Increase slice resources
        TRAFFIC_REROUTE,    // Route traffic to alternative path
        SCALE_UP,           // Add network resources
        ESCALATION,         // Escalate to human operator
        AUTO_RECOVERY,      // Automatic recovery action
        MITIGATION          // Mitigate impact without fixing root cause
    }

    public enum ActionStatus {
        INITIATED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED,
        EXPIRED
    }
}
