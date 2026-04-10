package com.softwarearchetypes.inventory.waitlist;

import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;

/**
 * Criteria-based selection. Finds first entry that matches the predicate from context. Order is
 * preserved (FIFO among matching entries).
 */
class CriteriaSelectionPolicy<T> implements WaitListSelectionPolicy<T> {

    @Override
    public Optional<WaitListEntry<T>> selectNext(
            Queue<WaitListEntry<T>> queue, SelectionContext<T> context) {
        if (context.canFulfill() == null) {
            throw new IllegalArgumentException(
                    "CriteriaSelectionPolicy requires predicate in context");
        }

        Iterator<WaitListEntry<T>> iterator = queue.iterator();

        while (iterator.hasNext()) {
            WaitListEntry<T> entry = iterator.next();

            if (context.canFulfill().test(entry.payload())) {
                iterator.remove();
                return Optional.of(entry);
            }
        }

        return Optional.empty();
    }
}
