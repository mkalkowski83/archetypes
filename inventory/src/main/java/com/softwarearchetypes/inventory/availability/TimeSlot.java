package com.softwarearchetypes.inventory.availability;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public record TimeSlot(Instant from, Instant to) {

    public TimeSlot {
        Objects.requireNonNull(from, "TimeSlot 'from' cannot be null");
        Objects.requireNonNull(to, "TimeSlot 'to' cannot be null");
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("TimeSlot 'from' must be before 'to'");
        }
    }

    public static TimeSlot of(Instant from, Instant to) {
        return new TimeSlot(from, to);
    }

    public static TimeSlot of(LocalDateTime from, LocalDateTime to) {
        return new TimeSlot(from.toInstant(ZoneOffset.UTC), to.toInstant(ZoneOffset.UTC));
    }

    public static TimeSlot ofDay(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return of(start, end);
    }

    public boolean contains(Instant instant) {
        return !instant.isBefore(from) && instant.isBefore(to);
    }

    public boolean overlaps(TimeSlot other) {
        return this.from.isBefore(other.to) && other.from.isBefore(this.to);
    }

    public boolean isAdjacentTo(TimeSlot other) {
        return this.to.equals(other.from) || other.to.equals(this.from);
    }

    public Duration duration() {
        return Duration.between(from, to);
    }
}
