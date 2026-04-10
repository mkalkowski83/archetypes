package com.softwarearchetypes.ordering;

import java.util.UUID;

public record OrderId(String value) {

    public static OrderId of(String value) {
        return new OrderId(value);
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
