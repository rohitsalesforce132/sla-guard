package com.slaguard.resource;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLABreach;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST API for SLA breach history
 */
@Path("/api/v1/breaches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class BreachResource {

    @GET
    public List<SLABreach> listAll(
            @QueryParam("sliceId") String sliceId,
            @QueryParam("severity") String severity,
            @QueryParam("resolved") Boolean resolved,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        if (sliceId != null && severity != null) {
            NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();
            if (slice == null) return List.of();

            return SLABreach.list(
                    "slice = ?1 and severity = ?2 ORDER BY detectedAt DESC",
                    slice,
                    SLABreach.Severity.valueOf(severity.toUpperCase())
            ).stream().limit(limit).toList();
        }

        if (sliceId != null) {
            NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();
            if (slice == null) return List.of();

            return SLABreach.list(
                    "slice = ?1 ORDER BY detectedAt DESC",
                    slice
            ).stream().limit(limit).toList();
        }

        if (severity != null) {
            return SLABreach.list(
                    "severity = ?1 ORDER BY detectedAt DESC",
                    SLABreach.Severity.valueOf(severity.toUpperCase())
            ).stream().limit(limit).toList();
        }

        if (resolved != null) {
            return SLABreach.list(
                    "resolved = ?1 ORDER BY detectedAt DESC",
                    resolved
            ).stream().limit(limit).toList();
        }

        return SLABreach.listAll().stream().limit(limit).toList();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) {
        SLABreach breach = SLABreach.find("breachId", id).firstResult();

        if (breach == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Breach not found\"}")
                    .build();
        }

        return Response.ok(breach).build();
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

        List<SLABreach> breaches = SLABreach.list(
                "slice = ?1 ORDER BY detectedAt DESC",
                slice
        );

        return Response.ok(breaches).build();
    }

    @GET
    @Path("/unresolved")
    public List<SLABreach> getUnresolved(
            @QueryParam("limit") @DefaultValue("100") int limit) {

        return SLABreach.list(
                "resolved = ?1 ORDER BY detectedAt DESC",
                false
        ).stream().limit(limit).toList();
    }
}
