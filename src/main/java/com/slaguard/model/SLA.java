package com.slaguard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.util.Map;

@Entity
@Table(name = "slas")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SLA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public String slaId;

    @Column(nullable = false)
    public String name;

    @Column(length = 2000)
    public String description;

    @Column(nullable = false)
    public Double latencyTargetMs;      // Target latency in milliseconds

    @Column(nullable = false)
    public Double latencyWarningMs;     // Warning threshold

    @Column(nullable = false)
    public Double latencyCriticalMs;    // Critical threshold

    @Column(nullable = false)
    public Double throughputTargetMbps; // Target throughput in Mbps

    @Column(nullable = false)
    public Double throughputWarningMbps;

    @Column(nullable = false)
    public Double throughputCriticalMbps;

    @Column
    public Double jitterTargetMs;

    @Column
    public Double jitterWarningMs;

    @Column
    public Double jitterCriticalMs;

    @Column
    public Double packetLossTargetPercent; // Target packet loss percentage

    @Column
    public Double packetLossWarningPercent;

    @Column
    public Double packetLossCriticalPercent;

    @Column(nullable = false)
    public Double availabilityTargetPercent; // Target availability (e.g., 99.99)

    @Column
    public Double availabilityWarningPercent;

    @Column
    public Double availabilityCriticalPercent;

    @Column(length = 500)
    @ElementCollection
    public Map<String, String> customMetrics; // Additional custom SLA metrics

    @Column(nullable = false)
    public boolean isActive;

    @Column(length = 1000)
    public String intentDefinition; // Natural language intent that created this SLA

    public SLA() {
    }

    public SLA(String slaId, String name, Double latencyTargetMs, Double throughputTargetMbps,
                Double availabilityTargetPercent) {
        this.slaId = slaId;
        this.name = name;
        this.latencyTargetMs = latencyTargetMs;
        this.latencyWarningMs = latencyTargetMs * 0.8;
        this.latencyCriticalMs = latencyTargetMs * 0.95;
        this.throughputTargetMbps = throughputTargetMbps;
        this.throughputWarningMbps = throughputTargetMbps * 0.8;
        this.throughputCriticalMbps = throughputTargetMbps * 0.95;
        this.availabilityTargetPercent = availabilityTargetPercent;
        this.availabilityWarningPercent = availabilityTargetPercent * 0.99;
        this.availabilityCriticalPercent = availabilityTargetPercent * 0.98;
        this.isActive = true;
    }

    @PrePersist
    public void onCreate() {
        if (slaId == null) {
            slaId = "SLA-" + System.currentTimeMillis();
        }
    }
}
