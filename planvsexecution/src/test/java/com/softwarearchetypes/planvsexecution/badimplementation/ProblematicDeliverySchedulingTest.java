package com.softwarearchetypes.planvsexecution.badimplementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.softwarearchetypes.planvsexecution.badimplementation.deliveryscheduling.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * This test demonstrates the problems with bad implementations:
 *
 * <p>Problem 1: Plan reconstructed from multiple sources (no single source of truth) Problem 2:
 * Plan and execution in one mutable entity
 */
class ProblematicDeliverySchedulingTest {

    // ============================================
    // PROBLEM 1: Plan without source of truth
    // ============================================

    @Test
    void problem1_plan_changes_when_customer_sla_changes() {
        // given
        CustomerRepository customerRepo = new CustomerRepository();
        WarehouseRepository warehouseRepo = new WarehouseRepository();
        WorkingCalendar calendar = new WorkingCalendar();
        DriverAvailabilityRepository driverRepo = new DriverAvailabilityRepository();

        Customer customer = new Customer(1L, "ACME Corp", 3, "North");
        customerRepo.save(customer);

        Warehouse warehouse = new Warehouse(1L, "North", 100, 0);
        warehouseRepo.save(warehouse);

        LocalDate orderDate = LocalDate.of(2024, 1, 15);
        driverRepo.save(new DriverAvailability(1L, "John", orderDate.plusDays(3), true, 10));
        driverRepo.save(new DriverAvailability(2L, "Mike", orderDate.plusDays(7), true, 10));

        DeliveryPlanService planService =
                new DeliveryPlanService(customerRepo, warehouseRepo, calendar, driverRepo);

        // when - calculate plan initially
        LocalDate initialPlan = planService.calculateDeliveryPlan(100L, 1L, orderDate);
        assertThat(initialPlan).isEqualTo(LocalDate.of(2024, 1, 18)); // 3 working days later

        // then - someone changes customer SLA (business decision? error correction? we don't know!)
        customer.setSlaDeliveryDays(5); // Changed from 3 to 5

        // when - recalculate plan
        LocalDate newPlan = planService.calculateDeliveryPlan(100L, 1L, orderDate);

        // then - THE PLAN CHANGED, but we have NO HISTORY!
        assertThat(newPlan).isNotEqualTo(initialPlan);

        // PROBLEM: We cannot answer:
        // - "What was the plan yesterday?"
        // - "Why did it change?"
        // - "Was it a business decision or a bug fix?"
        // - "Compare today's execution with yesterday's plan"
    }

    @Test
    void problem1_cannot_reconstruct_historical_plan() {
        // given
        CustomerRepository customerRepo = new CustomerRepository();
        WarehouseRepository warehouseRepo = new WarehouseRepository();
        WorkingCalendar calendar = new WorkingCalendar();
        DriverAvailabilityRepository driverRepo = new DriverAvailabilityRepository();

        Customer customer = new Customer(1L, "ACME Corp", 3, "North");
        customerRepo.save(customer);

        Warehouse warehouse = new Warehouse(1L, "North", 100, 0);
        warehouseRepo.save(warehouse);

        DeliveryPlanService planService =
                new DeliveryPlanService(customerRepo, warehouseRepo, calendar, driverRepo);

        LocalDate orderDate = LocalDate.of(2024, 1, 15);
        LocalDate asOfDate = LocalDate.of(2024, 1, 1);

        // when/then - CANNOT reconstruct plan "as it was on 2024-01-01"
        assertThrows(
                UnsupportedOperationException.class,
                () -> {
                    planService.recalculateHistoricalPlan(100L, 1L, orderDate, asOfDate);
                });

        // PROBLEM: No versioning of source entities = no historical plans!
    }

    @Test
    void problem1_warehouse_capacity_change_invalidates_all_plans() {
        // given
        CustomerRepository customerRepo = new CustomerRepository();
        WarehouseRepository warehouseRepo = new WarehouseRepository();
        WorkingCalendar calendar = new WorkingCalendar();
        DriverAvailabilityRepository driverRepo = new DriverAvailabilityRepository();

        Customer customer = new Customer(1L, "ACME Corp", 2, "North");
        customerRepo.save(customer);

        Warehouse warehouse = new Warehouse(1L, "North", 100, 0);
        warehouseRepo.save(warehouse);

        LocalDate orderDate = LocalDate.of(2024, 1, 15);
        driverRepo.save(new DriverAvailability(1L, "John", orderDate.plusDays(2), true, 10));

        DeliveryPlanService planService =
                new DeliveryPlanService(customerRepo, warehouseRepo, calendar, driverRepo);

        // when - calculate plan with capacity
        LocalDate planWithCapacity = planService.calculateDeliveryPlan(100L, 1L, orderDate);

        // then - someone changes warehouse capacity
        warehouse.setDailyCapacity(50); // Reduced capacity

        // PROBLEM: All plans that depended on this capacity are now invalid
        // But we have NO WAY to know:
        // - Which plans are affected?
        // - What were the original plans?
        // - Should we recalculate historical deliveries?
    }

    // ============================================
    // PROBLEM 2: Plan and execution in one entity + mutability
    // ============================================

    @Test
    void problem2_cannot_compare_execution_with_different_plans() {
        // given - one schedule entity with plan and execution
        DeliveryScheduleEntity schedule =
                new DeliveryScheduleEntity(1L, 100L, LocalDate.of(2024, 1, 20), 100);

        // when - execution happens
        schedule.updateActualDelivery(LocalDate.of(2024, 1, 22), 95);

        // then - calculate delta
        DeliveryDelta delta1 = schedule.calculateDelta();
        assertThat(delta1.dateDifferenceInDays()).isEqualTo(2); // 2 days late

        // BUT NOW: Business wants to know "what if we had planned for 2024-01-25?"
        // We CANNOT compare the same execution with a different plan!
        // The only option is to MUTATE the entity:

        schedule.updatePlan(LocalDate.of(2024, 1, 25), 100, "manager");

        // when - recalculate delta
        DeliveryDelta delta2 = schedule.calculateDelta();

        // then - now it looks "early" instead of "late"!
        assertThat(delta2.dateDifferenceInDays()).isEqualTo(-3); // 3 days early

        // PROBLEM:
        // - We LOST the original plan!
        // - We LOST the original delta!
        // - We cannot compare "same execution vs multiple plans"
        // - We corrupted our historical data!
    }

    @Test
    void problem2_simulation_creates_messy_copies() {
        // given
        DeliveryScheduleEntity schedule =
                new DeliveryScheduleEntity(1L, 100L, LocalDate.of(2024, 1, 20), 100);

        schedule.updateActualDelivery(LocalDate.of(2024, 1, 22), 95);

        // when - simulate "what if we delivered on time?"
        DeliveryDelta simulatedDelta =
                schedule.simulateIfDeliveredOn(LocalDate.of(2024, 1, 20), 100);

        // then - simulation works, but:
        assertThat(simulatedDelta.type()).isEqualTo(DeltaType.PERFECT_MATCH);

        // PROBLEMS with this approach:
        // 1. Had to create a COPY inside simulateIfDeliveredOn()
        // 2. Memory overhead for every simulation
        // 3. equals()/hashCode() issues with copies
        // 4. Risk of accidentally mutating production data
        // 5. Need flags like "isSimulation" everywhere
        // 6. Cannot simulate multiple scenarios in parallel
    }

    @Test
    void problem2_updating_plan_loses_intention() {
        // given
        DeliveryScheduleEntity schedule =
                new DeliveryScheduleEntity(1L, 100L, LocalDate.of(2024, 1, 20), 100);

        // when - business changes the plan
        schedule.updatePlan(LocalDate.of(2024, 1, 25), 120, "jane");

        // then - we know WHO changed it and WHEN
        assertThat(schedule.getLastModifiedBy()).isEqualTo("jane");
        assertThat(schedule.getLastModified()).isEqualTo(LocalDate.now());

        // BUT we DON'T know:
        // - WHY was it changed? (business decision? error correction? reaction to execution?)
        // - WHAT was the original plan?
        // - Should this change apply "from now" or "from always"?
        // - Was this a "shift deadline" or "increase quantity" or something else?

        // INTENTION IS LOST!
        // We have state change audit, but no intention audit.
    }

    @Test
    void problem2_cannot_answer_combinatoric_questions() {
        // Scenario: We have ONE execution, but want to compare it with THREE different plans

        DeliveryScheduleEntity schedule =
                new DeliveryScheduleEntity(1L, 100L, LocalDate.of(2024, 1, 20), 100);

        schedule.updateActualDelivery(LocalDate.of(2024, 1, 22), 95);

        // Business questions:
        // Q1: "How does this execution compare to the ORIGINAL plan?"
        // Q2: "How does this execution compare to the ADJUSTED plan (after correction)?"
        // Q3: "How does this execution compare to the ALTERNATIVE plan (what-if scenario)?"

        // PROBLEM: We CANNOT answer all three questions!
        // Because plan and execution are in ONE entity.
        // We can only calculate ONE delta at a time.
        // To answer all three, we'd need to:
        // - Either mutate the plan (losing previous comparison)
        // - Or create three copies (messy, error-prone)
        // - Or add "simulation mode" flags (ugly, fragile)

        // The root issue: NO COMBINATORICS.
        // We need: N plans × M executions = N×M deltas
        // But we can only do: 1 plan × 1 execution = 1 delta
    }

    @Test
    void problem2_mutability_kills_what_if_questions() {
        // given - original schedule
        DeliveryScheduleEntity schedule =
                new DeliveryScheduleEntity(1L, 100L, LocalDate.of(2024, 1, 20), 100);

        // Business wants to answer: "What if we had delivered on these three different dates?"
        LocalDate scenario1 = LocalDate.of(2024, 1, 18); // 2 days early
        LocalDate scenario2 = LocalDate.of(2024, 1, 20); // on time
        LocalDate scenario3 = LocalDate.of(2024, 1, 25); // 5 days late

        // PROBLEM: Each simulation MUTATES or COPIES the entity
        DeliveryDelta delta1 = schedule.simulateIfDeliveredOn(scenario1, 100);
        DeliveryDelta delta2 = schedule.simulateIfDeliveredOn(scenario2, 100);
        DeliveryDelta delta3 = schedule.simulateIfDeliveredOn(scenario3, 100);

        // We got the results, but:
        // - Created 3 copies internally (memory waste)
        // - Cannot run simulations in parallel safely
        // - Risk of corrupting production data if simulation forgets to use copy
        // - Code is full of defensive copying and "simulation mode" checks

        // MUTABILITY KILLS EXPERIMENTATION!
    }
}
