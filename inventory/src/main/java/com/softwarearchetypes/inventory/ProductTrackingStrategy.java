package com.softwarearchetypes.inventory;

/**
 * Local representation of product tracking strategy in Inventory module. Defines how product
 * instances are tracked and identified.
 */
public enum ProductTrackingStrategy {

    /** One-of-a-kind item (e.g., Mona Lisa painting). Always exactly one instance exists. */
    UNIQUE,

    /** Each instance has unique serial number (e.g., phones, laptops). */
    INDIVIDUALLY_TRACKED,

    /** Instances are tracked by production batch (e.g., milk, medicines). */
    BATCH_TRACKED,

    /** Both individual serial and batch tracking (e.g., high-value items in batches). */
    INDIVIDUALLY_AND_BATCH_TRACKED,

    /**
     * Fully interchangeable instances (e.g., electricity, water, sand). No serial numbers or
     * batches - pure quantity.
     */
    IDENTICAL;

    public boolean isTrackedIndividually() {
        return this == UNIQUE
                || this == INDIVIDUALLY_TRACKED
                || this == INDIVIDUALLY_AND_BATCH_TRACKED;
    }

    public boolean isTrackedByBatch() {
        return this == BATCH_TRACKED || this == INDIVIDUALLY_AND_BATCH_TRACKED;
    }

    public boolean requiresBothTrackingMethods() {
        return this == INDIVIDUALLY_AND_BATCH_TRACKED;
    }

    public boolean isInterchangeable() {
        return this == IDENTICAL;
    }
}
