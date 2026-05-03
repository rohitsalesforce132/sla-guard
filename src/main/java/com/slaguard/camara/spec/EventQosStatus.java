package com.slaguard.camara.spec;

/**
 * QoS status for events.
 * Only AVAILABLE and UNAVAILABLE are used in event notifications.
 */
public enum EventQosStatus {
    AVAILABLE,  // The requested QoS has been provided by the network
    UNAVAILABLE // A requested or previously available QoS session is currently unavailable
}
