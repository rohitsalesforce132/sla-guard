package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Duration with value and unit from CAMARA QoD API.
 */
public class CamaraDuration {

    @JsonProperty("value")
    private Integer value;

    @JsonProperty("unit")
    private String unit; // Days, Hours, Minutes, Seconds, Milliseconds, Microseconds, Nanoseconds

    public CamaraDuration() {}

    public CamaraDuration(Integer value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
