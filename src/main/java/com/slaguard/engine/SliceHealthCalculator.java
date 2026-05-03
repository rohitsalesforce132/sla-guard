package com.slaguard.engine;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLAMetric;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SliceHealthCalculator {

    /**
     * Calculate overall health score for a slice (0-100)
     * Higher score = healthier
     */
    public double calculateHealth(NetworkSlice slice, List<SLAMetric> metrics) {
        if (slice.sla == null || !slice.sla.isActive) {
            return 100.0; // No SLA means full health
        }

        double totalScore = 0.0;
        int metricCount = 0;

        // Score each metric type
        totalScore += scoreMetric(metrics, SLAMetric.MetricType.LATENCY);
        metricCount++;

        totalScore += scoreMetric(metrics, SLAMetric.MetricType.THROUGHPUT);
        metricCount++;

        if (slice.sla.jitterTargetMs != null) {
            totalScore += scoreMetric(metrics, SLAMetric.MetricType.JITTER);
            metricCount++;
        }

        if (slice.sla.packetLossTargetPercent != null) {
            totalScore += scoreMetric(metrics, SLAMetric.MetricType.PACKET_LOSS);
            metricCount++;
        }

        if (slice.sla.availabilityTargetPercent != null) {
            totalScore += scoreMetric(metrics, SLAMetric.MetricType.AVAILABILITY);
            metricCount++;
        }

        return metricCount > 0 ? totalScore / metricCount : 100.0;
    }

    /**
     * Score a single metric type (0-100)
     */
    private double scoreMetric(List<SLAMetric> metrics, SLAMetric.MetricType metricType) {
        SLAMetric metric = findLatestMetric(metrics, metricType);
        if (metric == null) {
            return 100.0; // No data = assume healthy
        }

        if (metric.status == SLAMetric.SLAStatus.COMPLIANT) {
            return 100.0;
        } else if (metric.status == SLAMetric.SLAStatus.WARNING) {
            return 75.0;
        } else if (metric.status == SLAMetric.SLAStatus.CRITICAL) {
            return 50.0;
        } else { // BREACHED
            return 0.0;
        }
    }

    /**
     * Find the latest metric of a specific type
     */
    private SLAMetric findLatestMetric(List<SLAMetric> metrics, SLAMetric.MetricType type) {
        return metrics.stream()
                .filter(m -> m.metricType == type)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get health category based on score
     */
    public HealthCategory getHealthCategory(double healthScore) {
        if (healthScore >= 90) {
            return HealthCategory.EXCELLENT;
        } else if (healthScore >= 75) {
            return HealthCategory.GOOD;
        } else if (healthScore >= 60) {
            return HealthCategory.FAIR;
        } else if (healthScore >= 40) {
            return HealthCategory.POOR;
        } else {
            return HealthCategory.CRITICAL;
        }
    }

    public enum HealthCategory {
        EXCELLENT,  // 90-100
        GOOD,       // 75-89
        FAIR,       // 60-74
        POOR,       // 40-59
        CRITICAL    // 0-39
    }
}
