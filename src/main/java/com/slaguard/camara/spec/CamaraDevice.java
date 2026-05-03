package com.slaguard.camara.spec;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Device identifier for CAMARA QoD API.
 * Device can be identified by at least one of: phoneNumber, networkAccessIdentifier, ipv4Address, or ipv6Address.
 */
public class CamaraDevice {

    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @JsonProperty("networkAccessIdentifier")
    private String networkAccessIdentifier;

    @JsonProperty("ipv4Address")
    private DeviceIpv4Addr ipv4Address;

    @JsonProperty("ipv6Address")
    private String ipv6Address;

    public CamaraDevice() {
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNetworkAccessIdentifier() {
        return networkAccessIdentifier;
    }

    public void setNetworkAccessIdentifier(String networkAccessIdentifier) {
        this.networkAccessIdentifier = networkAccessIdentifier;
    }

    public DeviceIpv4Addr getIpv4Address() {
        return ipv4Address;
    }

    public void setIpv4Address(DeviceIpv4Addr ipv4Address) {
        this.ipv4Address = ipv4Address;
    }

    public String getIpv6Address() {
        return ipv6Address;
    }

    public void setIpv6Address(String ipv6Address) {
        this.ipv6Address = ipv6Address;
    }

    public boolean isValid() {
        return phoneNumber != null || networkAccessIdentifier != null || ipv4Address != null || ipv6Address != null;
    }
}
