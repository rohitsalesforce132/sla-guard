package com.slaguard.prediction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PredictionResult {

    public String sliceId;
    public LocalDateTime predictedAt;
    public double breachProbability;        // 0.0 to 1.0
    public int estimatedTimeToBreach;       // minutes, Integer.MAX_VALUE if not expected
    public double confidence;               // 0.0 to 1.0
    public String criticalMetric;           // Metric most likely to breach
    public String trendDirection;           // "INCREASING", "DECREASING", "STABLE"
    public double volatilityScore;          // Volatility metric
    public List<String> affectedMetrics = new ArrayList<>();
    public Map<String, Object> details = new HashMap<>();

    public boolean isBreachLikely() {
        return breachProbability > 0.5;
    }

    public boolean isBreachImminent() {
        return breachProbability > 0.8 && estimatedTimeToBreach < 10;
    }

    public String getSummary() {
        if (breachProbability < 0.3) {
            return String.format("Low breach risk (%.1f%%)", breachProbability * 100);
        } else if (breachProbability < 0.7) {
            return String.format("Moderate breach risk (%.1f%%) in ~%d min",
                    breachProbability * 100, estimatedTimeToBreach);
        } else {
            return String.format("HIGH breach risk (%.1f%%) in ~%d min",
                    breachProbability * 100, estimatedTimeToBreach);
        }
    }
}
