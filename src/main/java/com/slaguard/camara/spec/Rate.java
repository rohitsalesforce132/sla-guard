package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data rate with value and unit from CAMARA QoD API.
 */
public class Rate {

    @JsonProperty("value")
    private Long value;

    @JsonProperty("unit")
    private String unit; // bps, kbps, Mbps, Gbps, Tbps

    public Rate() {}

    public Rate(Long value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
