package com.slaguard.engine;

import com.slaguard.model.*;
import com.slaguard.prediction.BreachPredictor;
import com.slaguard.prediction.PredictionResult;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SLAMonitoringEngine {

    @Inject
    SLAEvaluator slaEvaluator;

    @Inject
    BreachPredictor breachPredictor;

    @Inject
    RemediationEngine remediationEngine;

    @Inject
    SliceHealthCalculator healthCalculator;

    // Cache for recent metrics per slice (sliceId -> list of metrics)
    private final ConcurrentHashMap<String, List<SLAMetric>> metricsCache = new ConcurrentHashMap<>();

    /**
     * Scheduled task that runs every monitoring interval to evaluate all active slices
     */
    @Scheduled(every = "${sla-guard.monitoring.interval-seconds}s", delayed = "${sla-guard.monitoring.interval-seconds}s")
    public void monitorAllSlices() {
        Log.debug("Starting SLA monitoring cycle");

        List<NetworkSlice> activeSlices = NetworkSlice.list("status", NetworkSlice.SliceStatus.ACTIVE);
        Log.info("Monitoring " + activeSlices.size() + " active slices");

        for (NetworkSlice slice : activeSlices) {
            try {
                monitorSlice(slice);
            } catch (Exception e) {
                Log.errorf("Error monitoring slice %s: %s", slice.sliceId, e.getMessage(), e);
            }
        }

        Log.debug("SLA monitoring cycle completed");
    }

    /**
     * Monitor a single slice - evaluate metrics, predict breaches, trigger remediation
     */
    public void monitorSlice(NetworkSlice slice) {
        Log.debugf("Monitoring slice %s (%s)", slice.sliceId, slice.name);

        if (slice.sla == null || !slice.sla.isActive) {
            Log.debugf("Slice %s has no active SLA, skipping", slice.sliceId);
            return;
        }

        // Get latest metrics for this slice
        List<SLAMetric> latestMetrics = getLatestMetrics(slice);
        if (latestMetrics.isEmpty()) {
            Log.warnf("No metrics available for slice %s", slice.sliceId);
            return;
        }

        // Evaluate current SLA status
        SLAEvaluator.EvaluationResult evaluation = slaEvaluator.evaluate(slice, latestMetrics);

        // Update metrics cache for prediction
        updateMetricsCache(slice, latestMetrics);

        // Predict potential breaches
        PredictionResult prediction = breachPredictor.predict(slice, latestMetrics);

        // Log prediction if breach is likely
        if (prediction.breachProbability > 0.5) {
            Log.warnf("Slice %s: Breach prediction - probability=%.2f, timeToBreach=%d min, confidence=%.2f",
                    slice.sliceId,
                    prediction.breachProbability,
                    prediction.estimatedTimeToBreach,
                    prediction.confidence);
        }

        // Check if remediation is needed
        if (needsRemediation(evaluation, prediction)) {
            Log.infof("Triggering remediation for slice %s", slice.sliceId);
            remediationEngine.handleSLABreach(slice, evaluation, prediction);
        }

        // Log health score
        double healthScore = healthCalculator.calculateHealth(slice, latestMetrics);
        Log.debugf("Slice %s health score: %.2f", slice.sliceId, healthScore);
    }

    /**
     * Ingest a new metric for a slice (called by REST API)
     */
    public void ingestMetric(NetworkSlice slice, SLAMetric metric) {
        Log.debugf("Ingesting metric for slice %s: %s = %s %s",
                slice.sliceId, metric.metricType, metric.value, metric.unit);

        // Evaluate against SLA
        if (slice.sla != null && slice.sla.isActive) {
            slaEvaluator.evaluateMetric(slice, metric);
        }

        // Persist the metric
        metric.persist();
    }

    /**
     * Get latest metrics for a slice from database
     */
    private List<SLAMetric> getLatestMetrics(NetworkSlice slice) {
        return SLAMetric.list(
                "slice = ?1 ORDER BY timestamp DESC",
                slice
        ).subList(0, Math.min(10, SLAMetric.count("slice", slice)));
    }

    /**
     * Update metrics cache for prediction
     */
    private void updateMetricsCache(NetworkSlice slice, List<SLAMetric> metrics) {
        List<SLAMetric> cached = metricsCache.computeIfAbsent(slice.sliceId, k -> metrics);
        if (cached.size() > 100) {
            // Keep only last 100 metrics
            cached = cached.subList(cached.size() - 100, cached.size());
            metricsCache.put(slice.sliceId, cached);
        }
    }

    /**
     * Determine if remediation is needed based on current status and prediction
     */
    private boolean needsRemediation(SLAEvaluator.EvaluationResult evaluation, PredictionResult prediction) {
        // Immediate breach
        if (evaluation.overallStatus == SLAMetric.SLAStatus.BREACHED ||
            evaluation.overallStatus == SLAMetric.SLAStatus.CRITICAL) {
            return true;
        }

        // High probability of breach soon
        if (prediction.breachProbability > 0.8 && prediction.estimatedTimeToBreach < 10) {
            return true;
        }

        // Medium probability with short time to breach
        if (prediction.breachProbability > 0.6 && prediction.estimatedTimeToBreach < 5) {
            return true;
        }

        return false;
    }

    /**
     * Get cached metrics for a slice (for prediction)
     */
    public List<SLAMetric> getCachedMetrics(String sliceId) {
        return metricsCache.get(sliceId);
    }
}
