package com.softwarearchetypes.inventory.waitlist;

import java.time.Instant;
import java.util.Objects;

/**
 * Generic entry in waitlist - payload can be ANYTHING.
 *
 * @param <T> type of payload (LockRequest, OrderRequest, UUID, etc.)
 */
record WaitListEntry<T>(WaitListEntryId id, T payload, int priority, Instant addedAt)
        implements Comparable<WaitListEntry<T>> {

    private static final int DEFAULT_PRIORITY = 1;

    WaitListEntry {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        Objects.requireNonNull(addedAt, "addedAt cannot be null");
    }

    static <T> WaitListEntry<T> of(T payload) {
        return new WaitListEntry<>(
                WaitListEntryId.random(), payload, DEFAULT_PRIORITY, Instant.now());
    }

    static <T> WaitListEntry<T> of(T payload, int priority) {
        return new WaitListEntry<>(WaitListEntryId.random(), payload, priority, Instant.now());
    }

    static <T> WaitListEntry<T> of(T payload, Instant addedAt) {
        return new WaitListEntry<>(WaitListEntryId.random(), payload, DEFAULT_PRIORITY, addedAt);
    }

    static <T> WaitListEntry<T> of(T payload, int priority, Instant addedAt) {
        return new WaitListEntry<>(WaitListEntryId.random(), payload, priority, addedAt);
    }

    @Override
    public int compareTo(WaitListEntry<T> other) {
        // First by priority (higher priority = lower number = first)
        int priorityComp = Integer.compare(this.priority, other.priority);
        if (priorityComp != 0) {
            return priorityComp;
        }
        // Within same priority: FIFO (by timestamp)
        return this.addedAt.compareTo(other.addedAt);
    }
}
