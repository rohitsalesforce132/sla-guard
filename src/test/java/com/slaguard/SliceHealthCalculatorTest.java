package com.slaguard;

import com.slaguard.engine.SliceHealthCalculator;
import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLA;
import com.slaguard.model.SLAMetric;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SliceHealthCalculatorTest {

    @Inject
    SliceHealthCalculator healthCalculator;

    private NetworkSlice testSlice;

    @BeforeEach
    void setUp() {
        testSlice = new NetworkSlice();
        testSlice.sliceId = "TEST-SLICE-006";
        testSlice.name = "Test Health Slice";
        testSlice.sliceType = NetworkSlice.SliceType.EMBB;
        testSlice.status = NetworkSlice.SliceStatus.ACTIVE;

        testSlice.sla = new SLA();
        testSlice.sla.slaId = "SLA-TEST-006";
        testSlice.sla.name = "Health Test SLA";
        testSlice.sla.latencyTargetMs = 10.0;
        testSlice.sla.throughputTargetMbps = 100.0;
        testSlice.sla.availabilityTargetPercent = 99.9;
        testSlice.sla.isActive = true;
    }

    @Test
    void testPerfectHealthScore() {
        // All metrics compliant
        SLAMetric latency = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 5.0, "ms");
        latency.status = SLAMetric.SLAStatus.COMPLIANT;

        SLAMetric throughput = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 150.0, "Mbps");
        throughput.status = SLAMetric.SLAStatus.COMPLIANT;

        List<SLAMetric> metrics = List.of(latency, throughput);

        double healthScore = healthCalculator.calculateHealth(testSlice, metrics);

        assertEquals(100.0, healthScore, 0.1, "All compliant metrics should give perfect score");
        assertEquals(SliceHealthCalculator.HealthCategory.EXCELLENT,
                healthCalculator.getHealthCategory(healthScore));
    }

    @Test
    void testWarningHealthScore() {
        // One metric in warning
        SLAMetric latency = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 8.5, "ms");
        latency.status = SLAMetric.SLAStatus.WARNING;

        SLAMetric throughput = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 150.0, "Mbps");
        throughput.status = SLAMetric.SLAStatus.COMPLIANT;

        List<SLAMetric> metrics = List.of(latency, throughput);

        double healthScore = healthCalculator.calculateHealth(testSlice, metrics);

        assertTrue(healthScore < 100.0 && healthScore >= 75.0,
                "Warning metrics should reduce score to 75-100 range");
        assertEquals(SliceHealthCalculator.HealthCategory.GOOD,
                healthCalculator.getHealthCategory(healthScore));
    }

    @Test
    void testCriticalHealthScore() {
        // One metric in critical
        SLAMetric latency = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 9.8, "ms");
        latency.status = SLAMetric.SLAStatus.CRITICAL;

        SLAMetric throughput = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 150.0, "Mbps");
        throughput.status = SLAMetric.SLAStatus.COMPLIANT;

        List<SLAMetric> metrics = List.of(latency, throughput);

        double healthScore = healthCalculator.calculateHealth(testSlice, metrics);

        assertTrue(healthScore < 75.0 && healthScore >= 60.0,
                "Critical metrics should reduce score to 60-75 range");
        assertEquals(SliceHealthCalculator.HealthCategory.FAIR,
                healthCalculator.getHealthCategory(healthScore));
    }

    @Test
    void testBreachHealthScore() {
        // One metric in breach
        SLAMetric latency = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 15.0, "ms");
        latency.status = SLAMetric.SLAStatus.BREACHED;

        SLAMetric throughput = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 150.0, "Mbps");
        throughput.status = SLAMetric.SLAStatus.COMPLIANT;

        List<SLAMetric> metrics = List.of(latency, throughput);

        double healthScore = healthCalculator.calculateHealth(testSlice, metrics);

        assertTrue(healthScore < 50.0, "Breached metrics should give low score");
        assertEquals(SliceHealthCalculator.HealthCategory.CRITICAL,
                healthCalculator.getHealthCategory(healthScore));
    }

    @Test
    void testMixedMetricsHealthScore() {
        // Mix of compliant, warning, and critical
        SLAMetric latency = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 8.0, "ms");
        latency.status = SLAMetric.SLAStatus.WARNING;

        SLAMetric throughput = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 97.0, "Mbps");
        throughput.status = SLAMetric.SLAStatus.CRITICAL;

        SLAMetric availability = new SLAMetric(testSlice, SLAMetric.MetricType.AVAILABILITY, 99.95, "%");
        availability.status = SLAMetric.SLAStatus.COMPLIANT;

        List<SLAMetric> metrics = List.of(latency, throughput, availability);

        double healthScore = healthCalculator.calculateHealth(testSlice, metrics);

        // Average: (75 + 50 + 100) / 3 = 75
        assertTrue(healthScore > 60.0 && healthScore <= 85.0,
                "Mixed metrics should give intermediate score");
    }

    @Test
    void testNoMetricsGivesFullHealth() {
        // No metrics - assume healthy
        List<SLAMetric> metrics = List.of();

        double healthScore = healthCalculator.calculateHealth(testSlice, metrics);

        assertEquals(100.0, healthScore, 0.1, "No metrics should assume full health");
    }

    @Test
    void testNoSLAGivesFullHealth() {
        // Slice without SLA
        testSlice.sla = null;

        SLAMetric latency = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 50.0, "ms");

        double healthScore = healthCalculator.calculateHealth(testSlice, List.of(latency));

        assertEquals(100.0, healthScore, 0.1, "No SLA should give full health");
    }

    @Test
    void testHealthCategoryBoundaries() {
        // Test category boundaries
        assertEquals(SliceHealthCalculator.HealthCategory.EXCELLENT,
                healthCalculator.getHealthCategory(90.0));
        assertEquals(SliceHealthCalculator.HealthCategory.GOOD,
                healthCalculator.getHealthCategory(80.0));
        assertEquals(SliceHealthCalculator.HealthCategory.FAIR,
                healthCalculator.getHealthCategory(65.0));
        assertEquals(SliceHealthCalculator.HealthCategory.POOR,
                healthCalculator.getHealthCategory(45.0));
        assertEquals(SliceHealthCalculator.HealthCategory.CRITICAL,
                healthCalculator.getHealthCategory(30.0));
        assertEquals(SliceHealthCalculator.HealthCategory.CRITICAL,
                healthCalculator.getHealthCategory(0.0));
    }

    @Test
    void testWeightedAverageHealth() {
        // More metrics should be weighted correctly
        SLAMetric latency = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 5.0, "ms");
        latency.status = SLAMetric.SLAStatus.COMPLIANT;

        SLAMetric throughput = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 150.0, "Mbps");
        throughput.status = SLAMetric.SLAStatus.COMPLIANT;

        SLAMetric availability = new SLAMetric(testSlice, SLAMetric.MetricType.AVAILABILITY, 99.95, "%");
        availability.status = SLAMetric.SLAStatus.COMPLIANT;

        SLAMetric jitter = new SLAMetric(testSlice, SLAMetric.MetricType.JITTER, 1.0, "ms");
        jitter.status = SLAMetric.SLAStatus.COMPLIANT;

        SLAMetric packetLoss = new SLAMetric(testSlice, SLAMetric.MetricType.PACKET_LOSS, 0.01, "%");
        packetLoss.status = SLAMetric.SLAStatus.COMPLIANT;

        List<SLAMetric> metrics = List.of(latency, throughput, availability, jitter, packetLoss);

        double healthScore = healthCalculator.calculateHealth(testSlice, metrics);

        assertEquals(100.0, healthScore, 0.1, "All compliant metrics should give perfect score");
    }
}
