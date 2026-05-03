package com.slaguard.resource;

import com.slaguard.engine.SliceHealthCalculator;
import com.slaguard.model.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * REST API for dashboard and summary data
 */
@Path("/api/v1/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class DashboardResource {

    @Inject
    SliceHealthCalculator healthCalculator;

    @GET
    public Response getDashboard() {
        DashboardSummary summary = new DashboardSummary();

        // Count slices
        List<NetworkSlice> allSlices = NetworkSlice.listAll();
        summary.totalSlices = allSlices.size();

        summary.activeSlices = (int) NetworkSlice.count("status", NetworkSlice.SliceStatus.ACTIVE);

        // Calculate health and status for each slice
        List<DashboardSummary.SliceHealth> sliceHealthList = new ArrayList<>();

        for (NetworkSlice slice : allSlices) {
            DashboardSummary.SliceHealth health = new DashboardSummary.SliceHealth();
            health.sliceId = slice.sliceId;
            health.sliceName = slice.name;

            // Get latest metrics
            List<SLAMetric> metrics = SLAMetric.list(
                    "slice = ?1 ORDER BY timestamp DESC",
                    slice
            ).subList(0, Math.min(10, (int) SLAMetric.count("slice", slice)));

            // Calculate health score
            double healthScore = healthCalculator.calculateHealth(slice, metrics);
            health.healthScore = healthScore;

            // Determine status
            health.status = slice.status.name();
            health.slaStatus = determineOverallStatus(metrics).name();
            health.lastUpdated = metrics.isEmpty() ? null : metrics.get(0).timestamp;

            // Count active remediations
            health.activeRemediations = (int) RemediationAction.count(
                    "slice = ?1 and status = ?2",
                    slice, RemediationAction.ActionStatus.IN_PROGRESS);

            sliceHealthList.add(health);

            // Update counters
            if (health.slaStatus.equals("BREACHED")) {
                summary.slicesWithBreaches++;
            } else if (health.slaStatus.equals("CRITICAL")) {
                summary.criticalBreaches++;
            } else if (health.slaStatus.equals("WARNING")) {
                summary.warnings++;
            } else {
                summary.compliantSlices++;
            }
        }

        summary.sliceHealthList = sliceHealthList;

        // Calculate average health score
        if (!sliceHealthList.isEmpty()) {
            double totalHealth = sliceHealthList.stream()
                    .mapToDouble(h -> h.healthScore)
                    .sum();
            summary.averageHealthScore = totalHealth / sliceHealthList.size();
        }

        // Get recent breaches
        List<SLABreach> recentBreaches = SLABreach.list(
                "ORDER BY detectedAt DESC",
                SLABreach.class
        ).stream().limit(10).toList();

        summary.recentBreaches = recentBreaches.stream()
                .map(b -> {
                    DashboardSummary.RecentBreach rb = new DashboardSummary.RecentBreach();
                    rb.breachId = b.breachId;
                    rb.sliceId = b.slice.sliceId;
                    rb.sliceName = b.slice.name;
                    rb.metricType = b.metricType.name();
                    rb.severity = b.severity.name();
                    rb.actualValue = b.actualValue;
                    rb.thresholdValue = b.thresholdValue;
                    rb.detectedAt = b.detectedAt;
                    rb.resolved = b.resolved;
                    return rb;
                })
                .toList();

        // Get recent remediations
        List<RemediationAction> recentRemediations = RemediationAction.list(
                "ORDER BY initiatedAt DESC",
                RemediationAction.class
        ).stream().limit(10).toList();

        summary.recentRemediations = recentRemediations.stream()
                .map(r -> {
                    DashboardSummary.RecentRemediation rr = new DashboardSummary.RecentRemediation();
                    rr.actionId = r.actionId;
                    rr.breachId = r.breach != null ? r.breach.breachId : null;
                    rr.sliceId = r.slice.sliceId;
                    rr.actionType = r.type.name();
                    rr.status = r.status.name();
                    rr.initiatedAt = r.initiatedAt;
                    rr.successful = r.successful;
                    return rr;
                })
                .toList();

        return Response.ok(summary).build();
    }

    /**
     * Get health for a specific slice
     */
    @GET
    @Path("/slice/{sliceId}/health")
    public Response getSliceHealth(@PathParam("sliceId") String sliceId) {
        NetworkSlice slice = NetworkSlice.find("sliceId", sliceId).firstResult();

        if (slice == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Slice not found\"}")
                    .build();
        }

        List<SLAMetric> metrics = SLAMetric.list(
                "slice = ?1 ORDER BY timestamp DESC",
                slice
        );

        double healthScore = healthCalculator.calculateHealth(slice, metrics);
        SliceHealthCalculator.HealthCategory category = healthCalculator.getHealthCategory(healthScore);

        HealthResponse response = new HealthResponse();
        response.sliceId = sliceId;
        response.sliceName = slice.name;
        response.healthScore = healthScore;
        response.category = category.name();
        response.status = determineOverallStatus(metrics).name();

        return Response.ok(response).build();
    }

    private SLAMetric.SLAStatus determineOverallStatus(List<SLAMetric> metrics) {
        if (metrics.isEmpty()) {
            return SLAMetric.SLAStatus.COMPLIANT;
        }

        boolean hasBreach = metrics.stream().anyMatch(m -> m.status == SLAMetric.SLAStatus.BREACHED);
        if (hasBreach) return SLAMetric.SLAStatus.BREACHED;

        boolean hasCritical = metrics.stream().anyMatch(m -> m.status == SLAMetric.SLAStatus.CRITICAL);
        if (hasCritical) return SLAMetric.SLAStatus.CRITICAL;

        boolean hasWarning = metrics.stream().anyMatch(m -> m.status == SLAMetric.SLAStatus.WARNING);
        if (hasWarning) return SLAMetric.SLAStatus.WARNING;

        return SLAMetric.SLAStatus.COMPLIANT;
    }

    // Response DTOs

    public static class HealthResponse {
        public String sliceId;
        public String sliceName;
        public double healthScore;
        public String category;
        public String status;
    }
}
