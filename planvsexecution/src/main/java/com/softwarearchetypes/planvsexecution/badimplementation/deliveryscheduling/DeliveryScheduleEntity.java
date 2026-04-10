package com.softwarearchetypes.planvsexecution.badimplementation.deliveryscheduling;

import java.time.LocalDate;

/**
 * PROBLEM 2: Plan and execution in ONE entity + mutability
 *
 * <p>This entity tries to be EVERYTHING: - It holds the PLAN (plannedDate, plannedQuantity) - It
 * holds the EXECUTION (actualDate, actualQuantity) - It calculates DELTA (calculateDelta method) -
 * It mutates when execution happens (updateActualDelivery)
 *
 * <p>Problems: - Cannot compare different plans with same execution - Cannot compare same plan with
 * different executions - Simulations mutate production data - No combinatorics of comparisons -
 * Plan and execution are INTERTWINED - Changing one affects the other - Cannot answer: "how would
 * this execution look against previous plan?" - Cannot answer: "how would this execution look
 * against alternative plan?" - Cannot simulate without copying/flags/"simulation mode"
 */
public class DeliveryScheduleEntity {

    private Long id;
    private Long orderId;

    // PLAN fields
    private LocalDate plannedDate;
    private int plannedQuantity;

    // EXECUTION fields (in the same entity!)
    private LocalDate actualDate;
    private int actualQuantity;

    // Status flag (mixing concerns)
    private DeliveryStatus status;

    // Audit fields (trying to version, but without intention)
    private LocalDate lastModified;
    private String lastModifiedBy;

    public DeliveryScheduleEntity(
            Long id, Long orderId, LocalDate plannedDate, int plannedQuantity) {
        this.id = id;
        this.orderId = orderId;
        this.plannedDate = plannedDate;
        this.plannedQuantity = plannedQuantity;
        this.status = DeliveryStatus.PLANNED;
        this.lastModified = LocalDate.now();
    }

    /**
     * PROBLEM: This method MUTATES the entity. After calling this, the "plan" is still here, but
     * it's contaminated with execution.
     *
     * <p>You CANNOT now: - Compare this execution with a different plan - Simulate "what if"
     * scenarios - Answer: "what was planned before this execution?"
     */
    public void updateActualDelivery(LocalDate actualDate, int actualQuantity) {
        this.actualDate = actualDate;
        this.actualQuantity = actualQuantity;
        this.status = DeliveryStatus.DELIVERED;
        this.lastModified = LocalDate.now();
        // Intention is LOST - we don't know WHY this changed
    }

    /**
     * PROBLEM: Delta is calculated "inside" the entity that holds both plan and execution. This
     * prevents comparing: - Same execution with multiple plans - Multiple executions with same plan
     * - Alternative scenarios
     */
    public DeliveryDelta calculateDelta() {
        if (actualDate == null) {
            return new DeliveryDelta(0, 0, DeltaType.NO_EXECUTION);
        }

        long dateDiff = java.time.temporal.ChronoUnit.DAYS.between(plannedDate, actualDate);
        int quantityDiff = actualQuantity - plannedQuantity;

        DeltaType type;
        if (dateDiff == 0 && quantityDiff == 0) {
            type = DeltaType.PERFECT_MATCH;
        } else if (dateDiff > 0) {
            type = DeltaType.LATE;
        } else if (quantityDiff < 0) {
            type = DeltaType.UNDER_DELIVERED;
        } else {
            type = DeltaType.DEVIATION;
        }

        return new DeliveryDelta(dateDiff, quantityDiff, type);
    }

    /**
     * PROBLEM: Trying to update the plan. But this changes the ENTITY, which already has execution
     * data!
     *
     * <p>After this change: - You cannot compare new plan with old plan - You cannot see what was
     * originally planned - The delta changes, but you don't know if it's because execution improved
     * or because you adjusted the plan
     */
    public void updatePlan(LocalDate newPlannedDate, int newPlannedQuantity, String modifiedBy) {
        this.plannedDate = newPlannedDate;
        this.plannedQuantity = newPlannedQuantity;
        this.lastModified = LocalDate.now();
        this.lastModifiedBy = modifiedBy;

        // INTENTION IS LOST!
        // We don't know:
        // - Was this a business decision?
        // - Was this a correction of an error?
        // - Was this a reaction to execution?
        // - Should this apply "from now" or "from always"?
    }

    /**
     * PROBLEM: Trying to simulate. But simulation uses the SAME entity, so: - Flags are needed
     * ("simulation mode") - Copying is needed - Risk of corrupting production data
     */
    public DeliveryDelta simulateIfDeliveredOn(
            LocalDate hypotheticalDate, int hypotheticalQuantity) {
        // Option 1: Mutate this entity (DANGEROUS - corrupts production data!)
        // this.actualDate = hypotheticalDate;
        // this.actualQuantity = hypotheticalQuantity;
        // return calculateDelta();

        // Option 2: Create a copy (MESSY - memory overhead, equals/hashcode issues)
        DeliveryScheduleEntity copy =
                new DeliveryScheduleEntity(
                        this.id, this.orderId, this.plannedDate, this.plannedQuantity);
        copy.updateActualDelivery(hypotheticalDate, hypotheticalQuantity);
        return copy.calculateDelta();

        // Option 3: Add "simulation mode" flag (UGLY - conditional logic everywhere)
        // Option 4: Disable writes in simulation (FRAGILE - easy to forget)

        // NONE of these are good solutions!
        // The root problem: MUTABILITY + mixing plan and execution in ONE entity
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public int getPlannedQuantity() {
        return plannedQuantity;
    }

    public LocalDate getActualDate() {
        return actualDate;
    }

    public int getActualQuantity() {
        return actualQuantity;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public LocalDate getLastModified() {
        return lastModified;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
}
