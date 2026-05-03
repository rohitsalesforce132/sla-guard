package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Port range specification.
 * Both from and to must be between 0 and 65535 inclusive.
 */
public class PortRange {

    @JsonProperty("from")
    private int from;

    @JsonProperty("to")
    private int to;

    public PortRange() {
    }

    public PortRange(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        if (from < 0 || from > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        if (to < 0 || to > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
        this.to = to;
    }

    public boolean isValid() {
        return from >= 0 && from <= 65535 && to >= 0 && to <= 65535 && from <= to;
    }
}
