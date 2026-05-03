package com.slaguard.tmf;

import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLA;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TMF641: Service Inventory API
 * Manages inventory of network slices and their SLAs
 */
@Path("/tmf-api/serviceInventory/v4")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class TMF641Resource {

    @ConfigProperty(name = "sla-guard.tmf.base-url")
    String tmfBaseUrl;

    @Inject
    EntityManager em;

    /**
     * List all network slices (services)
     */
    @GET
    @Path("/service")
    public Response listServices(
            @QueryParam("serviceType") String serviceType,
            @QueryParam("status") String status,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("20") int limit) {

        Log.debugf("TMF641: Listing services - type=%s, status=%s", serviceType, status);

        StringBuilder jpql = new StringBuilder("SELECT s FROM NetworkSlice s WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (serviceType != null) {
            jpql.append(" AND s.sliceType = :sliceType");
            params.put("sliceType", NetworkSlice.SliceType.valueOf(serviceType.toUpperCase()));
        }

        if (status != null) {
            jpql.append(" AND s.status = :status");
            params.put("status", NetworkSlice.SliceStatus.valueOf(status.toUpperCase()));
        }

        TypedQuery<NetworkSlice> query = em.createQuery(jpql.toString(), NetworkSlice.class);
        params.forEach(query::setParameter);

        query.setFirstResult(offset);
        query.setMaxResults(limit);

        List<NetworkSlice> slices = query.getResultList();
        ServiceListResponse response = convertToServiceList(slices);

        return Response.ok(response).build();
    }

    /**
     * Get a specific service (network slice)
     */
    @GET
    @Path("/service/{id}")
    public Response getService(@PathParam("id") String id) {
        Log.debugf("TMF641: Getting service %s", id);

        NetworkSlice slice = NetworkSlice.find("sliceId", id).firstResult();

        if (slice == null) {
            return Response.status(404)
                    .entity(createErrorResponse("NOT_FOUND", "Service not found: " + id))
                    .build();
        }

        ServiceResponse response = convertToService(slice);
        return Response.ok(response).build();
    }

    /**
     * Create a new service (network slice)
     */
    @POST
    @Path("/service")
    @Transactional
    public Response createService(ServiceRequest request, @jakarta.ws.rs.core.Context UriInfo uriInfo) {
        Log.infof("TMF641: Creating service - name=%s, type=%s", request.name, request.serviceType);

        try {
            NetworkSlice slice = new NetworkSlice();
            slice.sliceId = request.id != null ? request.id : "SLICE-" + System.currentTimeMillis();
            slice.name = request.name;
            slice.description = request.description;
            slice.sliceType = NetworkSlice.SliceType.valueOf(request.serviceType.toUpperCase());
            slice.status = NetworkSlice.SliceStatus.valueOf(request.status.toUpperCase());
            slice.deviceId = request.deviceId;
            slice.deviceIpAddress = request.deviceIpAddress;
            slice.applicationServerIp = request.applicationServerIp;
            slice.createdAt = LocalDateTime.now();
            slice.updatedAt = LocalDateTime.now();

            // Create SLA if provided
            if (request.sla != null) {
                slice.sla = new SLA();
                slice.sla.slaId = "SLA-" + System.currentTimeMillis();
                slice.sla.name = request.sla.name;
                slice.sla.latencyTargetMs = request.sla.latencyTargetMs;
                slice.sla.throughputTargetMbps = request.sla.throughputTargetMbps;
                slice.sla.availabilityTargetPercent = request.sla.availabilityTargetPercent;
                slice.sla.isActive = true;
            }

            slice.persist();

            ServiceResponse response = convertToService(slice);
            return Response.status(201).entity(response).build();

        } catch (Exception e) {
            Log.errorf("Error creating service: %s", e.getMessage(), e);
            return Response.serverError()
                    .entity(createErrorResponse("CREATION_ERROR", e.getMessage()))
                    .build();
        }
    }

    /**
     * Update a service
     */
    @PATCH
    @Path("/service/{id}")
    @Transactional
    public Response updateService(@PathParam("id") String id, ServiceRequest request) {
        Log.infof("TMF641: Updating service %s", id);

        NetworkSlice slice = NetworkSlice.find("sliceId", id).firstResult();

        if (slice == null) {
            return Response.status(404)
                    .entity(createErrorResponse("NOT_FOUND", "Service not found: " + id))
                    .build();
        }

        if (request.name != null) slice.name = request.name;
        if (request.description != null) slice.description = request.description;
        if (request.status != null) slice.status = NetworkSlice.SliceStatus.valueOf(request.status.toUpperCase());
        if (request.deviceIpAddress != null) slice.deviceIpAddress = request.deviceIpAddress;
        if (request.applicationServerIp != null) slice.applicationServerIp = request.applicationServerIp;
        slice.updatedAt = LocalDateTime.now();

        ServiceResponse response = convertToService(slice);
        return Response.ok(response).build();
    }

    /**
     * Delete a service
     */
    @DELETE
    @Path("/service/{id}")
    @Transactional
    public Response deleteService(@PathParam("id") String id) {
        Log.infof("TMF641: Deleting service %s", id);

        NetworkSlice slice = NetworkSlice.find("sliceId", id).firstResult();

        if (slice == null) {
            return Response.status(404)
                    .entity(createErrorResponse("NOT_FOUND", "Service not found: " + id))
                    .build();
        }

        slice.delete();
        return Response.noContent().build();
    }

    private ServiceListResponse convertToServiceList(List<NetworkSlice> slices) {
        ServiceListResponse response = new ServiceListResponse();
        for (NetworkSlice slice : slices) {
            response.add(convertToService(slice));
        }
        return response;
    }

    private ServiceResponse convertToService(NetworkSlice slice) {
        ServiceResponse response = new ServiceResponse();
        response.id = slice.sliceId;
        response.type = "NetworkSlice";
        response.name = slice.name;
        response.description = slice.description;
        response.serviceType = slice.sliceType.name();
        response.status = slice.status.name();
        response.startDate = slice.createdAt;
        response.href = tmfBaseUrl + "/serviceInventory/v4/service/" + slice.sliceId;

        // Add SLA info
        if (slice.sla != null) {
            response.slaId = slice.sla.slaId;
            response.slaName = slice.sla.name;
        }

        return response;
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

    // TMF641 Data Models

    public static class ServiceListResponse extends ArrayList<ServiceResponse> {
    }

    public static class ServiceRequest {
        public String id;
        public String name;
        public String description;
        public String serviceType;
        public String status = "ACTIVE";
        public String deviceId;
        public String deviceIpAddress;
        public String applicationServerIp;
        public SLARequest sla;
    }

    public static class SLARequest {
        public String name;
        public Double latencyTargetMs;
        public Double throughputTargetMbps;
        public Double availabilityTargetPercent;
    }

    public static class ServiceResponse {
        public String id;
        @com.fasterxml.jackson.annotation.JsonProperty("@type")
        public String type = "NetworkSlice";
        public String name;
        public String description;
        public String serviceType;
        public String status;
        public LocalDateTime startDate;
        public String slaId;
        public String slaName;
        public String href;
    }
}
