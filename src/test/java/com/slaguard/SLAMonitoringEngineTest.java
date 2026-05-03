package com.slaguard;

import com.slaguard.engine.SLAEvaluator;
import com.slaguard.engine.SLAMonitoringEngine;
import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLA;
import com.slaguard.model.SLAMetric;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SLAMonitoringEngineTest {

    @Inject
    SLAMonitoringEngine monitoringEngine;

    @Inject
    SLAEvaluator slaEvaluator;

    private NetworkSlice testSlice;

    @BeforeEach
    void setUp() {
        // Create a test slice
        testSlice = new NetworkSlice();
        testSlice.sliceId = "TEST-SLICE-001";
        testSlice.name = "Test Enterprise Slice";
        testSlice.sliceType = NetworkSlice.SliceType.ENTERPRISE;
        testSlice.status = NetworkSlice.SliceStatus.ACTIVE;
        testSlice.deviceId = "device-001";
        testSlice.deviceIpAddress = "10.0.1.100";
        testSlice.applicationServerIp = "10.0.2.50";

        // Create SLA
        testSlice.sla = new SLA();
        testSlice.sla.slaId = "SLA-TEST-001";
        testSlice.sla.name = "Test SLA";
        testSlice.sla.latencyTargetMs = 10.0;
        testSlice.sla.throughputTargetMbps = 100.0;
        testSlice.sla.availabilityTargetPercent = 99.9;
        testSlice.sla.isActive = true;
    }

    @Test
    void testEvaluateCompliantMetrics() {
        // Create compliant metrics
        SLAMetric latencyMetric = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 5.0, "ms");
        SLAMetric throughputMetric = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 150.0, "Mbps");

        List<SLAMetric> metrics = List.of(latencyMetric, throughputMetric);

        // Evaluate
        SLAEvaluator.EvaluationResult result = slaEvaluator.evaluate(testSlice, metrics);

        assertEquals(SLAMetric.SLAStatus.COMPLIANT, result.overallStatus);
        assertEquals(SLAMetric.SLAStatus.COMPLIANT, result.latencyStatus);
        assertEquals(SLAMetric.SLAStatus.COMPLIANT, result.throughputStatus);
    }

    @Test
    void testEvaluateWarningMetrics() {
        // Create warning metrics (at 80% of threshold)
        SLAMetric latencyMetric = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 8.0, "ms");
        SLAMetric throughputMetric = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 85.0, "Mbps");

        List<SLAMetric> metrics = List.of(latencyMetric, throughputMetric);

        // Evaluate
        SLAEvaluator.EvaluationResult result = slaEvaluator.evaluate(testSlice, metrics);

        assertEquals(SLAMetric.SLAStatus.WARNING, result.overallStatus);
        assertEquals(SLAMetric.SLAStatus.WARNING, result.latencyStatus);
        assertEquals(SLAMetric.SLAStatus.WARNING, result.throughputStatus);
    }

    @Test
    void testEvaluateCriticalMetrics() {
        // Create critical metrics (at 95% of threshold)
        SLAMetric latencyMetric = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 9.5, "ms");
        SLAMetric throughputMetric = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 96.0, "Mbps");

        List<SLAMetric> metrics = List.of(latencyMetric, throughputMetric);

        // Evaluate
        SLAEvaluator.EvaluationResult result = slaEvaluator.evaluate(testSlice, metrics);

        assertEquals(SLAMetric.SLAStatus.CRITICAL, result.overallStatus);
    }

    @Test
    void testEvaluateBreachedMetrics() {
        // Create breached metrics (exceed threshold)
        SLAMetric latencyMetric = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 15.0, "ms");
        SLAMetric throughputMetric = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 50.0, "Mbps");

        List<SLAMetric> metrics = List.of(latencyMetric, throughputMetric);

        // Evaluate
        SLAEvaluator.EvaluationResult result = slaEvaluator.evaluate(testSlice, metrics);

        assertEquals(SLAMetric.SLAStatus.BREACHED, result.overallStatus);
        assertEquals(SLAMetric.SLAStatus.BREACHED, result.latencyStatus);
        assertEquals(SLAMetric.SLAStatus.BREACHED, result.throughputStatus);
    }

    @Test
    void testMetricDeviationCalculation() {
        SLAMetric latencyMetric = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 15.0, "ms");

        slaEvaluator.evaluateMetric(testSlice, latencyMetric);

        // Deviation should be 50% (15 is 50% over 10)
        assertEquals(50.0, latencyMetric.deviationFromTarget, 0.1);
        assertEquals(SLAMetric.SLAStatus.BREACHED, latencyMetric.status);
    }
}
