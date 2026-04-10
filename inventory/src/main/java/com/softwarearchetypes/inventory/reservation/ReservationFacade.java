package com.softwarearchetypes.inventory.reservation;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.inventory.InventoryFacade;
import com.softwarearchetypes.inventory.LockCommand;
import com.softwarearchetypes.inventory.availability.AvailabilityFacade;
import com.softwarearchetypes.inventory.availability.BlockadeId;
import com.softwarearchetypes.inventory.availability.OwnerId;
import com.softwarearchetypes.inventory.availability.UnlockRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * ReservationFacade is the published language layer for reservations. It translates between
 * business reservation concepts and inventory locks.
 */
public class ReservationFacade {

    private final InventoryFacade inventoryFacade;
    private final AvailabilityFacade availabilityFacade;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    ReservationFacade(
            InventoryFacade inventoryFacade,
            AvailabilityFacade availabilityFacade,
            ReservationRepository reservationRepository,
            Clock clock) {
        this.inventoryFacade = inventoryFacade;
        this.availabilityFacade = availabilityFacade;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    public Result<String, ReservationId> handle(ReserveRequest request) {
        // 1. Create LockCommand
        LockCommand lockCmd =
                new LockCommand(
                        request.productId(),
                        request.quantity(),
                        request.owner(),
                        request.resourceSpecification());

        // 2. Lock through InventoryFacade
        Result<String, List<BlockadeId>> lockResult = inventoryFacade.handle(lockCmd);

        if (lockResult.failure()) {
            return Result.failure(lockResult.getFailure());
        }

        // 3. Create Reservation
        Instant now = Instant.now(clock);
        Reservation reservation =
                Reservation.create(
                        request.owner(), request.purpose(), lockResult.getSuccess(), now);

        reservationRepository.save(reservation);
        return Result.success(reservation.id());
    }

    public Result<String, ReservationId> cancel(ReservationId reservationId, OwnerId requester) {
        Reservation reservation =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Reservation not found: " + reservationId));

        if (!reservation.owner().equals(requester)) {
            return Result.failure("Not authorized to cancel this reservation");
        }

        if (!reservation.isActive()) {
            return Result.failure("Reservation is not active");
        }

        // Unlock all blockades
        for (BlockadeId blockadeId : reservation.blockadeIds()) {
            availabilityFacade.handle(UnlockRequest.of(requester, blockadeId));
        }

        reservation.cancel();
        reservationRepository.save(reservation);

        return Result.success(reservationId);
    }

    public Optional<ReservationView> findById(ReservationId id) {
        return reservationRepository.findById(id).map(ReservationView::from);
    }

    public List<ReservationView> findByOwner(OwnerId owner) {
        return reservationRepository.findByOwner(owner).stream()
                .map(ReservationView::from)
                .toList();
    }

    public List<ReservationView> findActive() {
        return reservationRepository.findActive().stream().map(ReservationView::from).toList();
    }
}
