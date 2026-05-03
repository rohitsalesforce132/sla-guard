package com.slaguard.camara.spec;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * CAMARA Quality on Demand API v0.9.0 REST Client.
 * Generated from the official CAMARA QoD OpenAPI specification.
 *
 * Endpoints:
 *   POST   /qod/v0/sessions          — Create QoS session
 *   GET    /qod/v0/sessions/{id}      — Get session info
 *   DELETE /qod/v0/sessions/{id}      — Delete session
 *   GET    /qod/v0/qos-profiles       — List QoS profiles
 *   GET    /qod/v0/qos-profiles/{name} — Get specific profile
 */
@Path("/qod/v0")
@RegisterRestClient(configKey = "camara-qod")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CamaraQoDClient {

    /**
     * Create a new QoS session to manage latency/throughput priorities.
     * Returns 201 with SessionInfo on success.
     *
     * @param request session creation parameters
     * @return created session details
     */
    @POST
    @Path("/sessions")
    CamaraSessionInfo createSession(CamaraCreateSession request);

    /**
     * Get QoS session information by session ID.
     * Returns 200 with SessionInfo.
     *
     * @param sessionId the session UUID
     * @return session details
     */
    @GET
    @Path("/sessions/{sessionId}")
    CamaraSessionInfo getSession(@PathParam("sessionId") String sessionId);

    /**
     * Delete a QoS session by session ID.
     * Returns 204 on success.
     *
     * @param sessionId the session UUID
     */
    @DELETE
    @Path("/sessions/{sessionId}")
    void deleteSession(@PathParam("sessionId") String sessionId);

    /**
     * List available QoS profiles.
     * Optional filters by name and status.
     *
     * @param name   optional profile name filter
     * @param status optional profile status filter
     * @return list of QoS profiles
     */
    @GET
    @Path("/qos-profiles")
    List<QosProfile> listQosProfiles(
            @QueryParam("name") String name,
            @QueryParam("status") String status);

    /**
     * Get a specific QoS profile by name.
     *
     * @param name the QoS profile name (e.g., QOS_E, QOS_S, QOS_M, QOS_L, QOS_B)
     * @return QoS profile details
     */
    @GET
    @Path("/qos-profiles/{name}")
    QosProfile getQosProfile(@PathParam("name") String name);
}
