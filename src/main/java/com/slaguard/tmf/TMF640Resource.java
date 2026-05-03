package com.slaguard.tmf;

import com.slaguard.model.NetworkSlice;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TMF640: Service Activation API
 * Handles activation and deactivation of network slice services
 */
@Path("/tmf-api/serviceActivation/v4")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TMF640Resource {

    @ConfigProperty(name = "sla-guard.tmf.base-url")
    String tmfBaseUrl;

    /**
     * Activate a network slice service
     */
    @POST
    @Path("/serviceActivation")
    public Response activateService(ServiceActivationRequest request) {
        Log.infof("TMF640: Activating service for slice %s", request.serviceId);

        try {
            // In real implementation, would call external TMF API
            ServiceActivationResponse response = new ServiceActivationResponse();
            response.id = "ACT-" + UUID.randomUUID().toString();
            response.serviceId = request.serviceId;
            response.state = "IN_PROGRESS";
            response.startDate = LocalDateTime.now();
            response.href = tmfBaseUrl + "/serviceActivation/v4/serviceActivation/" + response.id;

            Log.infof("Service activation initiated: %s", response.id);
            return Response.status(202).entity(response).build();

        } catch (Exception e) {
            Log.errorf("Error activating service: %s", e.getMessage(), e);
            return Response.serverError()
                    .entity(createErrorResponse("ACTIVATION_ERROR", e.getMessage()))
                    .build();
        }
    }

    /**
     * Deactivate a network slice service
     */
    @POST
    @Path("/serviceDeactivation")
    public Response deactivateService(ServiceDeactivationRequest request) {
        Log.infof("TMF640: Deactivating service for slice %s", request.serviceId);

        try {
            ServiceActivationResponse response = new ServiceActivationResponse();
            response.id = "DEACT-" + UUID.randomUUID().toString();
            response.serviceId = request.serviceId;
            response.state = "IN_PROGRESS";
            response.startDate = LocalDateTime.now();
            response.href = tmfBaseUrl + "/serviceActivation/v4/serviceDeactivation/" + response.id;

            Log.infof("Service deactivation initiated: %s", response.id);
            return Response.status(202).entity(response).build();

        } catch (Exception e) {
            Log.errorf("Error deactivating service: %s", e.getMessage(), e);
            return Response.serverError()
                    .entity(createErrorResponse("DEACTIVATION_ERROR", e.getMessage()))
                    .build();
        }
    }

    /**
     * Get activation status
     */
    @GET
    @Path("/serviceActivation/{id}")
    public Response getActivationStatus(@PathParam("id") String id) {
        Log.debugf("TMF640: Getting activation status for %s", id);

        ServiceActivationResponse response = new ServiceActivationResponse();
        response.id = id;
        response.serviceId = "SLICE-" + id.substring(4);
        response.state = "COMPLETED";
        response.startDate = LocalDateTime.now().minusMinutes(5);
        response.endDate = LocalDateTime.now();

        return Response.ok(response).build();
    }

    /**
     * Remediate a service (triggered by SLA breach)
     */
    @POST
    @Path("/serviceRemediation")
    public Response remediateService(ServiceRemediationRequest request) {
        Log.infof("TMF640: Remediation for slice %s, type=%s",
                request.serviceId, request.remediationType);

        try {
            ServiceRemediationResponse response = new ServiceRemediationResponse();
            response.id = "REM-" + UUID.randomUUID().toString();
            response.serviceId = request.serviceId;
            response.remediationType = request.remediationType;
            response.state = "IN_PROGRESS";
            response.initiatedAt = LocalDateTime.now();
            response.href = tmfBaseUrl + "/serviceActivation/v4/serviceRemediation/" + response.id;

            Log.infof("Service remediation initiated: %s", response.id);
            return Response.status(202).entity(response).build();

        } catch (Exception e) {
            Log.errorf("Error during service remediation: %s", e.getMessage(), e);
            return Response.serverError()
                    .entity(createErrorResponse("REMEDIATION_ERROR", e.getMessage()))
                    .build();
        }
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

    // TMF640 Data Models

    public static class ServiceActivationRequest {
        public String serviceId;
        public String serviceType;
        public Map<String, Object> parameters = new HashMap<>();
    }

    public static class ServiceDeactivationRequest {
        public String serviceId;
        public String reason;
    }

    public static class ServiceRemediationRequest {
        public String serviceId;
        public String remediationType; // QOS_BOOST, TRAFFIC_REROUTE, SCALE_UP
        public String breachId;
        public Map<String, Object> parameters = new HashMap<>();
    }

    public static class ServiceActivationResponse {
        public String id;
        @com.fasterxml.jackson.annotation.JsonProperty("@type")
        public String type = "ServiceActivation";
        public String serviceId;
        public String state; // IN_PROGRESS, COMPLETED, FAILED
        public LocalDateTime startDate;
        public LocalDateTime endDate;
        public String href;
        public List<TMFCharacteristic> characteristics = new ArrayList<>();
    }

    public static class ServiceRemediationResponse {
        public String id;
        @com.fasterxml.jackson.annotation.JsonProperty("@type")
        public String type = "ServiceRemediation";
        public String serviceId;
        public String remediationType;
        public String state;
        public LocalDateTime initiatedAt;
        public LocalDateTime completedAt;
        public String href;
        public String resultMessage;
    }

    public static class TMFCharacteristic {
        public String name;
        public String value;
    }
}
