package com.slaguard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "network_slices")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkSlice extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String sliceId;

    @Column(nullable = false)
    public String name;

    @Column(length = 1000)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SliceType sliceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public SliceStatus status;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public SLA sla;

    @Column(nullable = false)
    public String deviceId;

    @Column
    public String deviceIpAddress;

    @Column
    public String applicationServerIp;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "slice", fetch = FetchType.LAZY)
    public List<SLAMetric> metrics = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "slice", fetch = FetchType.LAZY)
    public List<SLABreach> breaches = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "slice", fetch = FetchType.LAZY)
    public List<RemediationAction> remediations = new ArrayList<>();

    @Column(nullable = false)
    public LocalDateTime createdAt;

    @Column
    public LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SliceType {
        EMBB,          // Enhanced Mobile Broadband
        URLLC,         // Ultra-Reliable Low Latency Communications
        MMTC,          // Massive Machine Type Communications
        ENTERPRISE,    // Enterprise Private Network
        IOT,           // Internet of Things
        EMERGENCY      // Emergency Services
    }

    public enum SliceStatus {
        ACTIVE,
        SUSPENDED,
        TERMINATED,
        MAINTENANCE
    }
}
