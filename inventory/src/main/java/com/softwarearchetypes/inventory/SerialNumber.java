package com.softwarearchetypes.inventory;

/** SerialNumber uniquely identifies an individual instance. */
public interface SerialNumber {

    String type();

    String value();

    static SerialNumber of(String value) {
        return new TextualSerialNumber(value);
    }
}
