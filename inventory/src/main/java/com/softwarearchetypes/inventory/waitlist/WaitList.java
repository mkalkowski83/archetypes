package com.softwarearchetypes.inventory.waitlist;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Predicate;

/**
 * Generic waitlist with configurable selection policy. Supports FIFO, Priority, and Criteria-based
 * selection.
 */
class WaitList<T> {

    private final WaitListId id;
    private final int capacity;
    private final Queue<WaitListEntry<T>> queue;
    private final WaitListSelectionPolicy<T> policy;

    WaitList(
            WaitListId id,
            int capacity,
            Queue<WaitListEntry<T>> queue,
            WaitListSelectionPolicy<T> policy) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.id = id;
        this.capacity = capacity;
        this.queue = queue;
        this.policy = policy;
    }

    static <T> WaitList<T> fifo(int capacity) {
        return new WaitList<>(
                WaitListId.random(), capacity, new LinkedList<>(), new FifoSelectionPolicy<>());
    }

    static <T> WaitList<T> priority(int capacity) {
        return new WaitList<>(
                WaitListId.random(),
                capacity,
                new PriorityQueue<>(),
                new PrioritySelectionPolicy<>());
    }

    static <T> WaitList<T> criteria(int capacity) {
        return new WaitList<>(
                WaitListId.random(), capacity, new LinkedList<>(), new CriteriaSelectionPolicy<>());
    }

    WaitListId id() {
        return id;
    }

    boolean add(WaitListEntry<T> entry) {
        if (queue.size() >= capacity) {
            throw new IllegalStateException(
                    "Waitlist full - capacity: " + capacity + ", current size: " + queue.size());
        }
        return queue.add(entry);
    }

    Optional<WaitListEntry<T>> poll() {
        return policy.selectNext(queue, SelectionContext.empty());
    }

    Optional<WaitListEntry<T>> selectNext(SelectionContext<T> context) {
        return policy.selectNext(queue, context);
    }

    Optional<WaitListEntry<T>> selectNext(Predicate<T> canFulfill) {
        return selectNext(SelectionContext.withPredicate(canFulfill));
    }

    boolean remove(WaitListEntry<T> entry) {
        return queue.remove(entry);
    }

    boolean removeById(WaitListEntryId entryId) {
        return queue.removeIf(e -> e.id().equals(entryId));
    }

    Optional<WaitListEntry<T>> peek() {
        return Optional.ofNullable(queue.peek());
    }

    int size() {
        return queue.size();
    }

    int capacity() {
        return capacity;
    }

    boolean isEmpty() {
        return queue.isEmpty();
    }

    boolean isFull() {
        return queue.size() >= capacity;
    }

    int availableCapacity() {
        return capacity - queue.size();
    }

    boolean contains(WaitListEntryId entryId) {
        return queue.stream().anyMatch(e -> e.id().equals(entryId));
    }

    List<WaitListEntry<T>> entries() {
        return List.copyOf(queue);
    }
}
