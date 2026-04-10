package com.softwarearchetypes.inventory;

import com.softwarearchetypes.inventory.availability.TimeSlot;
import java.util.List;
import java.util.Objects;

/**
 * ResourceSpecification defines what kind of resources are being requested. Used by InventoryEntry
 * to route to appropriate lock handling.
 */
public sealed interface ResourceSpecification
        permits ResourceSpecification.TemporalSpecification,
                ResourceSpecification.IndividualSpecification,
                ResourceSpecification.QuantitySpecification {

    /** Temporal specification for time-based resources (hotel rooms, appointments, etc.) */
    record TemporalSpecification(List<TimeSlot> timeSlots) implements ResourceSpecification {
        public TemporalSpecification {
            Objects.requireNonNull(timeSlots, "timeSlots cannot be null");
            if (timeSlots.isEmpty()) {
                throw new IllegalArgumentException("timeSlots cannot be empty");
            }
        }

        public static TemporalSpecification of(TimeSlot... slots) {
            return new TemporalSpecification(List.of(slots));
        }

        public static TemporalSpecification of(List<TimeSlot> slots) {
            return new TemporalSpecification(slots);
        }
    }

    /** Individual specification for specific instances (specific laptop, specific car, etc.) */
    record IndividualSpecification(InstanceId instanceId) implements ResourceSpecification {
        public IndividualSpecification {
            Objects.requireNonNull(instanceId, "instanceId cannot be null");
        }

        public static IndividualSpecification of(InstanceId instanceId) {
            return new IndividualSpecification(instanceId);
        }
    }

    /**
     * Quantity specification for pool resources (milk, fuel, etc.) The actual resource selection is
     * delegated to InventoryEntry.
     */
    record QuantitySpecification() implements ResourceSpecification {
        public static QuantitySpecification instance() {
            return new QuantitySpecification();
        }
    }
}
