package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Application server identifier for CAMARA QoD API.
 * Must have at least one of: ipv4Address or ipv6Address.
 */
public class CamaraApplicationServer {

    @JsonProperty("ipv4Address")
    private String ipv4Address;

    @JsonProperty("ipv6Address")
    private String ipv6Address;

    public CamaraApplicationServer() {
    }

    public String getIpv4Address() {
        return ipv4Address;
    }

    public void setIpv4Address(String ipv4Address) {
        this.ipv4Address = ipv4Address;
    }

    public String getIpv6Address() {
        return ipv6Address;
    }

    public void setIpv6Address(String ipv6Address) {
        this.ipv6Address = ipv6Address;
    }

    public boolean isValid() {
        return ipv4Address != null || ipv6Address != null;
    }
}
