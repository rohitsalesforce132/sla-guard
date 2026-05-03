package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event notification payload from CAMARA QoD API.
 */
public class CamaraEventNotification {

    @JsonProperty("event")
    private CamaraEvent event;

    @JsonProperty("eventSubscriptionId")
    private String eventSubscriptionId;

    public CamaraEventNotification() {
    }

    public CamaraEvent getEvent() {
        return event;
    }

    public void setEvent(CamaraEvent event) {
        this.event = event;
    }

    public String getEventSubscriptionId() {
        return eventSubscriptionId;
    }

    public void setEventSubscriptionId(String eventSubscriptionId) {
        this.eventSubscriptionId = eventSubscriptionId;
    }
}
