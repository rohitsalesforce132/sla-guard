package com.slaguard.resource;

import com.slaguard.engine.SLAMonitoringEngine;
import com.slaguard.model.NetworkSlice;
import com.slaguard.model.SLAMetric;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST API for metric ingestion and querying
 */
@Path("/api/v1/slices/{sliceId}/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class MetricsResource {

    @Inject
    SLAMonitoringEngine monitoringEngine;

    @POST
    @Transactional
    public Response ingest(@PathParam("sliceId") String sliceId, @Valid MetricIngestRequest request) {
        Log.debugf("Ingesting metric for slice %s: %s = %s %s",
                sliceId, request.metricType, request.value, request.unit);

        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        SLAMetric metric = new SLAMetric(
                slice,
                SLAMetric.MetricType.valueOf(request.metricType.toUpperCase()),
                request.value,
                request.unit
        );

        if (request.timestamp != null) {
            metric.timestamp = request.timestamp;
        }

        // Ingest into monitoring engine
        monitoringEngine.ingestMetric(slice, metric);

        return Response.status(Response.Status.CREATED).entity(metric).build();
    }

    @POST
    @Path("/batch")
    @Transactional
    public Response ingestBatch(@PathParam("sliceId") String sliceId, List<MetricIngestRequest> requests) {
        Log.infof("Batch ingesting %d metrics for slice %s", requests.size(), sliceId);

        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        for (MetricIngestRequest request : requests) {
            SLAMetric metric = new SLAMetric(
                    slice,
                    SLAMetric.MetricType.valueOf(request.metricType.toUpperCase()),
                    request.value,
                    request.unit
            );

            if (request.timestamp != null) {
                metric.timestamp = request.timestamp;
            }

            monitoringEngine.ingestMetric(slice, metric);
        }

        return Response.status(Response.Status.CREATED)
                .entity("{\"message\":\"Ingested " + requests.size() + " metrics\"}")
                .build();
    }

    @GET
    public List<SLAMetric> list(
            @PathParam("sliceId") String sliceId,
            @QueryParam("metricType") String metricType,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null) {
            return List.of();
        }

        if (metricType != null) {
            return SLAMetric.list(
                    "slice = ?1 and metricType = ?2 ORDER BY timestamp DESC",
                    slice,
                    SLAMetric.MetricType.valueOf(metricType.toUpperCase())
            ).stream().limit(limit).toList();
        }

        return SLAMetric.list(
                "slice = ?1 ORDER BY timestamp DESC",
                slice
        ).stream().limit(limit).toList();
    }

    @GET
    @Path("/latest")
    public Response getLatest(@PathParam("sliceId") String sliceId) {
        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        List<SLAMetric> metrics = SLAMetric.list(
                "slice = ?1 ORDER BY timestamp DESC",
                slice
        ).subList(0, Math.min(5, SLAMetric.count("slice", slice)));

        return Response.ok(metrics).build();
    }

    // Request DTOs

    public static class MetricIngestRequest {
        public String metricType; // LATENCY, THROUGHPUT, JITTER, PACKET_LOSS, AVAILABILITY
        public Double value;
        public String unit; // ms, Mbps, %
        public LocalDateTime timestamp;
    }
}
