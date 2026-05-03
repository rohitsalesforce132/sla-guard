package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Port specification for device or application server.
 * Must contain at least one of: ranges or ports.
 */
public class PortsSpec {

    @JsonProperty("ranges")
    private List<PortRange> ranges = new ArrayList<>();

    @JsonProperty("ports")
    private List<Integer> ports = new ArrayList<>();

    public PortsSpec() {
    }

    public List<PortRange> getRanges() {
        return ranges;
    }

    public void setRanges(List<PortRange> ranges) {
        this.ranges = ranges;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }

    public boolean isValid() {
        return (ranges != null && !ranges.isEmpty()) || (ports != null && !ports.isEmpty());
    }
}
