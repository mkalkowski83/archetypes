package com.softwarearchetypes.inventory.waitlist;

import java.util.Optional;
import java.util.Queue;

/**
 * Strategy for selecting next entry from waitlist.
 *
 * @param <T> type of payload in waitlist entries
 */
interface WaitListSelectionPolicy<T> {

    /**
     * Select next entry from queue based on policy logic.
     *
     * @param queue mutable queue (policy may modify it - remove element)
     * @param context selection context (predicate, metadata, timestamp)
     * @return selected entry wrapped in Optional, empty if none can be selected
     */
    Optional<WaitListEntry<T>> selectNext(
            Queue<WaitListEntry<T>> queue, SelectionContext<T> context);
}
