package com.softwarearchetypes.ordering;

import java.util.UUID;

public record OrderLineId(String value) {

    public static OrderLineId of(String value) {
        return new OrderLineId(value);
    }

    public static OrderLineId generate() {
        return new OrderLineId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
