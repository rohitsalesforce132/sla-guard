package com.slaguard.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slaguard.model.Intent;
import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLA;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intent Parser - Translates natural language SLA definitions into structured SLA objects
 * Uses LangChain4j with OpenAI (or compatible) for natural language understanding
 */
@ApplicationScoped
public class IntentParser {

    @ConfigProperty(name = "sla-guard.intent.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "sla-guard.intent.model", defaultValue = "gpt-4")
    String modelName;

    @ConfigProperty(name = "sla-guard.intent.api-key")
    String apiKey;

    private ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse a natural language intent into an SLA definition
     */
    public IntentParseResult parse(String naturalLanguageIntent) {
        Log.infof("Parsing intent: %s", naturalLanguageIntent);

        IntentParseResult result = new IntentParseResult();
        result.originalIntent = naturalLanguageIntent;

        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            Log.warn("Intent parsing is disabled or no API key configured, using rule-based fallback");
            return parseWithRules(naturalLanguageIntent);
        }

        try {
            // Initialize chat model if needed
            if (chatModel == null) {
                chatModel = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build();
            }

            // Create prompt for intent parsing
            String prompt = buildPrompt(naturalLanguageIntent);

            // Call AI model
            String response = chatModel.generate(prompt);

            // Parse response into SLA definition
            result.slaDefinition = parseAIResponse(response);
            result.extractedConditions = extractConditions(naturalLanguageIntent);
            result.success = true;
            result.parsingMethod = "AI";
            result.modelUsed = modelName;

            Log.infof("Intent parsed successfully: latency=%.2fms, throughput=%.2fMbps, availability=%.2f%%",
                    result.slaDefinition.latencyTargetMs,
                    result.slaDefinition.throughputTargetMbps,
                    result.slaDefinition.availabilityTargetPercent);

        } catch (Exception e) {
            Log.errorf("Error parsing intent with AI, falling back to rules: %s", e.getMessage(), e);
            return parseWithRules(naturalLanguageIntent);
        }

        return result;
    }

    /**
     * Build prompt for AI-based intent parsing
     */
    private String buildPrompt(String intent) {
        return String.format("""
                You are an expert in 5G network slicing and SLA definitions.
                Parse the following natural language intent for an SLA and extract the key parameters.

                Intent: "%s"

                Extract and return ONLY a JSON object with these fields:
                {
                  "latencyTargetMs": number (target latency in milliseconds, or null if not specified),
                  "throughputTargetMbps": number (target throughput in Mbps, or null if not specified),
                  "jitterTargetMs": number (target jitter in milliseconds, or null if not specified),
                  "packetLossTargetPercent": number (target packet loss as percentage, or null if not specified),
                  "availabilityTargetPercent": number (target availability as percentage, or null if not specified),
                  "priority": "EMERGENCY" | "HIGH" | "MEDIUM" | "LOW" (based on context),
                  "sliceType": "EMBB" | "URLLC" | "MMTC" | "ENTERPRISE" | "IOT" | "EMERGENCY" (infer from context),
                  "timeWindows": [{"start": "HH:mm", "end": "HH:mm", "days": "Mon-Fri"}] (any time-based conditions),
                  "conditions": ["condition1", "condition2"] (any other conditions mentioned)
                }

                If a parameter is not mentioned, use null.
                Infer reasonable defaults if values are implied (e.g., "low latency" typically means < 10ms).
                """, intent);
    }

    /**
     * Parse AI response into SLA definition
     */
    private SLADefinition parseAIResponse(String response) {
        try {
            // Extract JSON from response (might be wrapped in markdown code blocks)
            String json = response;
            Pattern pattern = Pattern.compile("```json\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                json = matcher.group(1);
            }

            // Parse JSON
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

            SLADefinition sla = new SLADefinition();

            if (parsed.containsKey("latencyTargetMs")) {
                sla.latencyTargetMs = ((Number) parsed.get("latencyTargetMs")).doubleValue();
            }
            if (parsed.containsKey("throughputTargetMbps")) {
                sla.throughputTargetMbps = ((Number) parsed.get("throughputTargetMbps")).doubleValue();
            }
            if (parsed.containsKey("jitterTargetMs")) {
                sla.jitterTargetMs = ((Number) parsed.get("jitterTargetMs")).doubleValue();
            }
            if (parsed.containsKey("packetLossTargetPercent")) {
                sla.packetLossTargetPercent = ((Number) parsed.get("packetLossTargetPercent")).doubleValue();
            }
            if (parsed.containsKey("availabilityTargetPercent")) {
                sla.availabilityTargetPercent = ((Number) parsed.get("availabilityTargetPercent")).doubleValue();
            }
            if (parsed.containsKey("priority")) {
                sla.priority = (String) parsed.get("priority");
            }
            if (parsed.containsKey("sliceType")) {
                sla.sliceType = (String) parsed.get("sliceType");
            }
            if (parsed.containsKey("timeWindows")) {
                sla.timeWindows = (String) parsed.get("timeWindows").toString();
            }
            if (parsed.containsKey("conditions")) {
                sla.conditions = parsed.get("conditions").toString();
            }

            return sla;

        } catch (Exception e) {
            Log.errorf("Error parsing AI response: %s", e.getMessage(), e);
            return new SLADefinition(); // Return empty definition on error
        }
    }

    /**
     * Fallback rule-based parsing when AI is not available
     */
    private IntentParseResult parseWithRules(String intent) {
        IntentParseResult result = new IntentParseResult();
        result.originalIntent = intent;
        result.parsingMethod = "RULES";
        result.success = true;

        SLADefinition sla = new SLADefinition();

        // Extract latency
        Pattern latencyPattern = Pattern.compile("(?:latency|delay)[^0-9]*(\\d+(?:\\.\\d+)?)\\s*(?:ms|milliseconds)?", Pattern.CASE_INSENSITIVE);
        Matcher latencyMatcher = latencyPattern.matcher(intent);
        if (latencyMatcher.find()) {
            sla.latencyTargetMs = Double.parseDouble(latencyMatcher.group(1));
        }

        // Extract throughput/bandwidth
        Pattern throughputPattern = Pattern.compile("(?:throughput|bandwidth|speed)[^0-9]*(\\d+(?:\\.\\d+)?)\\s*(?:Mbps|Gbps|mbps|gbps)?", Pattern.CASE_INSENSITIVE);
        Matcher throughputMatcher = throughputPattern.matcher(intent);
        if (throughputMatcher.find()) {
            double value = Double.parseDouble(throughputMatcher.group(1));
            sla.throughputTargetMbps = intent.toLowerCase().contains("gbps") ? value * 1000 : value;
        }

        // Extract availability
        Pattern availabilityPattern = Pattern.compile("(?:availability|uptime)[^0-9]*(\\d+(?:\\.\\d+)?)\\s*(?:%|percent)?", Pattern.CASE_INSENSITIVE);
        Matcher availabilityMatcher = availabilityPattern.matcher(intent);
        if (availabilityMatcher.find()) {
            sla.availabilityTargetPercent = Double.parseDouble(availabilityMatcher.group(1));
        }

        // Infer slice type
        if (intent.toLowerCase().contains("emergency")) {
            sla.sliceType = "EMERGENCY";
            sla.priority = "EMERGENCY";
        } else if (intent.toLowerCase().contains("enterprise") || intent.toLowerCase().contains("business")) {
            sla.sliceType = "ENTERPRISE";
            sla.priority = "HIGH";
        } else if (intent.toLowerCase().contains("iot") || intent.toLowerCase().contains("sensor")) {
            sla.sliceType = "IOT";
            sla.priority = "LOW";
        } else if (intent.toLowerCase().contains("video") || intent.toLowerCase().contains("streaming")) {
            sla.sliceType = "EMBB";
            sla.priority = "MEDIUM";
        } else if (intent.toLowerCase().contains("control") || intent.toLowerCase().contains("critical")) {
            sla.sliceType = "URLLC";
            sla.priority = "HIGH";
        }

        // Extract time windows
        if (intent.toLowerCase().contains("peak hours") || intent.toLowerCase().contains("9am-5pm")) {
            sla.timeWindows = "[{\"start\":\"09:00\",\"end\":\"17:00\",\"days\":\"Mon-Fri\"}]";
        }

        result.slaDefinition = sla;
        result.extractedConditions = new HashMap<>();
        result.extractedConditions.put("sliceType", sla.sliceType);
        result.extractedConditions.put("priority", sla.priority);

        return result;
    }

    /**
     * Extract conditions from intent
     */
    private Map<String, String> extractConditions(String intent) {
        Map<String, String> conditions = new HashMap<>();

        // Check for time-based conditions
        if (intent.toLowerCase().contains("peak hours") || intent.toLowerCase().contains("during peak")) {
            conditions.put("timeWindow", "peak-hours");
        }
        if (intent.toLowerCase().contains("business hours") || intent.toLowerCase().contains("9am-5pm")) {
            conditions.put("timeWindow", "business-hours");
        }

        // Check for conditional triggers
        if (intent.toLowerCase().contains("if") && intent.toLowerCase().contains("drops")) {
            conditions.put("trigger", "metric-drop");
        }
        if (intent.toLowerCase().contains("when") && intent.toLowerCase().contains("exceeds")) {
            conditions.put("trigger", "metric-exceed");
        }

        // Check for priority conditions
        if (intent.toLowerCase().contains("prioritize") || intent.toLowerCase().contains("over all others")) {
            conditions.put("priorityAction", "always-prioritize");
        }

        return conditions;
    }

    /**
     * Create an Intent entity from natural language
     */
    public Intent createIntent(String naturalLanguageIntent, NetworkSlice slice) {
        IntentParseResult result = parse(naturalLanguageIntent);

        Intent intent = new Intent();
        intent.slice = slice;
        intent.naturalLanguageDefinition = naturalLanguageIntent;
        intent.parsedSLADefinition = convertSLAToJson(result.slaDefinition);
        intent.extractedConditions = result.extractedConditions;
        intent.modelUsed = result.modelUsed;

        if (result.success) {
            intent.status = Intent.IntentStatus.ACTIVE;
        } else {
            intent.status = Intent.IntentStatus.FAILED;
            intent.errorMessage = "Failed to parse intent";
        }

        return intent;
    }

    private String convertSLAToJson(SLADefinition sla) {
        try {
            return objectMapper.writeValueAsString(sla);
        } catch (Exception e) {
            return "{}";
        }
    }

    // Data classes

    public static class IntentParseResult {
        public String originalIntent;
        public SLADefinition slaDefinition = new SLADefinition();
        public Map<String, String> extractedConditions = new HashMap<>();
        public boolean success;
        public String parsingMethod;
        public String modelUsed;
    }

    public static class SLADefinition {
        public Double latencyTargetMs;
        public Double throughputTargetMbps;
        public Double jitterTargetMs;
        public Double packetLossTargetPercent;
        public Double availabilityTargetPercent;
        public String priority = "MEDIUM";
        public String sliceType = "EMBB";
        public String timeWindows;
        public String conditions;
    }
}
