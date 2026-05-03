package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Full QoS Profile details from CAMARA QoD API.
 * Maps to the QosProfile schema in the OpenAPI spec.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QosProfile {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private QosProfileStatus status;

    @JsonProperty("targetMinUpstreamRate")
    private Rate targetMinUpstreamRate;

    @JsonProperty("maxUpstreamRate")
    private Rate maxUpstreamRate;

    @JsonProperty("maxUpstreamBurstRate")
    private Rate maxUpstreamBurstRate;

    @JsonProperty("targetMinDownstreamRate")
    private Rate targetMinDownstreamRate;

    @JsonProperty("maxDownstreamRate")
    private Rate maxDownstreamRate;

    @JsonProperty("maxDownstreamBurstRate")
    private Rate maxDownstreamBurstRate;

    @JsonProperty("minDuration")
    private CamaraDuration minDuration;

    @JsonProperty("maxDuration")
    private CamaraDuration maxDuration;

    @JsonProperty("priority")
    private Integer priority;

    @JsonProperty("packetDelayBudget")
    private CamaraDuration packetDelayBudget;

    @JsonProperty("jitter")
    private CamaraDuration jitter;

    @JsonProperty("packetErrorLossRate")
    private Integer packetErrorLossRate;

    public QosProfile() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public QosProfileStatus getStatus() { return status; }
    public void setStatus(QosProfileStatus status) { this.status = status; }

    public Rate getTargetMinUpstreamRate() { return targetMinUpstreamRate; }
    public void setTargetMinUpstreamRate(Rate targetMinUpstreamRate) { this.targetMinUpstreamRate = targetMinUpstreamRate; }

    public Rate getMaxUpstreamRate() { return maxUpstreamRate; }
    public void setMaxUpstreamRate(Rate maxUpstreamRate) { this.maxUpstreamRate = maxUpstreamRate; }

    public Rate getMaxUpstreamBurstRate() { return maxUpstreamBurstRate; }
    public void setMaxUpstreamBurstRate(Rate maxUpstreamBurstRate) { this.maxUpstreamBurstRate = maxUpstreamBurstRate; }

    public Rate getTargetMinDownstreamRate() { return targetMinDownstreamRate; }
    public void setTargetMinDownstreamRate(Rate targetMinDownstreamRate) { this.targetMinDownstreamRate = targetMinDownstreamRate; }

    public Rate getMaxDownstreamRate() { return maxDownstreamRate; }
    public void setMaxDownstreamRate(Rate maxDownstreamRate) { this.maxDownstreamRate = maxDownstreamRate; }

    public Rate getMaxDownstreamBurstRate() { return maxDownstreamBurstRate; }
    public void setMaxDownstreamBurstRate(Rate maxDownstreamBurstRate) { this.maxDownstreamBurstRate = maxDownstreamBurstRate; }

    public CamaraDuration getMinDuration() { return minDuration; }
    public void setMinDuration(CamaraDuration minDuration) { this.minDuration = minDuration; }

    public CamaraDuration getMaxDuration() { return maxDuration; }
    public void setMaxDuration(CamaraDuration maxDuration) { this.maxDuration = maxDuration; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public CamaraDuration getPacketDelayBudget() { return packetDelayBudget; }
    public void setPacketDelayBudget(CamaraDuration packetDelayBudget) { this.packetDelayBudget = packetDelayBudget; }

    public CamaraDuration getJitter() { return jitter; }
    public void setJitter(CamaraDuration jitter) { this.jitter = jitter; }

    public Integer getPacketErrorLossRate() { return packetErrorLossRate; }
    public void setPacketErrorLossRate(Integer packetErrorLossRate) { this.packetErrorLossRate = packetErrorLossRate; }
}
