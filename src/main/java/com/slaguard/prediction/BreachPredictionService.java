package com.slaguard.prediction;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLAMetric;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class BreachPredictionService {

    @Inject
    TimeSeriesAnalyzer timeSeriesAnalyzer;

    /**
     * Get comprehensive breach prediction for a slice
     */
    public PredictionResult predictBreach(NetworkSlice slice) {
        List<SLAMetric> metrics = SLAMetric.list("slice = ?1 ORDER BY timestamp DESC", slice);
        return predictBreach(slice, metrics);
    }

    /**
     * Get comprehensive breach prediction for a slice with provided metrics
     */
    public PredictionResult predictBreach(NetworkSlice slice, List<SLAMetric> metrics) {
        PredictionResult result = new PredictionResult();
        result.sliceId = slice.sliceId;
        result.predictedAt = java.time.LocalDateTime.now();

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
            List<SLAMetric> metricHistory = metrics.stream()
                    .filter(m -> m.metricType == metricType)
                    .limit(100)
                    .toList();

            if (metricHistory.isEmpty()) {
                continue;
            }

            TimeSeriesAnalyzer.TrendAnalysis analysis =
                timeSeriesAnalyzer.analyzeTrend(slice, metricType, metricHistory);

            if (analysis.breachProbability > maxProbability) {
                maxProbability = analysis.breachProbability;
                minTimeToBreach = analysis.estimatedTimeToBreach;
                criticalMetric = metricType.name();
            }

            if (analysis.estimatedTimeToBreach < minTimeToBreach && analysis.breachProbability > 0.3) {
                minTimeToBreach = analysis.estimatedTimeToBreach;
            }

            Log.debugf("Metric %s: breachProb=%.2f, timeToBreach=%d, trend=%s",
                    metricType, analysis.breachProbability, analysis.estimatedTimeToBreach, analysis.trendDirection);
        }

        result.breachProbability = maxProbability;
        result.estimatedTimeToBreach = minTimeToBreach;
        result.confidence = calculateConfidence(metrics, maxProbability);
        result.criticalMetric = criticalMetric;

        Log.infof("Breach prediction for slice %s: probability=%.2f, timeToBreach=%d min, confidence=%.2f",
                slice.sliceId, result.breachProbability, result.estimatedTimeToBreach, result.confidence);

        return result;
    }

    /**
     * Get detailed predictions for all metric types
     */
    public MetricPredictions getDetailedPredictions(NetworkSlice slice) {
        List<SLAMetric> metrics = SLAMetric.list("slice = ?1 ORDER BY timestamp DESC", slice);

        MetricPredictions predictions = new MetricPredictions();
        predictions.sliceId = slice.sliceId;
        predictions.predictedAt = java.time.LocalDateTime.now();

        for (SLAMetric.MetricType metricType : SLAMetric.MetricType.values()) {
            List<SLAMetric> metricHistory = metrics.stream()
                    .filter(m -> m.metricType == metricType)
                    .limit(100)
                    .toList();

            if (metricHistory.isEmpty()) {
                continue;
            }

            TimeSeriesAnalyzer.TrendAnalysis analysis =
                timeSeriesAnalyzer.analyzeTrend(slice, metricType, metricHistory);

            PredictionResult metricPrediction = new PredictionResult();
            metricPrediction.sliceId = slice.sliceId;
            metricPrediction.predictedAt = java.time.LocalDateTime.now();
            metricPrediction.breachProbability = analysis.breachProbability;
            metricPrediction.estimatedTimeToBreach = analysis.estimatedTimeToBreach;
            metricPrediction.confidence = analysis.confidence;
            metricPrediction.criticalMetric = metricType.name();
            metricPrediction.trendDirection = analysis.trendDirection.name();
            metricPrediction.volatilityScore = analysis.volatility;

            predictions.predictions.put(metricType.name(), metricPrediction);
        }

        return predictions;
    }

    private double calculateConfidence(List<SLAMetric> metrics, double probability) {
        double dataQualityScore = Math.min(1.0, metrics.size() / 50.0);
        double probabilityScore = probability > 0.1 ? 1.0 : probability * 10;
        return Math.min(1.0, (dataQualityScore + probabilityScore) / 2.0);
    }
}
