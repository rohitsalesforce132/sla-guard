package com.slaguard.camara;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.QoDSession;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class QoSProfileManager {

    /**
     * Get recommended QoS profile for a slice type
     */
    public QoDSession.QoSProfile getRecommendedProfile(NetworkSlice.SliceType sliceType) {
        switch (sliceType) {
            case EMERGENCY:
                return QoDSession.QoSProfile.QOS_E; // Emergency - highest priority
            case URLLC:
                return QoDSession.QoSProfile.QOS_S; // Signaling - very high priority
            case EMBB:
                return QoDSession.QoSProfile.QOS_M; // Medium - high priority
            case ENTERPRISE:
                return QoDSession.QoSProfile.QOS_M; // Medium - high priority for enterprise
            case IOT:
                return QoDSession.QoSProfile.QOS_L; // Low - medium priority
            case MMTC:
            default:
                return QoDSession.QoSProfile.QOS_B; // Background - best effort
        }
    }

    /**
     * Get QoS profile by name
     */
    public QoDSession.QoSProfile getProfileByName(String profileName) {
        try {
            return QoDSession.QoSProfile.valueOf(profileName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Log.warnf("Unknown QoS profile: %s, defaulting to QOS_B", profileName);
            return QoDSession.QoSProfile.QOS_B;
        }
    }

    /**
     * Get all available QoS profiles
     */
    public List<QoDSession.QoSProfile> getAllProfiles() {
        return Arrays.asList(QoDSession.QoSProfile.values());
    }

    /**
     * Get profile priority (higher = more important)
     */
    public int getProfilePriority(QoDSession.QoSProfile profile) {
        switch (profile) {
            case QOS_E: return 5; // Emergency - highest
            case QOS_S: return 4; // Signaling
            case QOS_M: return 3; // Medium
            case QOS_L: return 2; // Low
            case QOS_B: return 1; // Background - lowest
            default: return 0;
        }
    }

    /**
     * Get profile description
     */
    public String getProfileDescription(QoDSession.QoSProfile profile) {
        switch (profile) {
            case QOS_E:
                return "Emergency - Highest priority for emergency services";
            case QOS_S:
                return "Signaling - Very high priority for control plane traffic";
            case QOS_M:
                return "Medium - High priority for premium services";
            case QOS_L:
                return "Low - Medium priority for standard services";
            case QOS_B:
                return "Background - Best effort for non-critical traffic";
            default:
                return "Unknown profile";
        }
    }

    /**
     * Determine if a higher priority profile is needed based on SLA breach severity
     */
    public QoDSession.QoSProfile escalateProfile(QoDSession.QoSProfile currentProfile,
                                                 SLASeverity severity) {
        int currentPriority = getProfilePriority(currentProfile);
        int targetPriority = currentPriority;

        switch (severity) {
            case CRITICAL:
                targetPriority = 5; // QOS_E
                break;
            case MAJOR:
                targetPriority = Math.min(4, currentPriority + 1);
                break;
            case WARNING:
                targetPriority = Math.min(3, currentPriority + 1);
                break;
            case INFO:
            default:
                return currentProfile;
        }

        if (targetPriority > currentPriority) {
            return getProfileByPriority(targetPriority);
        }

        return currentProfile;
    }

    private QoDSession.QoSProfile getProfileByPriority(int priority) {
        switch (priority) {
            case 5: return QoDSession.QoSProfile.QOS_E;
            case 4: return QoDSession.QoSProfile.QOS_S;
            case 3: return QoDSession.QoSProfile.QOS_M;
            case 2: return QoDSession.QoSProfile.QOS_L;
            case 1: return QoDSession.QoSProfile.QOS_B;
            default: return QoDSession.QoSProfile.QOS_B;
        }
    }

    public enum SLASeverity {
        INFO,
        WARNING,
        MAJOR,
        CRITICAL
    }
}
