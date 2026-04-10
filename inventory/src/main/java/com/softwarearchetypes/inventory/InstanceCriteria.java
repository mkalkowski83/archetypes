package com.softwarearchetypes.inventory;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * InstanceCriteria is a specification pattern for filtering product instances. Used for queries
 * like "find all instances with color=Black" or "instances from batch X". Supports composition via
 * and(), or(), and not() operations.
 */
@FunctionalInterface
public interface InstanceCriteria {

    boolean isSatisfiedBy(Instance instance);

    default InstanceCriteria and(InstanceCriteria other) {
        Objects.requireNonNull(other, "other criteria cannot be null");
        return instance -> this.isSatisfiedBy(instance) && other.isSatisfiedBy(instance);
    }

    default InstanceCriteria or(InstanceCriteria other) {
        Objects.requireNonNull(other, "other criteria cannot be null");
        return instance -> this.isSatisfiedBy(instance) || other.isSatisfiedBy(instance);
    }

    default InstanceCriteria not() {
        return instance -> !this.isSatisfiedBy(instance);
    }

    static InstanceCriteria any() {
        return instance -> true;
    }

    static InstanceCriteria none() {
        return instance -> false;
    }

    static InstanceCriteria byBatch(BatchId batchId) {
        Objects.requireNonNull(batchId, "batchId cannot be null");
        return instance -> instance.batchId().map(b -> b.equals(batchId)).orElse(false);
    }

    static InstanceCriteria bySerial(SerialNumber serialNumber) {
        Objects.requireNonNull(serialNumber, "serialNumber cannot be null");
        return instance -> instance.serialNumber().map(s -> s.equals(serialNumber)).orElse(false);
    }

    static InstanceCriteria custom(Predicate<Instance> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        return predicate::test;
    }
}
