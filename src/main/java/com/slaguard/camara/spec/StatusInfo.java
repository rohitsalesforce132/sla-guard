package com.slaguard.camara.spec;

/**
 * Reason for QoS status change.
 * Only applicable when qosStatus is UNAVAILABLE.
 */
public enum StatusInfo {
    DURATION_EXPIRED,   // Session terminated due to requested duration expired
    NETWORK_TERMINATED  // Network terminated the session before the requested duration expired
}
