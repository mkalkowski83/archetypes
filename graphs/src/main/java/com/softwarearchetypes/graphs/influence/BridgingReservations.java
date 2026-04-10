package com.softwarearchetypes.graphs.influence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

record BridgingReservations(Set<Reservation> reservations) {
    BridgingReservations(Set<Reservation> reservations) {
        this.reservations = Collections.unmodifiableSet(new HashSet<>(reservations));
    }

    boolean isBridging(Reservation reservation) {
        return reservations.contains(reservation);
    }

    int count() {
        return reservations.size();
    }

    boolean isEmpty() {
        return reservations.isEmpty();
    }
}
