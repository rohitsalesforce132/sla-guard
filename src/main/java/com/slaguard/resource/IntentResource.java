package com.slaguard.resource;

import com.slaguard.intent.IntentEnforcer;
import com.slaguard.intent.IntentExplainer;
import com.slaguard.intent.IntentParser;
import com.slaguard.model.Intent;
import com.slaguard.model.NetworkSlice;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * REST API for intent-based SLA definitions
 */
@Path("/api/v1/intents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class IntentResource {

    @Inject
    IntentParser intentParser;

    @Inject
    IntentEnforcer intentEnforcer;

    @Inject
    IntentExplainer intentExplainer;

    /**
     * Create an intent from natural language
     */
    @POST
    @Path("/parse")
    public Response parse(@Valid IntentParseRequest request) {
        Log.infof("Parsing intent: %s", request.intent);

        IntentParser.IntentParseResult result = intentParser.parse(request.intent);

        return Response.ok(result).build();
    }

    /**
     * Create and enforce an intent for a slice
     */
    @POST
    @Transactional
    public Response create(@Valid IntentCreateRequest request) {
        Log.infof("Creating intent for slice %s", request.sliceId);

        NetworkSlice slice = NetworkSlice.find("sliceId", request.sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        Intent intent = intentParser.createIntent(request.intent, slice);
        intent.persist();

        // Enforce the intent
        if (request.enforceImmediately) {
            intentEnforcer.enforceIntent(intent);
        }

        return Response.status(Response.Status.CREATED).entity(intent).build();
    }

    /**
     * Get an intent by ID
     */
    @GET
    @Path("/{intentId}")
    public Response get(@PathParam("intentId") String intentId) {
        Intent intent = Intent.find("intentId", intentId).firstResult();

        if (intent == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Intent not found\"}")
                    .build();
        }

        return Response.ok(intent).build();
    }

    /**
     * Get all intents
     */
    @GET
    public List<Intent> listAll(
            @QueryParam("sliceId") String sliceId,
            @QueryParam("status") String status) {

        if (sliceId != null && status != null) {
            NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();
            if (slice == null) return List.of();

            return Intent.list(
                    "slice = ?1 and status = ?2",
                    slice,
                    Intent.IntentStatus.valueOf(status.toUpperCase())
            );
        }

        if (sliceId != null) {
            NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();
            if (slice == null) return List.of();

            return Intent.list("slice", slice);
        }

        if (status != null) {
            return Intent.list("status", Intent.IntentStatus.valueOf(status.toUpperCase()));
        }

        return Intent.listAll();
    }

    /**
     * Get explanation of an intent in plain text
     */
    @GET
    @Path("/{intentId}/explain")
    @Produces(MediaType.TEXT_PLAIN)
    public Response explain(@PathParam("intentId") String intentId) {
        Intent intent = Intent.find("intentId", intentId).firstResult();

        if (intent == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Intent not found")
                    .type(MediaType.TEXT_PLAIN)
                    .build();
        }

        String explanation = intentExplainer.explainEnforcementStatus(intent);
        return Response.ok(explanation).type(MediaType.TEXT_PLAIN).build();
    }

    /**
     * Re-enforce an intent
     */
    @POST
    @Path("/{intentId}/enforce")
    @Transactional
    public Response enforce(@PathParam("intentId") String intentId) {
        Intent intent = Intent.find("intentId", intentId).firstResult();

        if (intent == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Intent not found\"}")
                    .build();
        }

        intentEnforcer.enforceIntent(intent);

        return Response.ok(intent).build();
    }

    /**
     * Delete an intent
     */
    @DELETE
    @Path("/{intentId}")
    @Transactional
    public Response delete(@PathParam("intentId") String intentId) {
        Intent intent = Intent.find("intentId", intentId).firstResult();

        if (intent == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Intent not found\"}")
                    .build();
        }

        intent.delete();
        return Response.noContent().build();
    }

    // Request DTOs

    public static class IntentParseRequest {
        public String intent;
    }

    public static class IntentCreateRequest {
        public String sliceId;
        public String intent;
        public boolean enforceImmediately = true;
    }
}
