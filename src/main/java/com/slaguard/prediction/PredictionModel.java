package com.slaguard.prediction;

import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@IfBuildProperty(name = "sla-guard.prediction.enabled", stringValue = "true", enableIfMissing = true)
public class PredictionModel {

    @ConfigProperty(name = "sla-guard.prediction.ema-alpha", defaultValue = "0.3")
    double emaAlpha;

    @ConfigProperty(name = "sla-guard.prediction.warning-threshold", defaultValue = "0.8")
    double warningThreshold;

    @ConfigProperty(name = "sla-guard.prediction.critical-threshold", defaultValue = "0.95")
    double criticalThreshold;

    @ConfigProperty(name = "sla-guard.prediction.prediction-window-minutes", defaultValue = "15")
    int predictionWindowMinutes;

    @ConfigProperty(name = "sla-guard.prediction.history-size", defaultValue = "100")
    int historySize;

    public double getEmaAlpha() {
        return emaAlpha;
    }

    public void setEmaAlpha(double emaAlpha) {
        this.emaAlpha = emaAlpha;
    }

    public double getWarningThreshold() {
        return warningThreshold;
    }

    public void setWarningThreshold(double warningThreshold) {
        this.warningThreshold = warningThreshold;
    }

    public double getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(double criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public int getPredictionWindowMinutes() {
        return predictionWindowMinutes;
    }

    public void setPredictionWindowMinutes(int predictionWindowMinutes) {
        this.predictionWindowMinutes = predictionWindowMinutes;
    }

    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }
}
