package com.slaguard.tmf;

/**
 * TMF characteristic for key-value pairs
 */
public class TMFCharacteristic {

    public String name;
    public String value;
    public String valueType; // string, number, boolean, etc.

    public TMFCharacteristic() {}

    public TMFCharacteristic(String name, String value) {
        this.name = name;
        this.value = value;
        this.valueType = "string";
    }
}
