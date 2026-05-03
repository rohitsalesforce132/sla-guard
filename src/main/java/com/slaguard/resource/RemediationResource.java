package com.slaguard.resource;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.RemediationAction;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API for remediation actions
 */
@Path("/api/v1/remediations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class RemediationResource {

    @GET
    public List<RemediationAction> listAll(
            @QueryParam("sliceId") String sliceId,
            @QueryParam("status") String status,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        if (sliceId != null && status != null) {
            NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();
            if (slice == null) return List.of();

            return RemediationAction.list(
                    "slice = ?1 and status = ?2 ORDER BY initiatedAt DESC",
                    slice,
                    RemediationAction.ActionStatus.valueOf(status.toUpperCase())
            ).stream().limit(limit).toList();
        }

        if (sliceId != null) {
            NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();
            if (slice == null) return List.of();

            return RemediationAction.list(
                    "slice = ?1 ORDER BY initiatedAt DESC",
                    slice
            ).stream().limit(limit).toList();
        }

        if (status != null) {
            return RemediationAction.list(
                    "status = ?1 ORDER BY initiatedAt DESC",
                    RemediationAction.ActionStatus.valueOf(status.toUpperCase())
            ).stream().limit(limit).toList();
        }

        return RemediationAction.listAll().stream().limit(limit).toList();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) {
        RemediationAction action = RemediationAction.find("actionId", id).firstResult();

        if (action == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Remediation action not found\"}")
                    .build();
        }

        return Response.ok(action).build();
    }

    @POST
    @Transactional
    public Response create(@Valid RemediationCreateRequest request) {
        Log.infof("Creating remediation action for slice %s: %s", request.sliceId, request.type);

        NetworkSlice slice = NetworkSlice.find("sliceId", request.sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        RemediationAction action = new RemediationAction();
        action.actionId = request.actionId != null ? request.actionId : "REM-" + System.currentTimeMillis();
        action.slice = slice;
        action.type = RemediationAction.RemediationType.valueOf(request.type.toUpperCase());
        action.description = request.description;
        action.actionDetails = request.actionDetails;
        action.status = RemediationAction.ActionStatus.INITIATED;
        action.initiatedAt = LocalDateTime.now();

        action.persist();

        return Response.status(Response.Status.CREATED).entity(action).build();
    }

    @GET
    @Path("/slice/{sliceId}")
    public Response getBySlice(@PathParam("sliceId") String sliceId) {
        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        List<RemediationAction> actions = RemediationAction.list(
                "slice = ?1 ORDER BY initiatedAt DESC",
                slice
        );

        return Response.ok(actions).build();
    }

    @GET
    @Path("/active")
    public List<RemediationAction> getActive(
            @QueryParam("limit") @DefaultValue("100") int limit) {

        return RemediationAction.list(
                "status = ?1 ORDER BY initiatedAt DESC",
                RemediationAction.ActionStatus.IN_PROGRESS
        ).stream().limit(limit).toList();
    }

    // Request DTOs

    public static class RemediationCreateRequest {
        public String actionId;
        public String sliceId;
        public String type; // QOS_BOOST, SLICE_RESIZE, TRAFFIC_REROUTE, ESCALATION
        public String description;
        public String actionDetails;
    }
}
