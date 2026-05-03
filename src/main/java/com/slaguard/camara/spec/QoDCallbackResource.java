package com.slaguard.camara.spec;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Callback endpoint for CAMARA QoD session notifications.
 * The CAMARA QoD server calls this when session status changes.
 * Spec reference: callbacks section in POST /sessions.
 */
@Path("/api/v1/camara/qod/callback")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class QoDCallbackResource {

    @Inject
    SpecCompliantQoDManager sessionManager;

    /**
     * Handle QOS_STATUS_CHANGED notification from CAMARA QoD server.
     * Returns 204 on success (spec-compliant).
     */
    @POST
    public Response handleNotification(CamaraEventNotification notification) {
        try {
            Log.infof("Received CAMARA QoD notification: %s",
                    notification != null && notification.getEvent() != null
                            ? notification.getEvent().getEventType() : "unknown");
            sessionManager.handleNotification(notification);
            return Response.noContent().build(); // 204 — spec requires
        } catch (Exception e) {
            Log.errorf("Error processing CAMARA notification: %s", e.getMessage(), e);
            return Response.serverError().build(); // 500 — spec allows
        }
    }
}
