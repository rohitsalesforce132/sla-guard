package com.slaguard.engine;

import com.slaguard.camara.QoDService;
import com.slaguard.camara.QoDSessionManager;
import com.slaguard.model.*;
import com.slaguard.prediction.PredictionResult;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class RemediationEngine {

    @Inject
    QoDService qodService;

    @Inject
    QoDSessionManager qodSessionManager;

    /**
     * Handle an SLA breach - decide and execute remediation
     */
    @Transactional
    public void handleSLABreach(NetworkSlice slice, SLAEvaluator.EvaluationResult evaluation,
                                PredictionResult prediction) {
        Log.infof("Handling SLA breach for slice %s (status: %s)", slice.sliceId, evaluation.overallStatus);

        // Determine remediation strategy
        RemediationStrategy strategy = determineRemediationStrategy(slice, evaluation, prediction);

        Log.infof("Remediation strategy for slice %s: %s", slice.sliceId, strategy.type);

        // Execute remediation
        RemediationAction action = executeRemediation(slice, strategy, evaluation, prediction);

        // Record breach if not already recorded
        if (evaluation.overallStatus == SLAMetric.SLAStatus.BREACHED) {
            recordBreach(slice, evaluation, prediction, action);
        }
    }

    /**
     * Determine the best remediation strategy based on current situation
     */
    private RemediationStrategy determineRemediationStrategy(NetworkSlice slice,
                                                             SLAEvaluator.EvaluationResult evaluation,
                                                             PredictionResult prediction) {
        RemediationStrategy strategy = new RemediationStrategy();

        // If breach is imminent and severe, use QoS boost
        if (prediction.breachProbability > 0.8 && prediction.estimatedTimeToBreach < 10) {
            strategy.type = RemediationAction.RemediationType.QOS_BOOST;
            strategy.priority = 1;
            strategy.reason = "High probability of imminent breach";
            strategy.qosProfile = determineQoSProfile(slice);
            return strategy;
        }

        // If already breached, use QoS boost
        if (evaluation.overallStatus == SLAMetric.SLAStatus.BREACHED) {
            strategy.type = RemediationAction.RemediationType.QOS_BOOST;
            strategy.priority = 1;
            strategy.reason = "SLA already breached";
            strategy.qosProfile = determineQoSProfile(slice);
            return strategy;
        }

        // If critical status, use QoS boost
        if (evaluation.overallStatus == SLAMetric.SLAStatus.CRITICAL) {
            strategy.type = RemediationAction.RemediationType.QOS_BOOST;
            strategy.priority = 1;
            strategy.reason = "Critical SLA status";
            strategy.qosProfile = determineQoSProfile(slice);
            return strategy;
        }

        // For warnings with medium probability, use lighter QoS boost
        if (evaluation.overallStatus == SLAMetric.SLAStatus.WARNING &&
            prediction.breachProbability > 0.5) {
            strategy.type = RemediationAction.RemediationType.QOS_BOOST;
            strategy.priority = 2;
            strategy.reason = "Warning with breach risk";
            strategy.qosProfile = QoDSession.QoSProfile.QOS_M; // Medium priority
            return strategy;
        }

        // If prediction shows long-term trend issues, escalate
        if (prediction.breachProbability > 0.7 && prediction.estimatedTimeToBreach > 30) {
            strategy.type = RemediationAction.RemediationType.ESCALATION;
            strategy.priority = 3;
            strategy.reason = "Long-term trend issues requiring manual intervention";
            return strategy;
        }

        // Default to escalation
        strategy.type = RemediationAction.RemediationType.ESCALATION;
        strategy.priority = 4;
        strategy.reason = "No clear automated remediation path";
        return strategy;
    }

    /**
     * Determine appropriate QoS profile based on slice type
     */
    private QoDSession.QoSProfile determineQoSProfile(NetworkSlice slice) {
        switch (slice.sliceType) {
            case EMERGENCY:
                return QoDSession.QoSProfile.QOS_E;
            case URLLC:
                return QoDSession.QoSProfile.QOS_S;
            case EMBB:
                return QoDSession.QoSProfile.QOS_M;
            case ENTERPRISE:
                return QoDSession.QoSProfile.QOS_M;
            case IOT:
                return QoDSession.QoSProfile.QOS_L;
            case MMTC:
            default:
                return QoDSession.QoSProfile.QOS_B;
        }
    }

    /**
     * Execute the remediation action
     */
    @Transactional
    private RemediationAction executeRemediation(NetworkSlice slice, RemediationStrategy strategy,
                                                 SLAEvaluator.EvaluationResult evaluation,
                                                 PredictionResult prediction) {
        RemediationAction action = new RemediationAction();
        action.slice = slice;
        action.type = strategy.type;
        action.description = strategy.reason;
        action.status = RemediationAction.ActionStatus.INITIATED;
        action.initiatedAt = LocalDateTime.now();

        try {
            switch (strategy.type) {
                case QOS_BOOST:
                    executeQoSBoost(slice, action, strategy.qosProfile);
                    break;

                case SLICE_RESIZE:
                    // Would call TMF API to resize slice
                    action.status = RemediationAction.ActionStatus.IN_PROGRESS;
                    action.resultMessage = "Slice resize initiated via TMF API";
                    break;

                case TRAFFIC_REROUTE:
                    // Would call TMF API to reroute traffic
                    action.status = RemediationAction.ActionStatus.IN_PROGRESS;
                    action.resultMessage = "Traffic reroute initiated via TMF API";
                    break;

                case ESCALATION:
                    action.status = RemediationAction.ActionStatus.IN_PROGRESS;
                    action.resultMessage = "Escalated to operator for manual intervention";
                    break;

                default:
                    action.status = RemediationAction.ActionStatus.FAILED;
                    action.errorMessage = "Unknown remediation type";
            }

            action.persist();

        } catch (Exception e) {
            Log.errorf("Error executing remediation for slice %s: %s", slice.sliceId, e.getMessage(), e);
            action.status = RemediationAction.ActionStatus.FAILED;
            action.errorMessage = e.getMessage();
            action.persist();
        }

        return action;
    }

    /**
     * Execute QoS boost via CAMARA QoD API
     */
    private void executeQoSBoost(NetworkSlice slice, RemediationAction action, QoDSession.QoSProfile profile) {
        Log.infof("Initiating QoS boost for slice %s with profile %s", slice.sliceId, profile);

        try {
            // Create CAMARA QoD session
            QoDSession session = qodService.createSession(
                slice.deviceId,
                slice.deviceIpAddress,
                slice.applicationServerIp,
                profile.name(),
                15 // 15 minutes default
            );

            if (session != null && session.status == QoDSession.SessionStatus.ACTIVE) {
                action.status = RemediationAction.ActionStatus.COMPLETED;
                action.successful = true;
                action.externalReference = session.sessionId;
                action.resultMessage = String.format("QoS boost applied via CAMARA session %s", session.sessionId);
                action.expiresAt = session.expiresAt;
                action.rollbackAction = "Allow CAMARA session to expire or explicitly revoke";
                action.completedAt = LocalDateTime.now();

                Log.infof("QoS boost successful for slice %s: session %s", slice.sliceId, session.sessionId);
            } else {
                action.status = RemediationAction.ActionStatus.FAILED;
                action.successful = false;
                action.errorMessage = "CAMARA QoD session creation failed";
            }

        } catch (Exception e) {
            Log.errorf("QoS boost failed for slice %s: %s", slice.sliceId, e.getMessage(), e);
            action.status = RemediationAction.ActionStatus.FAILED;
            action.successful = false;
            action.errorMessage = e.getMessage();
        }
    }

    /**
     * Record an SLA breach event
     */
    @Transactional
    private void recordBreach(NetworkSlice slice, SLAEvaluator.EvaluationResult evaluation,
                             PredictionResult prediction, RemediationAction action) {
        // Find the breached metric
        SLAMetric.MetricType breachedMetric = findBreachedMetric(evaluation);
        if (breachedMetric == null) {
            return;
        }

        SLABreach breach = new SLABreach();
        breach.slice = slice;
        breach.metricType = breachedMetric;
        breach.actualValue = getMetricValue(evaluation, breachedMetric);
        breach.thresholdValue = getMetricThreshold(evaluation, breachedMetric);
        breach.severity = determineSeverity(evaluation);
        breach.description = String.format("%s %s for slice %s",
                breachedMetric, evaluation.overallStatus, slice.sliceId);
        breach.detectedAt = LocalDateTime.now();
        breach.statusBeforeBreach = SLAStatus.COMPLIANT; // Would be tracked in real system
        breach.predictedMinutesBeforeBreach = prediction.estimatedTimeToBreach;
        breach.predictionConfidence = prediction.confidence;
        breach.remediationAction = action;
        breach.resolved = false;

        breach.persist();

        Log.warnf("Recorded breach %s for slice %s", breach.breachId, slice.sliceId);
    }

    private SLAMetric.MetricType findBreachedMetric(SLAEvaluator.EvaluationResult evaluation) {
        if (evaluation.latencyStatus == SLAMetric.SLAStatus.BREACHED) return SLAMetric.MetricType.LATENCY;
        if (evaluation.throughputStatus == SLAMetric.SLAStatus.BREACHED) return SLAMetric.MetricType.THROUGHPUT;
        if (evaluation.jitterStatus == SLAMetric.SLAStatus.BREACHED) return SLAMetric.MetricType.JITTER;
        if (evaluation.packetLossStatus == SLAMetric.SLAStatus.BREACHED) return SLAMetric.MetricType.PACKET_LOSS;
        if (evaluation.availabilityStatus == SLAMetric.SLAStatus.BREACHED) return SLAMetric.MetricType.AVAILABILITY;
        return null;
    }

    private Double getMetricValue(SLAEvaluator.EvaluationResult evaluation, SLAMetric.MetricType type) {
        switch (type) {
            case LATENCY: return evaluation.latencyValue;
            case THROUGHPUT: return evaluation.throughputValue;
            case JITTER: return evaluation.jitterValue;
            case PACKET_LOSS: return evaluation.packetLossValue;
            case AVAILABILITY: return evaluation.availabilityValue;
            default: return null;
        }
    }

    private Double getMetricThreshold(SLAEvaluator.EvaluationResult evaluation, SLAMetric.MetricType type) {
        // In real implementation, would get from SLA
        return null;
    }

    private SLABreach.Severity determineSeverity(SLAEvaluator.EvaluationResult evaluation) {
        if (evaluation.overallStatus == SLAMetric.SLAStatus.BREACHED) {
            return SLABreach.Severity.CRITICAL;
        } else if (evaluation.overallStatus == SLAMetric.SLAStatus.CRITICAL) {
            return SLABreach.Severity.MAJOR;
        } else {
            return SLABreach.Severity.WARNING;
        }
    }

    private static class RemediationStrategy {
        RemediationAction.RemediationType type;
        int priority;
        String reason;
        QoDSession.QoSProfile qosProfile;
    }
}
