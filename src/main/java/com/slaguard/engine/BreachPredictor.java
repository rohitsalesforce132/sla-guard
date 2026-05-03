package com.slaguard.engine;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLAMetric;
import com.slaguard.prediction.PredictionResult;
import com.slaguard.prediction.TimeSeriesAnalyzer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class BreachPredictor {

    @Inject
    TimeSeriesAnalyzer timeSeriesAnalyzer;

    /**
     * Predict whether an SLA breach will occur in the near future
     */
    public PredictionResult predict(NetworkSlice slice, List<SLAMetric> currentMetrics) {
        PredictionResult result = new PredictionResult();
        result.sliceId = slice.sliceId;
        result.predictedAt = LocalDateTime.now();

        if (slice.sla == null || !slice.sla.isActive) {
            result.breachProbability = 0.0;
            result.estimatedTimeToBreach = Integer.MAX_VALUE;
            result.confidence = 0.0;
            return result;
        }

        double maxProbability = 0.0;
        int minTimeToBreach = Integer.MAX_VALUE;
        String criticalMetric = null;

        // Analyze each metric type
        for (SLAMetric.MetricType metricType : SLAMetric.MetricType.values()) {
            com.slaguard.prediction.TimeSeriesAnalyzer.TrendAnalysis analysis =
                timeSeriesAnalyzer.analyzeTrend(slice, metricType, currentMetrics);

            if (analysis.breachProbability > maxProbability) {
                maxProbability = analysis.breachProbability;
                minTimeToBreach = analysis.estimatedTimeToBreach;
                criticalMetric = metricType.name();
            }

            if (analysis.estimatedTimeToBreach < minTimeToBreach && analysis.breachProbability > 0.3) {
                minTimeToBreach = analysis.estimatedTimeToBreach;
            }
        }

        result.breachProbability = maxProbability;
        result.estimatedTimeToBreach = minTimeToBreach;
        result.confidence = calculateConfidence(currentMetrics, maxProbability);
        result.criticalMetric = criticalMetric;

        if (result.breachProbability > 0.5) {
            Log.warnf("Breach prediction for slice %s: probability=%.2f, timeToBreach=%d min, criticalMetric=%s",
                    slice.sliceId, result.breachProbability, result.estimatedTimeToBreach, result.criticalMetric);
        }

        return result;
    }

    /**
     * Calculate confidence in prediction based on data quality
     */
    private double calculateConfidence(List<SLAMetric> metrics, double probability) {
        // Confidence increases with more data points
        double dataQualityScore = Math.min(1.0, metrics.size() / 50.0);

        // Confidence decreases for very low probabilities (noise)
        double probabilityScore = probability > 0.1 ? 1.0 : probability * 10;

        return Math.min(1.0, (dataQualityScore + probabilityScore) / 2.0);
    }

    /**
     * Get detailed prediction for a specific metric
     */
    public PredictionResult predictMetric(NetworkSlice slice, SLAMetric.MetricType metricType,
                                          List<SLAMetric> historicalMetrics) {
        PredictionResult result = new PredictionResult();
        result.sliceId = slice.sliceId;
        result.predictedAt = LocalDateTime.now();

        com.slaguard.prediction.TimeSeriesAnalyzer.TrendAnalysis analysis =
            timeSeriesAnalyzer.analyzeTrend(slice, metricType, historicalMetrics);

        result.breachProbability = analysis.breachProbability;
        result.estimatedTimeToBreach = analysis.estimatedTimeToBreach;
        result.confidence = analysis.confidence;
        result.criticalMetric = metricType.name();
        result.trendDirection = analysis.trendDirection.name();
        result.volatilityScore = analysis.volatility;

        return result;
    }
}
