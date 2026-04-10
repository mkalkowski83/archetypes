package com.softwarearchetypes.inventory.reservation;

import java.util.Objects;
import java.util.UUID;

public record ReservationId(UUID id) {

    public ReservationId {
        Objects.requireNonNull(id, "ReservationId cannot be null");
    }

    public static ReservationId random() {
        return new ReservationId(UUID.randomUUID());
    }

    public static ReservationId of(UUID id) {
        return new ReservationId(id);
    }

    public static ReservationId of(String id) {
        return new ReservationId(UUID.fromString(id));
    }
}
