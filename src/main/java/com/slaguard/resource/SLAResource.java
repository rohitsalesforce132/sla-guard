package com.slaguard.resource;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLA;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API for SLA management
 */
@Path("/api/v1/slices/{sliceId}/sla")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class SLAResource {

    @GET
    public Response get(@PathParam("sliceId") String sliceId) {
        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        return Response.ok(slice.sla).build();
    }

    @POST
    @Transactional
    public Response create(@PathParam("sliceId") String sliceId, @Valid SLACreateRequest request) {
        Log.infof("Creating SLA for slice %s: %s", sliceId, request.name);

        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        SLA sla = new SLA(
                request.slaId != null ? request.slaId : "SLA-" + System.currentTimeMillis(),
                request.name,
                request.latencyTargetMs,
                request.throughputTargetMbps,
                request.availabilityTargetPercent
        );

        sla.description = request.description;
        sla.jitterTargetMs = request.jitterTargetMs;
        sla.packetLossTargetPercent = request.packetLossTargetPercent;

        slice.sla = sla;
        slice.persist();

        return Response.status(Response.Status.CREATED).entity(sla).build();
    }

    @PUT
    @Path("/{slaId}")
    @Transactional
    public Response update(@PathParam("sliceId") String sliceId,
                          @PathParam("slaId") String slaId,
                          @Valid SLAUpdateRequest request) {
        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null || slice.sla == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice or SLA not found\"}")
                    .build();
        }

        SLA sla = slice.sla;

        if (request.name != null) sla.name = request.name;
        if (request.description != null) sla.description = request.description;
        if (request.latencyTargetMs != null) {
            sla.latencyTargetMs = request.latencyTargetMs;
            sla.latencyWarningMs = request.latencyTargetMs * 0.8;
            sla.latencyCriticalMs = request.latencyTargetMs * 0.95;
        }
        if (request.throughputTargetMbps != null) {
            sla.throughputTargetMbps = request.throughputTargetMbps;
            sla.throughputWarningMbps = request.throughputTargetMbps * 0.8;
            sla.throughputCriticalMbps = request.throughputTargetMbps * 0.95;
        }
        if (request.jitterTargetMs != null) {
            sla.jitterTargetMs = request.jitterTargetMs;
            sla.jitterWarningMs = request.jitterTargetMs * 0.8;
            sla.jitterCriticalMs = request.jitterTargetMs * 0.95;
        }
        if (request.packetLossTargetPercent != null) {
            sla.packetLossTargetPercent = request.packetLossTargetPercent;
            sla.packetLossWarningPercent = request.packetLossTargetPercent * 0.8;
            sla.packetLossCriticalPercent = request.packetLossTargetPercent * 0.95;
        }
        if (request.availabilityTargetPercent != null) {
            sla.availabilityTargetPercent = request.availabilityTargetPercent;
            sla.availabilityWarningPercent = request.availabilityTargetPercent * 0.99;
            sla.availabilityCriticalPercent = request.availabilityTargetPercent * 0.98;
        }
        if (request.isActive != null) sla.isActive = request.isActive;

        return Response.ok(sla).build();
    }

    @DELETE
    @Path("/{slaId}")
    @Transactional
    public Response delete(@PathParam("sliceId") String sliceId,
                          @PathParam("slaId") String slaId) {
        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null || slice.sla == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice or SLA not found\"}")
                    .build();
        }

        slice.sla = null;
        slice.persist();

        return Response.noContent().build();
    }

    // Request/Response DTOs

    public static class SLACreateRequest {
        public String slaId;
        public String name;
        public String description;
        public Double latencyTargetMs;
        public Double throughputTargetMbps;
        public Double jitterTargetMs;
        public Double packetLossTargetPercent;
        public Double availabilityTargetPercent;
    }

    public static class SLAUpdateRequest {
        public String name;
        public String description;
        public Double latencyTargetMs;
        public Double throughputTargetMbps;
        public Double jitterTargetMs;
        public Double packetLossTargetPercent;
        public Double availabilityTargetPercent;
        public Boolean isActive;
    }
}
