package com.slaguard.intent;

import com.slaguard.engine.SLAEvaluator;
import com.slaguard.engine.SLAMonitoringEngine;
import com.slaguard.model.Intent;
import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLA;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * Intent Enforcer - Monitors and enforces intent-derived SLAs
 */
@ApplicationScoped
public class IntentEnforcer {

    @Inject
    SLAMonitoringEngine monitoringEngine;

    @Inject
    SLAEvaluator slaEvaluator;

    /**
     * Enforce an intent by creating the SLA and starting monitoring
     */
    @Transactional
    public void enforceIntent(Intent intent) {
        Log.infof("Enforcing intent %s for slice %s", intent.intentId, intent.slice.sliceId);

        try {
            // Parse the SLA definition from the intent
            IntentParser.IntentParseResult result = IntentParser.this.parse(intent.naturalLanguageDefinition);

            if (!result.success || result.slaDefinition == null) {
                intent.status = Intent.IntentStatus.FAILED;
                intent.errorMessage = "Failed to parse intent or no SLA definition found";
                intent.persist();
                Log.errorf("Intent enforcement failed: %s", intent.errorMessage);
                return;
            }

            // Create or update the SLA
            SLA sla = createOrUpdateSLA(intent.slice, result.slaDefinition);

            if (sla != null) {
                intent.status = Intent.IntentStatus.ENFORCED;
                intent.lastEnforcedAt = LocalDateTime.now();
                intent.enforcementCount = (intent.enforcementCount != null ? intent.enforcementCount : 0) + 1;
                intent.persist();

                Log.infof("Intent %s enforced successfully. SLA %s created for slice %s",
                        intent.intentId, sla.slaId, intent.slice.sliceId);

            } else {
                intent.status = Intent.IntentStatus.FAILED;
                intent.errorMessage = "Failed to create SLA from intent";
                intent.persist();
            }

        } catch (Exception e) {
            Log.errorf("Error enforcing intent: %s", e.getMessage(), e);
            intent.status = Intent.IntentStatus.FAILED;
            intent.errorMessage = e.getMessage();
            intent.persist();
        }
    }

    /**
     * Create or update an SLA from the parsed intent definition
     */
    @Transactional
    private SLA createOrUpdateSLA(NetworkSlice slice, IntentParser.SLADefinition definition) {
        SLA sla = slice.sla;

        if (sla == null) {
            sla = new SLA();
            sla.slaId = "SLA-" + System.currentTimeMillis();
            slice.sla = sla;
        }

        sla.name = "Intent-Derived SLA for " + slice.name;
        sla.intentDefinition = slice.name + ": " + definition.toString();

        // Set SLA parameters from definition
        if (definition.latencyTargetMs != null) {
            sla.latencyTargetMs = definition.latencyTargetMs;
            sla.latencyWarningMs = definition.latencyTargetMs * 0.8;
            sla.latencyCriticalMs = definition.latencyTargetMs * 0.95;
        }

        if (definition.throughputTargetMbps != null) {
            sla.throughputTargetMbps = definition.throughputTargetMbps;
            sla.throughputWarningMbps = definition.throughputTargetMbps * 0.8;
            sla.throughputCriticalMbps = definition.throughputTargetMbps * 0.95;
        }

        if (definition.jitterTargetMs != null) {
            sla.jitterTargetMs = definition.jitterTargetMs;
            sla.jitterWarningMs = definition.jitterTargetMs * 0.8;
            sla.jitterCriticalMs = definition.jitterTargetMs * 0.95;
        }

        if (definition.packetLossTargetPercent != null) {
            sla.packetLossTargetPercent = definition.packetLossTargetPercent;
            sla.packetLossWarningPercent = definition.packetLossTargetPercent * 0.8;
            sla.packetLossCriticalPercent = definition.packetLossTargetPercent * 0.95;
        }

        if (definition.availabilityTargetPercent != null) {
            sla.availabilityTargetPercent = definition.availabilityTargetPercent;
            sla.availabilityWarningPercent = definition.availabilityTargetPercent * 0.99;
            sla.availabilityCriticalPercent = definition.availabilityTargetPercent * 0.98;
        }

        sla.isActive = true;
        slice.persist();

        return sla;
    }

    /**
     * Check if an intent should be enforced based on time windows
     */
    public boolean shouldEnforceNow(Intent intent) {
        if (intent.extractedConditions == null || !intent.extractedConditions.containsKey("timeWindow")) {
            return true; // No time window constraint
        }

        String timeWindow = intent.extractedConditions.get("timeWindow");
        LocalTime now = LocalTime.now();

        switch (timeWindow.toLowerCase()) {
            case "peak-hours":
                // Assume 9am-5pm as peak hours
                return !now.isBefore(LocalTime.of(9, 0)) && !now.isAfter(LocalTime.of(17, 0));
            case "business-hours":
                return !now.isBefore(LocalTime.of(9, 0)) && !now.isAfter(LocalTime.of(18, 0));
            default:
                return true;
        }
    }

    /**
     * Check if a trigger condition for the intent is met
     */
    public boolean isTriggerConditionMet(Intent intent, NetworkSlice slice) {
        if (intent.extractedConditions == null || !intent.extractedConditions.containsKey("trigger")) {
            return true; // No trigger condition
        }

        String trigger = intent.extractedConditions.get("trigger");
        SLAEvaluator.EvaluationResult evaluation = slaEvaluator.evaluate(slice,
                com.slaguard.model.SLAMetric.list("slice = ?1 ORDER BY timestamp DESC", slice));

        switch (trigger.toLowerCase()) {
            case "metric-drop":
                // Trigger if throughput drops below warning threshold
                return evaluation.throughputStatus == com.slaguard.model.SLAMetric.SLAStatus.WARNING ||
                       evaluation.throughputStatus == com.slaguard.model.SLAMetric.SLAStatus.CRITICAL;
            case "metric-exceed":
                // Trigger if latency exceeds warning threshold
                return evaluation.latencyStatus == com.slaguard.model.SLAMetric.SLAStatus.WARNING ||
                       evaluation.latencyStatus == com.slaguard.model.SLAMetric.SLAStatus.CRITICAL;
            default:
                return true;
        }
    }

    /**
     * Re-evaluate and potentially re-enforce an intent
     */
    @Transactional
    public void reevaluateIntent(Intent intent) {
        if (intent.status != Intent.IntentStatus.ENFORCED &&
            intent.status != Intent.IntentStatus.ACTIVE) {
            return;
        }

        Log.debugf("Re-evaluating intent %s", intent.intentId);

        if (shouldEnforceNow(intent)) {
            if (isTriggerConditionMet(intent, intent.slice)) {
                Log.infof("Trigger condition met for intent %s, enforcing", intent.intentId);
                enforceIntent(intent);
            }
        } else {
            Log.debugf("Intent %s time window not active", intent.intentId);
        }
    }
}
