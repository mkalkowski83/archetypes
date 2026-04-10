package com.softwarearchetypes.inventory.waitlist;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;

/**
 * Quota-based selection policy. Ensures fair distribution across segments (e.g., VIP vs Standard).
 * Each segment has a quota - max number of entries that can be fulfilled per period.
 */
class QuotaSelectionPolicy<T> implements WaitListSelectionPolicy<T> {

    private final Map<String, Integer> quotaConfig;
    private final Map<String, Integer> allocated;
    private final Function<T, String> segmentExtractor;

    QuotaSelectionPolicy(Map<String, Integer> quotaConfig, Function<T, String> segmentExtractor) {
        this.quotaConfig = Map.copyOf(quotaConfig);
        this.allocated = new HashMap<>();
        this.segmentExtractor = segmentExtractor;
    }

    static <T> QuotaSelectionPolicy<T> of(
            Map<String, Integer> quotaConfig, Function<T, String> segmentExtractor) {
        return new QuotaSelectionPolicy<>(quotaConfig, segmentExtractor);
    }

    @Override
    public Optional<WaitListEntry<T>> selectNext(
            Queue<WaitListEntry<T>> queue, SelectionContext<T> context) {
        Iterator<WaitListEntry<T>> iterator = queue.iterator();

        while (iterator.hasNext()) {
            WaitListEntry<T> entry = iterator.next();

            String segment = segmentExtractor.apply(entry.payload());
            int current = allocated.getOrDefault(segment, 0);
            int quota = quotaConfig.getOrDefault(segment, Integer.MAX_VALUE);

            if (current < quota && context.canFulfill().test(entry.payload())) {
                allocated.put(segment, current + 1);
                iterator.remove();
                return Optional.of(entry);
            }
        }

        return Optional.empty();
    }

    void resetQuotas() {
        allocated.clear();
    }

    void resetQuota(String segment) {
        allocated.remove(segment);
    }

    int getAllocated(String segment) {
        return allocated.getOrDefault(segment, 0);
    }

    int getRemainingQuota(String segment) {
        int quota = quotaConfig.getOrDefault(segment, Integer.MAX_VALUE);
        int current = allocated.getOrDefault(segment, 0);
        return Math.max(0, quota - current);
    }
}
