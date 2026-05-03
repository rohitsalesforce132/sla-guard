package com.slaguard.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sla_breaches")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SLABreach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String breachId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slice_id")
    public NetworkSlice slice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SLAMetric.MetricType metricType;

    @Column(nullable = false)
    public Double actualValue;

    @Column(nullable = false)
    public Double thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Severity severity; // WARNING, CRITICAL, MAJOR, CRITICAL

    @Column(length = 2000)
    public String description;

    @Column(length = 5000)
    public String rootCauseAnalysis;

    @Column(length = 2000)
    public String affectedSubscribers;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime detectedAt;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime resolvedAt;

    @Column(nullable = false)
    public boolean resolved;

    @Column
    public Long durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column
    public SLAStatus statusBeforeBreach;

    @Column
    public Integer predictedMinutesBeforeBreach; // If predicted by BreachPredictor

    @Column
    public Double predictionConfidence;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public RemediationAction remediationAction;

    @PrePersist
    public void onCreate() {
        if (breachId == null) {
            breachId = "BREACH-" + System.currentTimeMillis();
        }
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }

    public enum Severity {
        WARNING,     // SLA approaching breach
        CRITICAL,    // SLA threshold exceeded
        MAJOR,       // Significant breach
        SEVERE       // Complete SLA failure
    }
}
