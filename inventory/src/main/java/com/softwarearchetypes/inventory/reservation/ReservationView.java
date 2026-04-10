package com.softwarearchetypes.inventory.reservation;

import com.softwarearchetypes.inventory.availability.BlockadeId;
import com.softwarearchetypes.inventory.availability.OwnerId;
import java.time.Instant;
import java.util.List;

public record ReservationView(
        ReservationId id,
        OwnerId owner,
        ReservationPurpose purpose,
        List<BlockadeId> blockadeIds,
        Instant createdAt,
        String status) {

    static ReservationView from(Reservation reservation) {
        return new ReservationView(
                reservation.id(),
                reservation.owner(),
                reservation.purpose(),
                reservation.blockadeIds(),
                reservation.createdAt(),
                reservation.status().name());
    }
}
