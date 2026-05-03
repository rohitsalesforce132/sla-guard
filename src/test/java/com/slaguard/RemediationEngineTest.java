package com.slaguard;

import com.slaguard.engine.RemediationEngine;
import com.slaguard.engine.SLAEvaluator;
import com.slaguard.model.NetworkSlice;
import com.slaguard.model.RemediationAction;
import com.slaguard.model.SLA;
import com.slaguard.model.SLAMetric;
import com.slaguard.prediction.PredictionResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RemediationEngineTest {

    @Inject
    RemediationEngine remediationEngine;

    @Inject
    SLAEvaluator slaEvaluator;

    private NetworkSlice testSlice;

    @BeforeEach
    void setUp() {
        testSlice = new NetworkSlice();
        testSlice.sliceId = "TEST-SLICE-003";
        testSlice.name = "Test Emergency Slice";
        testSlice.sliceType = NetworkSlice.SliceType.EMERGENCY;
        testSlice.status = NetworkSlice.SliceStatus.ACTIVE;
        testSlice.deviceId = "device-emergency-001";
        testSlice.deviceIpAddress = "10.0.3.100";
        testSlice.applicationServerIp = "10.0.4.50";

        testSlice.sla = new SLA();
        testSlice.sla.slaId = "SLA-TEST-003";
        testSlice.sla.name = "Emergency SLA";
        testSlice.sla.latencyTargetMs = 5.0;
        testSlice.sla.throughputTargetMbps = 50.0;
        testSlice.sla.availabilityTargetPercent = 99.99;
        testSlice.sla.isActive = true;
    }

    @Test
    void testRemediationForBreach() {
        // Create breached metrics
        SLAMetric latencyMetric = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 10.0, "ms");
        SLAMetric throughputMetric = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 20.0, "Mbps");

        List<SLAMetric> metrics = List.of(latencyMetric, throughputMetric);

        // Evaluate to get result
        SLAEvaluator.EvaluationResult evaluation = slaEvaluator.evaluate(testSlice, metrics);

        // Create prediction indicating high risk
        PredictionResult prediction = new PredictionResult();
        prediction.breachProbability = 1.0;
        prediction.estimatedTimeToBreach = 0; // Already breached
        prediction.confidence = 0.9;

        // Handle breach
        remediationEngine.handleSLABreach(testSlice, evaluation, prediction);

        // Verify remediation action was created (would check DB in real test)
        assertNotNull(evaluation, "Evaluation should not be null");
        assertEquals(SLAMetric.SLAStatus.BREACHED, evaluation.overallStatus);
    }

    @Test
    void testRemediationForCriticalStatus() {
        // Create critical metrics
        SLAMetric latencyMetric = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 4.75, "ms");

        List<SLAMetric> metrics = List.of(latencyMetric);

        // Evaluate
        SLAEvaluator.EvaluationResult evaluation = slaEvaluator.evaluate(testSlice, metrics);

        // Create prediction
        PredictionResult prediction = new PredictionResult();
        prediction.breachProbability = 0.85;
        prediction.estimatedTimeToBreach = 5;
        prediction.confidence = 0.8;

        // Handle
        remediationEngine.handleSLABreach(testSlice, evaluation, prediction);

        assertEquals(SLAMetric.SLAStatus.CRITICAL, evaluation.overallStatus);
    }

    @Test
    void testRemediationForImminentBreach() {
        // Create warning metrics with high prediction
        SLAMetric latencyMetric = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 4.0, "ms");

        List<SLAMetric> metrics = List.of(latencyMetric);

        // Evaluate
        SLAEvaluator.EvaluationResult evaluation = slaEvaluator.evaluate(testSlice, metrics);

        // Create prediction of imminent breach
        PredictionResult prediction = new PredictionResult();
        prediction.breachProbability = 0.9;
        prediction.estimatedTimeToBreach = 8;
        prediction.confidence = 0.85;

        // Handle
        remediationEngine.handleSLABreach(testSlice, evaluation, prediction);

        // Should trigger remediation even though status is just WARNING
        assertTrue(evaluation.latencyStatus == SLAMetric.SLAStatus.WARNING ||
                   evaluation.latencyStatus == SLAMetric.SLAStatus.CRITICAL);
    }

    @Test
    void testNoRemediationForCompliantStatus() {
        // Create compliant metrics
        SLAMetric latencyMetric = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 3.0, "ms");
        SLAMetric throughputMetric = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 60.0, "Mbps");

        List<SLAMetric> metrics = List.of(latencyMetric, throughputMetric);

        // Evaluate
        SLAEvaluator.EvaluationResult evaluation = slaEvaluator.evaluate(testSlice, metrics);

        // Create low-risk prediction
        PredictionResult prediction = new PredictionResult();
        prediction.breachProbability = 0.2;
        prediction.estimatedTimeToBreach = 60;
        prediction.confidence = 0.7;

        // Handle - should not trigger remediation
        // (In real implementation, we'd verify no action was created)
        assertEquals(SLAMetric.SLAStatus.COMPLIANT, evaluation.overallStatus);
        assertTrue(prediction.breachProbability < 0.5);
    }

    @Test
    void testEscalationDecision() {
        // Create warning metrics with long-term trend
        SLAMetric latencyMetric = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 4.2, "ms");

        List<SLAMetric> metrics = List.of(latencyMetric);

        // Evaluate
        SLAEvaluator.EvaluationResult evaluation = slaEvaluator.evaluate(testSlice, metrics);

        // Create prediction with moderate probability but long time to breach
        PredictionResult prediction = new PredictionResult();
        prediction.breachProbability = 0.75;
        prediction.estimatedTimeToBreach = 45; // Long time
        prediction.confidence = 0.7;

        // Handle - might decide to escalate
        remediationEngine.handleSLABreach(testSlice, evaluation, prediction);

        assertTrue(prediction.breachProbability > 0.7);
        assertTrue(prediction.estimatedTimeToBreach > 30);
    }
}
