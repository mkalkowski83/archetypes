package com.softwarearchetypes.inventory.reservation;

import com.softwarearchetypes.inventory.availability.OwnerId;
import java.util.List;
import java.util.Optional;

interface ReservationRepository {

    void save(Reservation reservation);

    Optional<Reservation> findById(ReservationId id);

    List<Reservation> findByOwner(OwnerId owner);

    List<Reservation> findActive();

    void delete(ReservationId id);
}
