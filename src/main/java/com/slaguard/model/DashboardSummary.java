package com.slaguard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummary {

    public int totalSlices;
    public int activeSlices;
    public int slicesWithBreaches;
    public int criticalBreaches;
    public int warnings;
    public int compliantSlices;
    public double averageHealthScore;
    public List<SliceHealth> sliceHealthList;
    public List<RecentBreach> recentBreaches;
    public List<RecentRemediation> recentRemediations;
    public LocalDateTime generatedAt;

    public DashboardSummary() {
        this.generatedAt = LocalDateTime.now();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SliceHealth {
        public String sliceId;
        public String sliceName;
        public double healthScore;
        public String status;
        public String slaStatus;
        public int activeRemediations;
        public LocalDateTime lastUpdated;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecentBreach {
        public String breachId;
        public String sliceId;
        public String sliceName;
        public String metricType;
        public String severity;
        public Double actualValue;
        public Double thresholdValue;
        public LocalDateTime detectedAt;
        public boolean resolved;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RecentRemediation {
        public String actionId;
        public String breachId;
        public String sliceId;
        public String actionType;
        public String status;
        public LocalDateTime initiatedAt;
        public boolean successful;
    }
}
