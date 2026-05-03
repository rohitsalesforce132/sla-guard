package com.slaguard;

import com.slaguard.intent.IntentParser;
import com.slaguard.model.NetworkSlice;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class IntentParserTest {

    @Inject
    IntentParser intentParser;

    private NetworkSlice testSlice;

    @BeforeEach
    void setUp() {
        testSlice = new NetworkSlice();
        testSlice.sliceId = "TEST-SLICE-005";
        testSlice.name = "Test Intent Slice";
        testSlice.sliceType = NetworkSlice.SliceType.ENTERPRISE;
        testSlice.status = NetworkSlice.SliceStatus.ACTIVE;
    }

    @Test
    void testParseLatencyIntent() {
        String intent = "Ensure latency stays below 5ms for this slice";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success, "Parsing should succeed");
        assertNotNull(result.slaDefinition, "SLA definition should not be null");
        assertEquals(5.0, result.slaDefinition.latencyTargetMs, 0.1,
                "Latency target should be extracted correctly");
    }

    @Test
    void testParseThroughputIntent() {
        String intent = "Maintain throughput of at least 100 Mbps";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success);
        assertEquals(100.0, result.slaDefinition.throughputTargetMbps, 0.1);
    }

    @Test
    void testParseAvailabilityIntent() {
        String intent = "Ensure 99.99% availability at all times";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success);
        assertEquals(99.99, result.slaDefinition.availabilityTargetPercent, 0.01);
    }

    @Test
    void testParseComplexIntent() {
        String intent = "Ensure my enterprise slice maintains latency below 5ms with 99.99% availability";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success);
        assertNotNull(result.slaDefinition);
        assertEquals(5.0, result.slaDefinition.latencyTargetMs, 0.1);
        assertEquals(99.99, result.slaDefinition.availabilityTargetPercent, 0.01);
    }

    @Test
    void testParseIntentWithTimeWindow() {
        String intent = "Boost QoS for the IoT slice during peak hours (9am-5pm) if throughput drops below 10Mbps";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success);
        assertTrue(result.extractedConditions.containsKey("timeWindow"),
                "Should extract time window condition");
    }

    @Test
    void testParsePriorityIntent() {
        String intent = "Prioritize emergency services slice over all others during incidents";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success);
        assertTrue(result.extractedConditions.containsKey("priorityAction"),
                "Should extract priority condition");
    }

    @Test
    void testParseEmergencySliceType() {
        String intent = "This is an emergency services slice with ultra-low latency requirements";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success);
        assertEquals("EMERGENCY", result.slaDefinition.sliceType,
                "Should infer EMERGENCY slice type");
        assertEquals("EMERGENCY", result.slaDefinition.priority,
                "Should set EMERGENCY priority");
    }

    @Test
    void testParseEnterpriseSliceType() {
        String intent = "Configure this enterprise network slice for business applications";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success);
        assertEquals("ENTERPRISE", result.slaDefinition.sliceType);
    }

    @Test
    void testParseIoTSliceType() {
        String intent = "Set up this IoT slice for sensor data collection";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success);
        assertEquals("IOT", result.slaDefinition.sliceType);
    }

    @Test
    void testParseConditionalIntent() {
        String intent = "If latency exceeds 10ms, trigger automatic QoS boost";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success);
        assertTrue(result.extractedConditions.containsKey("trigger"),
                "Should extract trigger condition");
    }

    @Test
    void testCreateIntentFromNaturalLanguage() {
        String naturalLanguage = "Ensure my enterprise slice maintains latency below 5ms with 99.99% availability";

        com.slaguard.model.Intent intent = intentParser.createIntent(naturalLanguage, testSlice);

        assertNotNull(intent, "Intent should be created");
        assertEquals(naturalLanguage, intent.naturalLanguageDefinition,
                "Natural language definition should match");
        assertNotNull(intent.parsedSLADefinition, "Parsed SLA definition should be set");
        assertEquals(com.slaguard.model.Intent.IntentStatus.ACTIVE, intent.status,
                "Intent status should be ACTIVE");
    }

    @Test
    void testFallbackToRuleBasedParsing() {
        // Even without AI API, rule-based parsing should work
        String intent = "Maintain latency below 8ms";

        IntentParser.IntentParseResult result = intentParser.parse(intent);

        assertTrue(result.success, "Rule-based parsing should succeed");
        assertEquals(8.0, result.slaDefinition.latencyTargetMs, 0.1);
        assertEquals("RULES", result.parsingMethod, "Should use rule-based method");
    }
}
