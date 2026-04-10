package com.softwarearchetypes.planvsexecution.repaymentanalysis.modification;

public record ModificationRule(
        ScheduleModificationCondition condition,
        PaymentScheduleModifier modifier,
        boolean applyOnce) {

    public ModificationRule {
        if (condition == null) {
            throw new IllegalArgumentException("condition cannot be null");
        }
        if (modifier == null) {
            throw new IllegalArgumentException("modifier cannot be null");
        }
    }

    public static ModificationRule of(
            ScheduleModificationCondition condition, PaymentScheduleModifier modifier) {
        return new ModificationRule(condition, modifier, false);
    }

    public static ModificationRule once(
            ScheduleModificationCondition condition, PaymentScheduleModifier modifier) {
        return new ModificationRule(condition, modifier, true);
    }
}
