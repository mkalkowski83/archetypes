package com.softwarearchetypes.inventory.availability;

import com.softwarearchetypes.quantity.Quantity;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

record PoolBlockade(
        BlockadeId id, OwnerId owner, Quantity quantity, Instant blockedAt, LockDuration duration)
        implements Blockade {

    public PoolBlockade {
        Objects.requireNonNull(id, "BlockadeId cannot be null");
        Objects.requireNonNull(owner, "OwnerId cannot be null");
        Objects.requireNonNull(quantity, "Quantity cannot be null");
        Objects.requireNonNull(blockedAt, "blockedAt cannot be null");
        Objects.requireNonNull(duration, "LockDuration cannot be null");
    }

    public static PoolBlockade create(
            OwnerId owner, Quantity quantity, LockDuration duration, Clock clock) {
        return new PoolBlockade(BlockadeId.random(), owner, quantity, Instant.now(clock), duration);
    }

    public static PoolBlockade create(OwnerId owner, Quantity quantity, LockDuration duration) {
        return create(owner, quantity, duration, Clock.systemUTC());
    }
}
