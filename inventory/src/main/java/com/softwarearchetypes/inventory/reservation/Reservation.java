package com.softwarearchetypes.inventory.reservation;

import com.softwarearchetypes.common.Version;
import com.softwarearchetypes.inventory.availability.BlockadeId;
import com.softwarearchetypes.inventory.availability.OwnerId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Reservation is the published language layer on top of resource availability. It represents a
 * customer's claim on one or more resources for a specific purpose.
 *
 * <p>A single reservation can block multiple resources (e.g., hotel room for 4 nights).
 */
class Reservation {

    private final ReservationId id;
    private final OwnerId owner;
    private final ReservationPurpose purpose;
    private final List<BlockadeId> blockadeIds;
    private final Instant createdAt;
    private ReservationStatus status;
    private final Version version;

    Reservation(
            ReservationId id,
            OwnerId owner,
            ReservationPurpose purpose,
            List<BlockadeId> blockadeIds,
            Instant createdAt,
            ReservationStatus status,
            Version version) {
        this.id = Objects.requireNonNull(id, "ReservationId cannot be null");
        this.owner = Objects.requireNonNull(owner, "OwnerId cannot be null");
        this.purpose = Objects.requireNonNull(purpose, "purpose cannot be null");
        this.blockadeIds =
                List.copyOf(Objects.requireNonNull(blockadeIds, "blockadeIds cannot be null"));
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.version = version;
    }

    static Reservation create(
            OwnerId owner,
            ReservationPurpose purpose,
            List<BlockadeId> blockadeIds,
            Instant createdAt) {
        return new Reservation(
                ReservationId.random(),
                owner,
                purpose,
                blockadeIds,
                createdAt,
                ReservationStatus.CONFIRMED,
                Version.initial());
    }

    ReservationId id() {
        return id;
    }

    OwnerId owner() {
        return owner;
    }

    ReservationPurpose purpose() {
        return purpose;
    }

    List<BlockadeId> blockadeIds() {
        return blockadeIds;
    }

    Instant createdAt() {
        return createdAt;
    }

    ReservationStatus status() {
        return status;
    }

    boolean isConfirmed() {
        return status == ReservationStatus.CONFIRMED;
    }

    boolean isCancelled() {
        return status == ReservationStatus.CANCELLED;
    }

    boolean isActive() {
        return isConfirmed();
    }

    void cancel() {
        if (!isActive()) {
            throw new IllegalStateException("Can only cancel active reservations");
        }
        this.status = ReservationStatus.CANCELLED;
    }

    Version version() {
        return version;
    }
}
