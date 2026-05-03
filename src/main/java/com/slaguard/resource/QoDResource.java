package com.slaguard.resource;

import com.slaguard.camara.QoDService;
import com.slaguard.camara.QoDSessionManager;
import com.slaguard.model.NetworkSlice;
import com.slaguard.model.QoDSession;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST API for CAMARA Quality on Demand session management
 */
@Path("/api/v1/qod/sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class QoDResource {

    @Inject
    QoDService qodService;

    @Inject
    QoDSessionManager sessionManager;

    /**
     * Create a new QoD session
     */
    @POST
    @Transactional
    public Response create(@Valid QoDSessionCreateRequest request) {
        Log.infof("Creating QoD session for slice %s with profile %s",
                request.sliceId, request.qosProfile);

        NetworkSlice slice = NetworkSlice.find("sliceId", request.sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        QoDSession session = qodService.createSessionForSlice(
                slice,
                QoDSession.QoSProfile.valueOf(request.qosProfile.toUpperCase()),
                request.durationMinutes
        );

        if (session != null) {
            session.slice = slice;
            session.persist();
            return Response.status(Response.Status.CREATED).entity(session).build();
        } else {
            return Response.serverError()
                    .entity("{\"error\":\"Failed to create QoD session\"}")
                    .build();
        }
    }

    /**
     * Get all QoD sessions
     */
    @GET
    public List<QoDSession> listAll(
            @QueryParam("sliceId") String sliceId,
            @QueryParam("status") String status) {

        if (sliceId != null && status != null) {
            NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();
            if (slice == null) return List.of();

            return QoDSession.list(
                    "slice = ?1 and status = ?2 ORDER BY startedAt DESC",
                    slice,
                    QoDSession.SessionStatus.valueOf(status.toUpperCase())
            );
        }

        if (sliceId != null) {
            NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();
            if (slice == null) return List.of();

            return QoDSession.list("slice = ?1 ORDER BY startedAt DESC", slice);
        }

        if (status != null) {
            return QoDSession.list(
                    "status = ?1 ORDER BY startedAt DESC",
                    QoDSession.SessionStatus.valueOf(status.toUpperCase())
            );
        }

        return QoDSession.listAll();
    }

    /**
     * Get a specific QoD session
     */
    @GET
    @Path("/{sessionId}")
    public Response get(@PathParam("sessionId") String sessionId) {
        QoDSession session = QoDSession.find("sessionId", sessionId).firstResult();

        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Session not found\"}")
                    .build();
        }

        return Response.ok(session).build();
    }

    /**
     * Revoke a QoD session
     */
    @DELETE
    @Path("/{sessionId}")
    @Transactional
    public Response revoke(@PathParam("sessionId") String sessionId) {
        QoDSession session = QoDSession.find("sessionId", sessionId).firstResult();

        if (session == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Session not found\"}")
                    .build();
        }

        boolean success = sessionManager.revokeSession(session);

        if (success) {
            return Response.ok(session).build();
        } else {
            return Response.serverError()
                    .entity("{\"error\":\"Failed to revoke session\"}")
                    .build();
        }
    }

    /**
     * Get active sessions
     */
    @GET
    @Path("/active")
    public List<QoDSession> getActive() {
        return sessionManager.getAllActiveSessions();
    }

    /**
     * Get sessions for a slice
     */
    @GET
    @Path("/slice/{sliceId}")
    public Response getBySlice(@PathParam("sliceId") String sliceId) {
        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        List<QoDSession> sessions = QoDSession.list(
                "slice = ?1 ORDER BY startedAt DESC",
                slice
        );

        return Response.ok(sessions).build();
    }

    // Request DTOs

    public static class QoDSessionCreateRequest {
        public String sliceId;
        public String qosProfile; // QOS_E, QOS_S, QOS_M, QOS_L, QOS_B
        public int durationMinutes = 15;
    }
}
