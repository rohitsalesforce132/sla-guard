package com.slaguard.intent;

import com.slaguard.model.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Intent Explainer - Explains current enforcement status in plain text
 */
@ApplicationScoped
public class IntentExplainer {

    /**
     * Generate a plain text explanation of current intent enforcement status
     */
    public String explainEnforcementStatus(Intent intent) {
        StringBuilder explanation = new StringBuilder();

        explanation.append("📋 **Intent Status**\n\n");
        explanation.append("**Original Intent:**\n> ").append(intent.naturalLanguageDefinition).append("\n\n");

        explanation.append("**Current Status:** ").append(intent.status).append("\n");
        if (intent.lastEnforcedAt != null) {
            explanation.append("**Last Enforced:** ").append(intent.lastEnforcedAt).append("\n");
        }
        explanation.append("**Enforcement Count:** ").append(intent.enforcementCount).append("\n\n");

        // Explain SLA
        if (intent.slice != null && intent.slice.sla != null) {
            explanation.append("**Enforced SLA Parameters:**\n");
            SLA sla = intent.slice.sla;
            if (sla.latencyTargetMs != null) {
                explanation.append("• Latency: ").append(sla.latencyTargetMs).append("ms (target)\n");
            }
            if (sla.throughputTargetMbps != null) {
                explanation.append("• Throughput: ").append(sla.throughputTargetMbps).append("Mbps (target)\n");
            }
            if (sla.availabilityTargetPercent != null) {
                explanation.append("• Availability: ").append(sla.availabilityTargetPercent).append("% (target)\n");
            }
            explanation.append("\n");
        }

        // Explain conditions
        if (intent.extractedConditions != null && !intent.extractedConditions.isEmpty()) {
            explanation.append("**Extracted Conditions:**\n");
            for (Map.Entry<String, String> entry : intent.extractedConditions.entrySet()) {
                explanation.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            explanation.append("\n");
        }

        return explanation.toString();
    }

    /**
     * Generate a plain text explanation of current slice health
     */
    public String explainSliceHealth(NetworkSlice slice) {
        StringBuilder explanation = new StringBuilder();

        explanation.append("🏥 **Slice Health Report**\n\n");
        explanation.append("**Slice:** ").append(slice.name).append(" (").append(slice.sliceId).append(")\n");
        explanation.append("**Type:** ").append(slice.sliceType).append("\n");
        explanation.append("**Status:** ").append(slice.status).append("\n\n");

        // Get latest metrics
        List<SLAMetric> metrics = SLAMetric.list("slice = ?1 ORDER BY timestamp DESC", slice);

        if (!metrics.isEmpty()) {
            explanation.append("**Current Metrics:**\n");

            for (SLAMetric.MetricType type : SLAMetric.MetricType.values()) {
                SLAMetric metric = metrics.stream()
                        .filter(m -> m.metricType == type)
                        .findFirst()
                        .orElse(null);

                if (metric != null) {
                    String statusEmoji = getStatusEmoji(metric.status);
                    explanation.append(String.format("%s **%s:** %.2f %s - %s\n",
                            statusEmoji,
                            type,
                            metric.value,
                            metric.unit != null ? metric.unit : "",
                            metric.status));
                }
            }
            explanation.append("\n");
        }

        // Get recent breaches
        List<SLABreach> breaches = SLABreach.list(
                "slice = ?1 ORDER BY detectedAt DESC",
                slice);

        if (!breaches.isEmpty()) {
            explanation.append("**Recent Breaches:**\n");
            for (SLABreach breach : breaches.subList(0, Math.min(5, breaches.size()))) {
                explanation.append(String.format("• [%s] %s breach detected at %s - Severity: %s\n",
                        breach.breachId.substring(0, 8),
                        breach.metricType,
                        breach.detectedAt,
                        breach.severity));
            }
            explanation.append("\n");
        }

        // Get active remediations
        List<RemediationAction> remediations = RemediationAction.list(
                "slice = ?1 and status = ?2",
                slice, RemediationAction.ActionStatus.IN_PROGRESS);

        if (!remediations.isEmpty()) {
            explanation.append("**Active Remediations:**\n");
            for (RemediationAction action : remediations) {
                explanation.append(String.format("• [%s] %s - %s\n",
                        action.actionId.substring(0, 8),
                        action.type,
                        action.description));
            }
        }

        return explanation.toString();
    }

    /**
     * Explain a specific SLA breach in plain text
     */
    public String explainBreach(SLABreach breach) {
        StringBuilder explanation = new StringBuilder();

        explanation.append("⚠️ **SLA Breach Alert**\n\n");
        explanation.append("**Breach ID:** ").append(breach.breachId).append("\n");
        explanation.append("**Slice:** ").append(breach.slice.name).append("\n");
        explanation.append("**Metric:** ").append(breach.metricType).append("\n");
        explanation.append("**Severity:** ").append(breach.severity).append("\n\n");

        explanation.append("**Values:**\n");
        explanation.append("• Actual: ").append(breach.actualValue).append("\n");
        explanation.append("• Threshold: ").append(breach.thresholdValue).append("\n\n");

        explanation.append("**Detected At:** ").append(breach.detectedAt).append("\n");

        if (breach.rootCauseAnalysis != null) {
            explanation.append("\n**Root Cause Analysis:**\n").append(breach.rootCauseAnalysis).append("\n");
        }

        if (breach.remediationAction != null) {
            explanation.append("\n**Remediation:**\n");
            explanation.append("• Type: ").append(breach.remediationAction.type).append("\n");
            explanation.append("• Status: ").append(breach.remediationAction.status).append("\n");
            if (breach.remediationAction.externalReference != null) {
                explanation.append("• Reference: ").append(breach.remediationAction.externalReference).append("\n");
            }
        }

        if (breach.predictedMinutesBeforeBreach != null) {
            explanation.append("\n**Prediction:**\n");
            explanation.append("• Predicted ").append(breach.predictedMinutesBeforeBreach)
                    .append(" minutes before breach\n");
            explanation.append("• Confidence: ").append(String.format("%.1f%%", breach.predictionConfidence * 100)).append("\n");
        }

        return explanation.toString();
    }

    /**
     * Explain a prediction result in plain text
     */
    public String explainPrediction(com.slaguard.prediction.PredictionResult prediction) {
        StringBuilder explanation = new StringBuilder();

        explanation.append("🔮 **Breach Prediction**\n\n");
        explanation.append("**Slice:** ").append(prediction.sliceId).append("\n");
        explanation.append("**Predicted At:** ").append(prediction.predictedAt).append("\n\n");

        explanation.append("**Breach Probability:** ")
                .append(String.format("%.1f%%", prediction.breachProbability * 100)).append("\n");

        if (prediction.estimatedTimeToBreach < Integer.MAX_VALUE) {
            explanation.append("**Estimated Time to Breach:** ")
                    .append(prediction.estimatedTimeToBreach).append(" minutes\n");
        }

        explanation.append("**Confidence:** ")
                .append(String.format("%.1f%%", prediction.confidence * 100)).append("\n");

        if (prediction.criticalMetric != null) {
            explanation.append("**Critical Metric:** ").append(prediction.criticalMetric).append("\n");
        }

        if (prediction.trendDirection != null) {
            explanation.append("**Trend:** ").append(prediction.trendDirection).append("\n");
        }

        explanation.append("\n**Summary:** ").append(prediction.getSummary()).append("\n");

        return explanation.toString();
    }

    private String getStatusEmoji(SLAMetric.SLAStatus status) {
        switch (status) {
            case COMPLIANT: return "✅";
            case WARNING: return "⚠️";
            case CRITICAL: return "🔴";
            case BREACHED: return "💥";
            default: return "❓";
        }
    }
}
