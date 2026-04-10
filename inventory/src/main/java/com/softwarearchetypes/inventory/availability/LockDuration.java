package com.softwarearchetypes.inventory.availability;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public sealed interface LockDuration permits LockDuration.Indefinite, LockDuration.Timed {

    boolean isActive(Instant now, Instant blockedAt);

    boolean isExpired(Instant now, Instant blockedAt);

    record Indefinite() implements LockDuration {
        @Override
        public boolean isActive(Instant now, Instant blockedAt) {
            return true;
        }

        @Override
        public boolean isExpired(Instant now, Instant blockedAt) {
            return false;
        }
    }

    record Timed(Duration duration) implements LockDuration {
        public Timed {
            Objects.requireNonNull(duration, "Duration cannot be null");
            if (duration.isNegative() || duration.isZero()) {
                throw new IllegalArgumentException("Duration must be positive");
            }
        }

        @Override
        public boolean isActive(Instant now, Instant blockedAt) {
            return now.isBefore(blockedAt.plus(duration));
        }

        @Override
        public boolean isExpired(Instant now, Instant blockedAt) {
            return !isActive(now, blockedAt);
        }
    }

    static LockDuration indefinite() {
        return new Indefinite();
    }

    static LockDuration of(Duration duration) {
        return new Timed(duration);
    }

    static LockDuration ofMinutes(long minutes) {
        return new Timed(Duration.ofMinutes(minutes));
    }

    static LockDuration ofHours(long hours) {
        return new Timed(Duration.ofHours(hours));
    }

    static LockDuration ofDays(long days) {
        return new Timed(Duration.ofDays(days));
    }
}
