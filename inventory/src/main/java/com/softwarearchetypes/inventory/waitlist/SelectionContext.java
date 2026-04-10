package com.softwarearchetypes.inventory.waitlist;

import java.time.Instant;
import java.util.Map;
import java.util.function.Predicate;

/** Context passed to selection policy - contains everything policy needs to make a decision. */
record SelectionContext<T>(Predicate<T> canFulfill, Map<String, Object> metadata, Instant now) {

    static <T> SelectionContext<T> empty() {
        return new SelectionContext<>(x -> true, Map.of(), Instant.now());
    }

    static <T> SelectionContext<T> withPredicate(Predicate<T> predicate) {
        return new SelectionContext<>(predicate, Map.of(), Instant.now());
    }

    static <T> SelectionContext<T> withMetadata(Map<String, Object> metadata) {
        return new SelectionContext<>(x -> true, metadata, Instant.now());
    }

    static <T> SelectionContext<T> of(Predicate<T> predicate, Map<String, Object> metadata) {
        return new SelectionContext<>(predicate, metadata, Instant.now());
    }

    static <T> SelectionContext<T> of(
            Predicate<T> predicate, Map<String, Object> metadata, Instant now) {
        return new SelectionContext<>(predicate, metadata, now);
    }
}
