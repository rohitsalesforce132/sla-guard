package com.slaguard;

import com.slaguard.engine.BreachPredictor;
import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLA;
import com.slaguard.model.SLAMetric;
import com.slaguard.prediction.PredictionResult;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class BreachPredictorTest {

    @Inject
    BreachPredictor breachPredictor;

    private NetworkSlice testSlice;

    @BeforeEach
    void setUp() {
        testSlice = new NetworkSlice();
        testSlice.sliceId = "TEST-SLICE-002";
        testSlice.name = "Test IoT Slice";
        testSlice.sliceType = NetworkSlice.SliceType.IOT;
        testSlice.status = NetworkSlice.SliceStatus.ACTIVE;

        testSlice.sla = new SLA();
        testSlice.sla.slaId = "SLA-TEST-002";
        testSlice.sla.name = "Test IoT SLA";
        testSlice.sla.latencyTargetMs = 20.0;
        testSlice.sla.throughputTargetMbps = 10.0;
        testSlice.sla.availabilityTargetPercent = 99.5;
        testSlice.sla.isActive = true;
    }

    @Test
    void testNoBreachPredictionForStableMetrics() {
        // Create stable metrics (no upward trend)
        List<SLAMetric> metrics = createStableMetrics(20);

        PredictionResult prediction = breachPredictor.predict(testSlice, metrics);

        assertTrue(prediction.breachProbability < 0.5,
                "Breach probability should be low for stable metrics");
    }

    @Test
    void testBreachPredictionForIncreasingTrend() {
        // Create metrics with increasing trend
        List<SLAMetric> metrics = createIncreasingTrendMetrics(20);

        PredictionResult prediction = breachPredictor.predict(testSlice, metrics);

        assertTrue(prediction.breachProbability > 0.5,
                "Breach probability should be high for increasing trend");
        assertTrue(prediction.estimatedTimeToBreach < 30,
                "Time to breach should be short for strong upward trend");
    }

    @Test
    void testPredictionConfidenceWithMoreData() {
        // Test with more data points
        List<SLAMetric> manyMetrics = createIncreasingTrendMetrics(100);

        PredictionResult prediction = breachPredictor.predict(testSlice, manyMetrics);

        // More data should lead to higher confidence
        assertTrue(prediction.confidence > 0.5,
                "Confidence should be higher with more data points");
    }

    @Test
    void testPredictionWithInsufficientData() {
        // Test with very few data points
        List<SLAMetric> fewMetrics = new ArrayList<>();
        fewMetrics.add(new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 5.0, "ms"));
        fewMetrics.add(new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 6.0, "ms"));

        PredictionResult prediction = breachPredictor.predict(testSlice, fewMetrics);

        // Low confidence with insufficient data
        assertTrue(prediction.confidence < 0.5,
                "Confidence should be low with insufficient data");
    }

    @Test
    void testImminentBreachDetection() {
        // Create metrics that indicate imminent breach
        List<SLAMetric> metrics = createImminentBreachMetrics();

        PredictionResult prediction = breachPredictor.predict(testSlice, metrics);

        assertTrue(prediction.isBreachImminent(),
                "Should detect imminent breach");
        assertTrue(prediction.breachProbability > 0.8,
                "Breach probability should be very high");
        assertTrue(prediction.estimatedTimeToBreach < 10,
                "Time to breach should be very short");
    }

    @Test
    void testCriticalMetricIdentification() {
        // Create metrics where latency is the problem
        List<SLAMetric> metrics = new ArrayList<>();

        // Good throughput
        for (int i = 0; i < 10; i++) {
            SLAMetric m = new SLAMetric(testSlice, SLAMetric.MetricType.THROUGHPUT, 15.0, "Mbps");
            m.timestamp = LocalDateTime.now().minusMinutes(10 - i);
            metrics.add(m);
        }

        // Bad latency (increasing)
        for (int i = 0; i < 10; i++) {
            SLAMetric m = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 10.0 + i * 0.5, "ms");
            m.timestamp = LocalDateTime.now().minusMinutes(10 - i);
            metrics.add(m);
        }

        PredictionResult prediction = breachPredictor.predict(testSlice, metrics);

        assertEquals("LATENCY", prediction.criticalMetric,
                "Should identify latency as the critical metric");
    }

    // Helper methods to create test data

    private List<SLAMetric> createStableMetrics(int count) {
        List<SLAMetric> metrics = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SLAMetric m = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, 5.0, "ms");
            m.timestamp = LocalDateTime.now().minusMinutes(count - i);
            metrics.add(m);
        }
        return metrics;
    }

    private List<SLAMetric> createIncreasingTrendMetrics(int count) {
        List<SLAMetric> metrics = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Latency increases from 5 to 15 over time
            double value = 5.0 + (i * 10.0 / count);
            SLAMetric m = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, value, "ms");
            m.timestamp = LocalDateTime.now().minusMinutes(count - i);
            metrics.add(m);
        }
        return metrics;
    }

    private List<SLAMetric> createImminentBreachMetrics() {
        List<SLAMetric> metrics = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            // Latency very close to threshold (19+)
            double value = 19.0 + (i * 0.2);
            SLAMetric m = new SLAMetric(testSlice, SLAMetric.MetricType.LATENCY, value, "ms");
            m.timestamp = LocalDateTime.now().minusMinutes(10 - i);
            metrics.add(m);
        }
        return metrics;
    }
}
