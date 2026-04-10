package com.softwarearchetypes.inventory;

import java.util.Objects;

record TextualSerialNumber(String value) implements SerialNumber {

    TextualSerialNumber {
        Objects.requireNonNull(value, "SerialNumber value cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("SerialNumber value cannot be blank");
        }
    }

    static TextualSerialNumber of(String value) {
        return new TextualSerialNumber(value);
    }

    @Override
    public String type() {
        return "TEXTUAL";
    }

    @Override
    public String toString() {
        return value;
    }
}
