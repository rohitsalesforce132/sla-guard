package com.slaguard.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sla_metrics")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SLAMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slice_id")
    public NetworkSlice slice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public MetricType metricType;

    @Column(nullable = false)
    public Double value;

    @Column(length = 100)
    public String unit; // e.g., "ms", "Mbps", "%"

    @Column
    public SLAStatus status; // COMPLIANT, WARNING, CRITICAL, BREACHED

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime timestamp;

    @Column
    public Double slaTarget;

    @Column
    public Double warningThreshold;

    @Column
    public Double criticalThreshold;

    @Column
    public Double deviationFromTarget; // Percentage deviation

    public SLAMetric() {
    }

    public SLAMetric(NetworkSlice slice, MetricType metricType, Double value, String unit) {
        this.slice = slice;
        this.metricType = metricType;
        this.value = value;
        this.unit = unit;
        this.timestamp = LocalDateTime.now();
    }

    public enum MetricType {
        LATENCY,
        THROUGHPUT,
        JITTER,
        PACKET_LOSS,
        AVAILABILITY
    }

    public enum SLAStatus {
        COMPLIANT,
        WARNING,
        CRITICAL,
        BREACHED
    }
}
