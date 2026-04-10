package com.softwarearchetypes.inventory.waitlist;

import java.util.Optional;
import java.util.Queue;

/**
 * Priority-based selection. Relies on PriorityQueue ordering (entries must be Comparable). Higher
 * priority (lower number) comes first, FIFO within same priority.
 */
class PrioritySelectionPolicy<T> implements WaitListSelectionPolicy<T> {

    @Override
    public Optional<WaitListEntry<T>> selectNext(
            Queue<WaitListEntry<T>> queue, SelectionContext<T> context) {
        return Optional.ofNullable(queue.poll());
    }
}
