package com.softwarearchetypes.inventory.waitlist;

import java.util.Optional;
import java.util.Queue;

/** FIFO - First In, First Out. Simply returns the head of the queue. */
class FifoSelectionPolicy<T> implements WaitListSelectionPolicy<T> {

    @Override
    public Optional<WaitListEntry<T>> selectNext(
            Queue<WaitListEntry<T>> queue, SelectionContext<T> context) {
        return Optional.ofNullable(queue.poll());
    }
}
