package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * QoS Status Changed Event from CAMARA QoD API.
 * Triggered when a session's QoS status transitions.
 */
public class QosStatusChangedEvent extends CamaraEvent {

    @JsonProperty("eventDetail")
    private QosStatusChangedDetail eventDetail;

    public QosStatusChangedEvent() {
        setEventType(EventType.QOS_STATUS_CHANGED);
    }

    public QosStatusChangedDetail getEventDetail() {
        return eventDetail;
    }

    public void setEventDetail(QosStatusChangedDetail eventDetail) {
        this.eventDetail = eventDetail;
    }
}
