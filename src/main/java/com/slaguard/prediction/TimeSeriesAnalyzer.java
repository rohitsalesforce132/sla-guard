package com.slaguard.prediction;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLAMetric;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TimeSeriesAnalyzer {

    @Inject
    PredictionModel predictionModel;

    /**
     * Analyze time series trend for a specific metric
     */
    public TrendAnalysis analyzeTrend(NetworkSlice slice, SLAMetric.MetricType metricType,
                                      List<SLAMetric> metrics) {
        TrendAnalysis analysis = new TrendAnalysis();
        analysis.metricType = metricType;
        analysis.sliceId = slice.sliceId;

        if (metrics.isEmpty()) {
            analysis.breachProbability = 0.0;
            analysis.estimatedTimeToBreach = Integer.MAX_VALUE;
            analysis.trendDirection = TrendDirection.STABLE;
            analysis.confidence = 0.0;
            return analysis;
        }

        // Sort by timestamp
        List<Double> values = metrics.stream()
                .sorted((a, b) -> a.timestamp.compareTo(b.timestamp))
                .map(m -> m.value)
                .toList();

        if (values.size() < 2) {
            analysis.breachProbability = 0.0;
            analysis.estimatedTimeToBreach = Integer.MAX_VALUE;
            analysis.trendDirection = TrendDirection.STABLE;
            analysis.confidence = 0.0;
            return analysis;
        }

        // Calculate EMA (Exponential Moving Average)
        double ema = calculateEMA(values, predictionModel.getEmaAlpha());
        double emaTrend = calculateEMATrend(values, predictionModel.getEmaAlpha());

        // Calculate volatility (standard deviation)
        double volatility = calculateVolatility(values);
        analysis.volatility = volatility;

        // Determine trend direction
        analysis.trendDirection = determineTrendDirection(emaTrend, volatility);

        // Calculate breach probability based on trend and thresholds
        analysis.breachProbability = calculateBreachProbability(slice, metricType, ema, emaTrend, volatility);

        // Estimate time to breach
        analysis.estimatedTimeToBreach = estimateTimeToBreach(slice, metricType, ema, emaTrend);

        // Calculate confidence
        analysis.confidence = calculateConfidence(values.size(), volatility, analysis.breachProbability);

        Log.debugf("Trend analysis for %s/%s: EMA=%.2f, trend=%.4f, volatility=%.2f, breachProb=%.2f",
                slice.sliceId, metricType, ema, emaTrend, volatility, analysis.breachProbability);

        return analysis;
    }

    /**
     * Calculate Exponential Moving Average
     */
    private double calculateEMA(List<Double> values, double alpha) {
        double ema = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            ema = alpha * values.get(i) + (1 - alpha) * ema;
        }
        return ema;
    }

    /**
     * Calculate EMA trend (derivative)
     */
    private double calculateEMATrend(List<Double> values, double alpha) {
        List<Double> emaValues = new ArrayList<>();
        double ema = values.get(0);
        emaValues.add(ema);

        for (int i = 1; i < values.size(); i++) {
            ema = alpha * values.get(i) + (1 - alpha) * ema;
            emaValues.add(ema);
        }

        // Simple linear trend on EMA values
        if (emaValues.size() < 2) {
            return 0.0;
        }

        int n = emaValues.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += emaValues.get(i);
            sumXY += i * emaValues.get(i);
            sumX2 += i * i;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }

    /**
     * Calculate volatility (standard deviation)
     */
    private double calculateVolatility(List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    /**
     * Determine trend direction
     */
    private TrendDirection determineTrendDirection(double trend, double volatility) {
        // Normalize trend by volatility to account for noise
        double normalizedTrend = trend / (volatility + 0.001);

        if (normalizedTrend > 0.5) {
            return TrendDirection.INCREASING;
        } else if (normalizedTrend < -0.5) {
            return TrendDirection.DECREASING;
        } else {
            return TrendDirection.STABLE;
        }
    }

    /**
     * Calculate breach probability
     */
    private double calculateBreachProbability(NetworkSlice slice, SLAMetric.MetricType metricType,
                                              double ema, double trend, double volatility) {
        SLA sla = slice.sla;
        if (sla == null) {
            return 0.0;
        }

        double threshold = 0.0;
        double warningThreshold = 0.0;
        boolean increasingBreach = false; // True if breach occurs when value increases (e.g., latency)

        switch (metricType) {
            case LATENCY:
                threshold = sla.latencyCriticalMs;
                warningThreshold = sla.latencyWarningMs;
                increasingBreach = true;
                break;
            case THROUGHPUT:
                threshold = sla.throughputCriticalMbps;
                warningThreshold = sla.throughputWarningMbps;
                increasingBreach = false; // Breach when throughput decreases
                break;
            case JITTER:
                threshold = sla.jitterCriticalMs != null ? sla.jitterCriticalMs : 0.0;
                warningThreshold = sla.jitterWarningMs != null ? sla.jitterWarningMs : 0.0;
                increasingBreach = true;
                break;
            case PACKET_LOSS:
                threshold = sla.packetLossCriticalPercent != null ? sla.packetLossCriticalPercent : 0.0;
                warningThreshold = sla.packetLossWarningPercent != null ? sla.packetLossWarningPercent : 0.0;
                increasingBreach = true;
                break;
            case AVAILABILITY:
                threshold = sla.availabilityCriticalPercent != null ? sla.availabilityCriticalPercent : 0.0;
                warningThreshold = sla.availabilityWarningPercent != null ? sla.availabilityWarningPercent : 0.0;
                increasingBreach = false; // Breach when availability decreases
                break;
        }

        if (threshold == 0.0) {
            return 0.0;
        }

        // Calculate distance to thresholds
        double distanceToWarning = increasingBreach ?
                (warningThreshold - ema) : (ema - warningThreshold);
        double distanceToCritical = increasingBreach ?
                (threshold - ema) : (ema - threshold);

        // If already in breach zone
        if (distanceToCritical <= 0) {
            return 1.0;
        }

        // If in warning zone
        if (distanceToWarning <= 0) {
            return 0.7 + (1.0 - (distanceToCritical / (threshold - warningThreshold))) * 0.3;
        }

        // Calculate probability based on trend and distance
        double trendFactor = increasingBreach ? Math.max(0, trend) : Math.max(0, -trend);
        double distanceFactor = 1.0 - Math.min(1.0, distanceToWarning / (warningThreshold * 0.5));

        // Combine factors
        double probability = 0.3 * distanceFactor + 0.4 * trendFactor + 0.3 * (volatility / threshold);

        return Math.max(0.0, Math.min(1.0, probability));
    }

    /**
     * Estimate time to breach in minutes
     */
    private int estimateTimeToBreach(NetworkSlice slice, SLAMetric.MetricType metricType,
                                     double ema, double trend) {
        SLA sla = slice.sla;
        if (sla == null || trend == 0) {
            return Integer.MAX_VALUE;
        }

        double threshold = 0.0;
        boolean increasingBreach = true;

        switch (metricType) {
            case LATENCY:
                threshold = sla.latencyCriticalMs;
                increasingBreach = true;
                break;
            case THROUGHPUT:
                threshold = sla.throughputCriticalMbps;
                increasingBreach = false;
                break;
            case JITTER:
                threshold = sla.jitterCriticalMs != null ? sla.jitterCriticalMs : 0.0;
                increasingBreach = true;
                break;
            case PACKET_LOSS:
                threshold = sla.packetLossCriticalPercent != null ? sla.packetLossCriticalPercent : 0.0;
                increasingBreach = true;
                break;
            case AVAILABILITY:
                threshold = sla.availabilityCriticalPercent != null ? sla.availabilityCriticalPercent : 0.0;
                increasingBreach = false;
                break;
        }

        if (threshold == 0.0) {
            return Integer.MAX_VALUE;
        }

        double distance = increasingBreach ? (threshold - ema) : (ema - threshold);

        if (distance <= 0) {
            return 0; // Already in breach
        }

        if (increasingBreach && trend <= 0) {
            return Integer.MAX_VALUE; // Trend is opposite direction
        }

        if (!increasingBreach && trend >= 0) {
            return Integer.MAX_VALUE; // Trend is opposite direction
        }

        // Estimate time = distance / |trend| (assuming trend is per minute)
        // For safety, divide by 2 to be conservative
        double effectiveTrend = increasingBreach ? trend : -trend;
        double minutesToBreach = (distance / effectiveTrend) / 2.0;

        return (int) Math.max(1, Math.min(Integer.MAX_VALUE - 1, minutesToBreach));
    }

    /**
     * Calculate confidence score
     */
    private double calculateConfidence(int dataPoints, double volatility, double breachProbability) {
        // More data points = higher confidence
        double dataScore = Math.min(1.0, dataPoints / 50.0);

        // Lower volatility = higher confidence
        double volatilityScore = Math.max(0.0, 1.0 - volatility / 10.0);

        // Higher breach probability = higher confidence (less likely to be noise)
        double probabilityScore = breachProbability > 0.3 ? 1.0 : breachProbability / 0.3;

        return (dataScore + volatilityScore + probabilityScore) / 3.0;
    }

    public static class TrendAnalysis {
        public String sliceId;
        public SLAMetric.MetricType metricType;
        public double breachProbability;
        public int estimatedTimeToBreach;
        public double confidence;
        public TrendDirection trendDirection;
        public double volatility;
    }

    public enum TrendDirection {
        INCREASING,  // Values going up (worse for latency, jitter, packet loss)
        DECREASING,  // Values going down (worse for throughput, availability)
        STABLE       // No clear trend
    }
}
