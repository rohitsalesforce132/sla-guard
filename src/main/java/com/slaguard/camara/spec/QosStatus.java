package com.slaguard.camara.spec;

/**
 * QoS status for a session.
 */
public enum QosStatus {
    REQUESTED,  // QoS has been requested by creating a session
    AVAILABLE,  // The requested QoS has been provided by the network
    UNAVAILABLE // The requested QoS cannot be provided by the network
}
