package com.slaguard.tmf;

import com.slaguard.model.SLABreach;
import com.slaguard.model.RemediationAction;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TMF688: Event Management API
 * Publishes and queries SLA breach and remediation events
 */
@Path("/tmf-api/eventManagement/v4")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TMF688Resource {

    @ConfigProperty(name = "sla-guard.tmf.base-url")
    String tmfBaseUrl;

    /**
     * Publish an event (SLA breach, remediation, etc.)
     */
    @POST
    @Path("/event")
    public Response publishEvent(EventRequest request) {
        Log.infof("TMF688: Publishing event - type=%s, source=%s", request.eventType, request.source);

        try {
            EventResponse response = new EventResponse();
            response.id = "EVT-" + UUID.randomUUID().toString();
            response.eventTime = LocalDateTime.now();
            response.eventType = request.eventType;
            response.source = request.source;
            response.href = tmfBaseUrl + "/eventManagement/v4/event/" + response.id;

            Log.infof("Event published: %s", response.id);
            return Response.status(201).entity(response).build();

        } catch (Exception e) {
            Log.errorf("Error publishing event: %s", e.getMessage(), e);
            return Response.serverError()
                    .entity(createErrorResponse("EVENT_PUBLISH_ERROR", e.getMessage()))
                    .build();
        }
    }

    /**
     * Query events
     */
    @GET
    @Path("/event")
    public Response queryEvents(
            @QueryParam("eventType") String eventType,
            @QueryParam("source") String source,
            @QueryParam("startDateTime") String startDateTime,
            @QueryParam("endDateTime") String endDateTime,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        Log.debugf("TMF688: Querying events - type=%s, source=%s", eventType, source);

        EventListResponse response = new EventListResponse();

        // Return mock events for demo
        if (eventType == null || eventType.equals("SLABreach")) {
            response.add(createSLABreachEvent());
        }

        if (eventType == null || eventType.equals("Remediation")) {
            response.add(createRemediationEvent());
        }

        return Response.ok(response).build();
    }

    /**
     * Get a specific event
     */
    @GET
    @Path("/event/{id}")
    public Response getEvent(@PathParam("id") String id) {
        Log.debugf("TMF688: Getting event %s", id);

        EventResponse event = new EventResponse();
        event.id = id;
        event.eventTime = LocalDateTime.now();
        event.eventType = "SLABreach";
        event.source = "SLA-Guard";
        event.href = tmfBaseUrl + "/eventManagement/v4/event/" + id;

        return Response.ok(event).build();
    }

    /**
     * Create an SLA breach event
     */
    public EventResponse createSLABreachEvent() {
        EventResponse event = new EventResponse();
        event.id = "EVT-BREACH-" + UUID.randomUUID().toString();
        event.eventTime = LocalDateTime.now().minusMinutes(5);
        event.eventType = "SLABreach";
        event.source = "SLA-Guard";
        event.href = tmfBaseUrl + "/eventManagement/v4/event/" + event.id;

        EventAttribute attr = new EventAttribute();
        attr.name = "sliceId";
        attr.value = "SLICE-001";
        event.attribute.add(attr);

        attr = new EventAttribute();
        attr.name = "metricType";
        attr.value = "LATENCY";
        event.attribute.add(attr);

        attr = new EventAttribute();
        attr.name = "severity";
        attr.value = "CRITICAL";
        event.attribute.add(attr);

        attr = new EventAttribute();
        attr.name = "actualValue";
        attr.value = "15.5";
        event.attribute.add(attr);

        attr = new EventAttribute();
        attr.name = "thresholdValue";
        attr.value = "10.0";
        event.attribute.add(attr);

        return event;
    }

    /**
     * Create a remediation event
     */
    public EventResponse createRemediationEvent() {
        EventResponse event = new EventResponse();
        event.id = "EVT-REM-" + UUID.randomUUID().toString();
        event.eventTime = LocalDateTime.now().minusMinutes(2);
        event.eventType = "Remediation";
        event.source = "SLA-Guard";
        event.href = tmfBaseUrl + "/eventManagement/v4/event/" + event.id;

        EventAttribute attr = new EventAttribute();
        attr.name = "sliceId";
        attr.value = "SLICE-001";
        event.attribute.add(attr);

        attr = new EventAttribute();
        attr.name = "remediationType";
        attr.value = "QOS_BOOST";
        event.attribute.add(attr);

        attr = new EventAttribute();
        attr.name = "actionId";
        attr.value = "REM-" + UUID.randomUUID().toString();
        event.attribute.add(attr);

        attr = new EventAttribute();
        attr.name = "status";
        attr.value = "COMPLETED";
        event.attribute.add(attr);

        attr = new EventAttribute();
        attr.name = "camaraSessionId";
        attr.value = "QOD-" + UUID.randomUUID().toString();
        event.attribute.add(attr);

        return event;
    }

    private TMFErrorResponse createErrorResponse(String code, String message) {
        TMFErrorResponse error = new TMFErrorResponse();
        error.code = code;
        error.message = message;
        error.reason = message;
        error.status = 500;
        error.timestamp = LocalDateTime.now();
        return error;
    }

    // TMF688 Data Models

    public static class EventRequest {
        public String eventType;
        public String source;
        public List<EventAttribute> attribute = new ArrayList<>();
    }

    public static class EventResponse {
        public String id;
        @com.fasterxml.jackson.annotation.JsonProperty("@type")
        public String type = "Event";
        public LocalDateTime eventTime;
        public String eventType;
        public String source;
        public String href;
        public List<EventAttribute> attribute = new ArrayList<>();
    }

    public static class EventAttribute {
        public String name;
        public String value;
    }

    public static class EventListResponse extends ArrayList<EventResponse> {
    }
}
