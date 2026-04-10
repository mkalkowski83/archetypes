package com.softwarearchetypes.graphs.cycles;

record OwnerId(String value) {

    static OwnerId of(String value) {
        return new OwnerId(value);
    }

    static OwnerId empty() {
        return new OwnerId("");
    }

    boolean isEmpty() {
        return value == null || value.isEmpty();
    }
}
