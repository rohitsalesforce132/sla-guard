package com.slaguard.prediction;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class MetricPredictions {

    public String sliceId;
    public LocalDateTime predictedAt;
    public Map<String, PredictionResult> predictions = new HashMap<>();

    public PredictionResult getPredictionForMetric(String metricName) {
        return predictions.get(metricName);
    }

    public double getHighestBreachProbability() {
        return predictions.values().stream()
                .mapToDouble(p -> p.breachProbability)
                .max()
                .orElse(0.0);
    }

    public int getMinimumTimeToBreach() {
        return predictions.values().stream()
                .mapToInt(p -> p.estimatedTimeToBreach)
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    public String getCriticalMetric() {
        return predictions.entrySet().stream()
                .filter(e -> e.getValue().breachProbability > 0.5)
                .max((a, b) -> Double.compare(a.getValue().breachProbability, b.getValue().breachProbability))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
