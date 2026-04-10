package com.softwarearchetypes.graphs.influence;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

record InfluenceZone(Set<Reservation> reservations) {
    InfluenceZone(Set<Reservation> reservations) {
        this.reservations = Collections.unmodifiableSet(new HashSet<>(reservations));
    }

    int countReservationsToNegotiateWith(Reservation reservation) {
        if (!reservations.contains(reservation)) {
            return 0;
        }
        return reservations.size() - 1;
    }

    Set<Reservation> getReservationsToNegotiateWith(Reservation reservation) {
        if (!reservations.contains(reservation)) {
            return Set.of();
        }
        Set<Reservation> toNegotiate = new HashSet<>(reservations);
        toNegotiate.remove(reservation);
        return Collections.unmodifiableSet(toNegotiate);
    }

    int size() {
        return reservations.size();
    }
}
