package com.softwarearchetypes.inventory.availability;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

record TemporalBlockade(BlockadeId id, OwnerId owner, Instant blockedAt, LockDuration duration)
        implements Blockade {

    public TemporalBlockade {
        Objects.requireNonNull(id, "BlockadeId cannot be null");
        Objects.requireNonNull(owner, "OwnerId cannot be null");
        Objects.requireNonNull(blockedAt, "blockedAt cannot be null");
        Objects.requireNonNull(duration, "LockDuration cannot be null");
    }

    public static TemporalBlockade create(OwnerId owner, LockDuration duration, Clock clock) {
        return new TemporalBlockade(BlockadeId.random(), owner, Instant.now(clock), duration);
    }

    public static TemporalBlockade create(OwnerId owner, LockDuration duration) {
        return create(owner, duration, Clock.systemUTC());
    }
}
