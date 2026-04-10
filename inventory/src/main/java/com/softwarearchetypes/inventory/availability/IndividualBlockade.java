package com.softwarearchetypes.inventory.availability;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

record IndividualBlockade(BlockadeId id, OwnerId owner, Instant blockedAt, LockDuration duration)
        implements Blockade {

    public IndividualBlockade {
        Objects.requireNonNull(id, "BlockadeId cannot be null");
        Objects.requireNonNull(owner, "OwnerId cannot be null");
        Objects.requireNonNull(blockedAt, "blockedAt cannot be null");
        Objects.requireNonNull(duration, "LockDuration cannot be null");
    }

    public static IndividualBlockade create(OwnerId owner, LockDuration duration, Clock clock) {
        return new IndividualBlockade(BlockadeId.random(), owner, Instant.now(clock), duration);
    }

    public static IndividualBlockade create(OwnerId owner, LockDuration duration) {
        return create(owner, duration, Clock.systemUTC());
    }
}
