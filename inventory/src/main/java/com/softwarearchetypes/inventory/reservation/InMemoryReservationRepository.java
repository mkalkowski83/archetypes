package com.softwarearchetypes.inventory.reservation;

import com.softwarearchetypes.inventory.availability.OwnerId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryReservationRepository implements ReservationRepository {

    private final Map<ReservationId, Reservation> storage = new HashMap<>();

    @Override
    public void save(Reservation reservation) {
        storage.put(reservation.id(), reservation);
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Reservation> findByOwner(OwnerId owner) {
        return storage.values().stream().filter(r -> r.owner().equals(owner)).toList();
    }

    @Override
    public List<Reservation> findActive() {
        return storage.values().stream().filter(Reservation::isActive).toList();
    }

    @Override
    public void delete(ReservationId id) {
        storage.remove(id);
    }
}
