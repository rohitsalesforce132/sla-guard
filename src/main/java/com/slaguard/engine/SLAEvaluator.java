package com.slaguard.engine;

import com.slaguard.model.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SLAEvaluator {

    /**
     * Evaluate all metrics for a slice against its SLA
     */
    public EvaluationResult evaluate(NetworkSlice slice, List<SLAMetric> metrics) {
        EvaluationResult result = new EvaluationResult();
        result.sliceId = slice.sliceId;
        result.sliceName = slice.name;
        result.evaluatedAt = java.time.LocalDateTime.now();

        Map<SLAMetric.MetricType, SLAMetric> latestByType = new HashMap<>();
        for (SLAMetric metric : metrics) {
            latestByType.put(metric.metricType, metric);
        }

        SLA sla = slice.sla;
        if (sla == null) {
            result.overallStatus = SLAMetric.SLAStatus.COMPLIANT;
            return result;
        }

        // Evaluate each metric type
        evaluateLatency(sla, latestByType.get(SLAMetric.MetricType.LATENCY), result);
        evaluateThroughput(sla, latestByType.get(SLAMetric.MetricType.THROUGHPUT), result);
        evaluateJitter(sla, latestByType.get(SLAMetric.MetricType.JITTER), result);
        evaluatePacketLoss(sla, latestByType.get(SLAMetric.MetricType.PACKET_LOSS), result);
        evaluateAvailability(sla, latestByType.get(SLAMetric.MetricType.AVAILABILITY), result);

        // Determine overall status
        result.overallStatus = determineOverallStatus(result);

        Log.debugf("Slice %s evaluation: %s", slice.sliceId, result.overallStatus);

        return result;
    }

    /**
     * Evaluate a single metric against SLA
     */
    public void evaluateMetric(NetworkSlice slice, SLAMetric metric) {
        SLA sla = slice.sla;
        if (sla == null) {
            metric.status = SLAMetric.SLAStatus.COMPLIANT;
            return;
        }

        switch (metric.metricType) {
            case LATENCY:
                evaluateLatency(sla, metric, null);
                break;
            case THROUGHPUT:
                evaluateThroughput(sla, metric, null);
                break;
            case JITTER:
                evaluateJitter(sla, metric, null);
                break;
            case PACKET_LOSS:
                evaluatePacketLoss(sla, metric, null);
                break;
            case AVAILABILITY:
                evaluateAvailability(sla, metric, null);
                break;
        }
    }

    private void evaluateLatency(SLA sla, SLAMetric metric, EvaluationResult result) {
        if (metric == null) return;

        metric.slaTarget = sla.latencyTargetMs;
        metric.warningThreshold = sla.latencyWarningMs;
        metric.criticalThreshold = sla.latencyCriticalMs;

        if (metric.value <= sla.latencyTargetMs) {
            metric.status = SLAMetric.SLAStatus.COMPLIANT;
        } else if (metric.value <= sla.latencyWarningMs) {
            metric.status = SLAMetric.SLAStatus.WARNING;
        } else if (metric.value <= sla.latencyCriticalMs) {
            metric.status = SLAMetric.SLAStatus.CRITICAL;
        } else {
            metric.status = SLAMetric.SLAStatus.BREACHED;
        }

        metric.deviationFromTarget = ((metric.value - sla.latencyTargetMs) / sla.latencyTargetMs) * 100;

        if (result != null) {
            result.latencyStatus = metric.status;
            result.latencyValue = metric.value;
            result.latencyDeviation = metric.deviationFromTarget;
        }
    }

    private void evaluateThroughput(SLA sla, SLAMetric metric, EvaluationResult result) {
        if (metric == null) return;

        metric.slaTarget = sla.throughputTargetMbps;
        metric.warningThreshold = sla.throughputWarningMbps;
        metric.criticalThreshold = sla.throughputCriticalMbps;

        if (metric.value >= sla.throughputTargetMbps) {
            metric.status = SLAMetric.SLAStatus.COMPLIANT;
        } else if (metric.value >= sla.throughputWarningMbps) {
            metric.status = SLAMetric.SLAStatus.WARNING;
        } else if (metric.value >= sla.throughputCriticalMbps) {
            metric.status = SLAMetric.SLAStatus.CRITICAL;
        } else {
            metric.status = SLAMetric.SLAStatus.BREACHED;
        }

        metric.deviationFromTarget = ((sla.throughputTargetMbps - metric.value) / sla.throughputTargetMbps) * 100;

        if (result != null) {
            result.throughputStatus = metric.status;
            result.throughputValue = metric.value;
            result.throughputDeviation = metric.deviationFromTarget;
        }
    }

    private void evaluateJitter(SLA sla, SLAMetric metric, EvaluationResult result) {
        if (metric == null || sla.jitterTargetMs == null) return;

        metric.slaTarget = sla.jitterTargetMs;
        metric.warningThreshold = sla.jitterWarningMs;
        metric.criticalThreshold = sla.jitterCriticalMs;

        if (metric.value <= sla.jitterTargetMs) {
            metric.status = SLAMetric.SLAStatus.COMPLIANT;
        } else if (metric.value <= sla.jitterWarningMs) {
            metric.status = SLAMetric.SLAStatus.WARNING;
        } else if (metric.value <= sla.jitterCriticalMs) {
            metric.status = SLAMetric.SLAStatus.CRITICAL;
        } else {
            metric.status = SLAMetric.SLAStatus.BREACHED;
        }

        metric.deviationFromTarget = ((metric.value - sla.jitterTargetMs) / sla.jitterTargetMs) * 100;

        if (result != null) {
            result.jitterStatus = metric.status;
            result.jitterValue = metric.value;
            result.jitterDeviation = metric.deviationFromTarget;
        }
    }

    private void evaluatePacketLoss(SLA sla, SLAMetric metric, EvaluationResult result) {
        if (metric == null || sla.packetLossTargetPercent == null) return;

        metric.slaTarget = sla.packetLossTargetPercent;
        metric.warningThreshold = sla.packetLossWarningPercent;
        metric.criticalThreshold = sla.packetLossCriticalPercent;

        if (metric.value <= sla.packetLossTargetPercent) {
            metric.status = SLAMetric.SLAStatus.COMPLIANT;
        } else if (metric.value <= sla.packetLossWarningPercent) {
            metric.status = SLAMetric.SLAStatus.WARNING;
        } else if (metric.value <= sla.packetLossCriticalPercent) {
            metric.status = SLAMetric.SLAStatus.CRITICAL;
        } else {
            metric.status = SLAMetric.SLAStatus.BREACHED;
        }

        metric.deviationFromTarget = ((metric.value - sla.packetLossTargetPercent) / sla.packetLossTargetPercent) * 100;

        if (result != null) {
            result.packetLossStatus = metric.status;
            result.packetLossValue = metric.value;
            result.packetLossDeviation = metric.deviationFromTarget;
        }
    }

    private void evaluateAvailability(SLA sla, SLAMetric metric, EvaluationResult result) {
        if (metric == null || sla.availabilityTargetPercent == null) return;

        metric.slaTarget = sla.availabilityTargetPercent;
        metric.warningThreshold = sla.availabilityWarningPercent;
        metric.criticalThreshold = sla.availabilityCriticalPercent;

        if (metric.value >= sla.availabilityTargetPercent) {
            metric.status = SLAMetric.SLAStatus.COMPLIANT;
        } else if (metric.value >= sla.availabilityWarningPercent) {
            metric.status = SLAMetric.SLAStatus.WARNING;
        } else if (metric.value >= sla.availabilityCriticalPercent) {
            metric.status = SLAMetric.SLAStatus.CRITICAL;
        } else {
            metric.status = SLAMetric.SLAStatus.BREACHED;
        }

        metric.deviationFromTarget = ((sla.availabilityTargetPercent - metric.value) / sla.availabilityTargetPercent) * 100;

        if (result != null) {
            result.availabilityStatus = metric.status;
            result.availabilityValue = metric.value;
            result.availabilityDeviation = metric.deviationFromTarget;
        }
    }

    private SLAMetric.SLAStatus determineOverallStatus(EvaluationResult result) {
        // If any metric is BREACHED, overall is BREACHED
        if (result.latencyStatus == SLAMetric.SLAStatus.BREACHED ||
            result.throughputStatus == SLAMetric.SLAStatus.BREACHED ||
            result.jitterStatus == SLAMetric.SLAStatus.BREACHED ||
            result.packetLossStatus == SLAMetric.SLAStatus.BREACHED ||
            result.availabilityStatus == SLAMetric.SLAStatus.BREACHED) {
            return SLAMetric.SLAStatus.BREACHED;
        }

        // If any metric is CRITICAL, overall is CRITICAL
        if (result.latencyStatus == SLAMetric.SLAStatus.CRITICAL ||
            result.throughputStatus == SLAMetric.SLAStatus.CRITICAL ||
            result.jitterStatus == SLAMetric.SLAStatus.CRITICAL ||
            result.packetLossStatus == SLAMetric.SLAStatus.CRITICAL ||
            result.availabilityStatus == SLAMetric.SLAStatus.CRITICAL) {
            return SLAMetric.SLAStatus.CRITICAL;
        }

        // If any metric is WARNING, overall is WARNING
        if (result.latencyStatus == SLAMetric.SLAStatus.WARNING ||
            result.throughputStatus == SLAMetric.SLAStatus.WARNING ||
            result.jitterStatus == SLAMetric.SLAStatus.WARNING ||
            result.packetLossStatus == SLAMetric.SLAStatus.WARNING ||
            result.availabilityStatus == SLAMetric.SLAStatus.WARNING) {
            return SLAMetric.SLAStatus.WARNING;
        }

        return SLAMetric.SLAStatus.COMPLIANT;
    }

    public static class EvaluationResult {
        public String sliceId;
        public String sliceName;
        public SLAMetric.SLAStatus overallStatus;
        public java.time.LocalDateTime evaluatedAt;

        public SLAMetric.SLAStatus latencyStatus;
        public Double latencyValue;
        public Double latencyDeviation;

        public SLAMetric.SLAStatus throughputStatus;
        public Double throughputValue;
        public Double throughputDeviation;

        public SLAMetric.SLAStatus jitterStatus;
        public Double jitterValue;
        public Double jitterDeviation;

        public SLAMetric.SLAStatus packetLossStatus;
        public Double packetLossValue;
        public Double packetLossDeviation;

        public SLAMetric.SLAStatus availabilityStatus;
        public Double availabilityValue;
        public Double availabilityDeviation;
    }
}
