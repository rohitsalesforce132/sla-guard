package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Device IPv4 address with NAT awareness.
 * Must have publicAddress + privateAddress OR publicAddress + publicPort.
 */
public class DeviceIpv4Addr {

    @JsonProperty("publicAddress")
    private String publicAddress;

    @JsonProperty("privateAddress")
    private String privateAddress;

    @JsonProperty("publicPort")
    private Integer publicPort;

    public DeviceIpv4Addr() {
    }

    public String getPublicAddress() {
        return publicAddress;
    }

    public void setPublicAddress(String publicAddress) {
        this.publicAddress = publicAddress;
    }

    public String getPrivateAddress() {
        return privateAddress;
    }

    public void setPrivateAddress(String privateAddress) {
        this.privateAddress = privateAddress;
    }

    public Integer getPublicPort() {
        return publicPort;
    }

    public void setPublicPort(Integer publicPort) {
        this.publicPort = publicPort;
    }

    public boolean isValid() {
        if (publicAddress == null) {
            return false;
        }
        return (privateAddress != null) || (publicPort != null);
    }
}
