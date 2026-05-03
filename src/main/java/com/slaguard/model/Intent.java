package com.slaguard.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "intents")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Intent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String intentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slice_id")
    public NetworkSlice slice;

    @Column(nullable = false, length = 2000)
    public String naturalLanguageDefinition;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime createdAt;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime lastEnforcedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public IntentStatus status;

    @Column(length = 5000)
    public String parsedSLADefinition; // JSON string of parsed SLA parameters

    @Column(length = 2000)
    public String errorMessage;

    @Column
    public Integer enforcementCount; // How many times this intent was enforced

    @Column(length = 5000)
    @ElementCollection
    public Map<String, String> extractedConditions; // Parsed conditions (e.g., time windows, thresholds)

    @Column(length = 1000)
    public String modelUsed; // AI model used for parsing

    @PrePersist
    public void onCreate() {
        if (intentId == null) {
            intentId = "INTENT-" + System.currentTimeMillis();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = IntentStatus.PENDING;
        }
        if (enforcementCount == null) {
            enforcementCount = 0;
        }
    }

    public enum IntentStatus {
        PENDING,      // Awaiting parsing and validation
        ACTIVE,       // SLA created and being monitored
        ENFORCED,     // Actively enforcing SLA
        FAILED,       // Parsing or creation failed
        EXPIRED       // Intent no longer valid
    }
}
