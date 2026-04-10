package com.softwarearchetypes.planvsexecution.productionanalysis.modification;

/**
 * A rule that combines a condition with a modification action. When condition is fulfilled, the
 * modifier is applied.
 */
public record ModificationRule(
        ScheduleModificationCondition condition, ScheduleModifier modifier, boolean applyOnce) {

    public static ModificationRule once(
            ScheduleModificationCondition condition, ScheduleModifier modifier) {
        return new ModificationRule(condition, modifier, true);
    }

    public static ModificationRule always(
            ScheduleModificationCondition condition, ScheduleModifier modifier) {
        return new ModificationRule(condition, modifier, false);
    }
}
