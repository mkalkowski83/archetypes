package com.softwarearchetypes.planvsexecution.productionanalysis.modification;

/** Condition triggered when under-production exceeds a threshold. */
public record UnderProductionCondition(int minQuantity) implements ScheduleModificationCondition {

    public static UnderProductionCondition atLeast(int minQuantity) {
        return new UnderProductionCondition(minQuantity);
    }
}
