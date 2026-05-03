package com.slaguard.resource;

import com.slaguard.model.NetworkSlice;
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
 * REST API for Network Slice management
 */
@Path("/api/v1/slices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class SliceResource {

    @GET
    public List<NetworkSlice> listAll(
            @QueryParam("status") String status,
            @QueryParam("sliceType") String sliceType) {

        if (status != null && sliceType != null) {
            return NetworkSlice.list("status = ?1 and sliceType = ?2",
                    NetworkSlice.SliceStatus.valueOf(status.toUpperCase()),
                    NetworkSlice.SliceType.valueOf(sliceType.toUpperCase()));
        } else if (status != null) {
            return NetworkSlice.list("status", NetworkSlice.SliceStatus.valueOf(status.toUpperCase()));
        } else if (sliceType != null) {
            return NetworkSlice.list("sliceType", NetworkSlice.SliceType.valueOf(sliceType.toUpperCase()));
        }

        return NetworkSlice.listAll();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") String id) {
        NetworkSlice slice = NetworkSlice.find("sliceId", id).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        return Response.ok(slice).build();
    }

    @POST
    @Transactional
    public Response create(@Valid SliceCreateRequest request) {
        Log.infof("Creating slice: %s", request.name);

        NetworkSlice slice = new NetworkSlice();
        slice.sliceId = request.sliceId != null ? request.sliceId : "SLICE-" + System.currentTimeMillis();
        slice.name = request.name;
        slice.description = request.description;
        slice.sliceType = NetworkSlice.SliceType.valueOf(request.sliceType.toUpperCase());
        slice.status = NetworkSlice.SliceStatus.valueOf(request.status.toUpperCase());
        slice.deviceId = request.deviceId;
        slice.deviceIpAddress = request.deviceIpAddress;
        slice.applicationServerIp = request.applicationServerIp;
        slice.createdAt = LocalDateTime.now();
        slice.updatedAt = LocalDateTime.now();

        slice.persist();

        return Response.status(Response.Status.CREATED).entity(slice).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") String id, @Valid SliceUpdateRequest request) {
        NetworkSlice slice = NetworkSlice.find("sliceId", id).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        if (request.name != null) slice.name = request.name;
        if (request.description != null) slice.description = request.description;
        if (request.status != null) slice.status = NetworkSlice.SliceStatus.valueOf(request.status.toUpperCase());
        if (request.deviceIpAddress != null) slice.deviceIpAddress = request.deviceIpAddress;
        if (request.applicationServerIp != null) slice.applicationServerIp = request.applicationServerIp;
        slice.updatedAt = LocalDateTime.now();

        return Response.ok(slice).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") String id) {
        NetworkSlice slice = NetworkSlice.find("sliceId", id).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        slice.delete();
        return Response.noContent().build();
    }

    // Request/Response DTOs

    public static class SliceCreateRequest {
        public String sliceId;
        public String name;
        public String description;
        public String sliceType;
        public String status = "ACTIVE";
        public String deviceId;
        public String deviceIpAddress;
        public String applicationServerIp;
    }

    public static class SliceUpdateRequest {
        public String name;
        public String description;
        public String status;
        public String deviceIpAddress;
        public String applicationServerIp;
    }
}
