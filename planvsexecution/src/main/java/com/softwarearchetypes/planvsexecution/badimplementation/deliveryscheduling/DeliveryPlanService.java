package com.softwarearchetypes.planvsexecution.badimplementation.deliveryscheduling;

import java.time.LocalDate;
import java.util.List;

/**
 * PROBLEM 1: "Plan" bez źródła prawdy
 *
 * <p>This service reconstructs delivery plan from multiple sources: - Customer SLA - Warehouse
 * capacity - Working calendar - Driver availability
 *
 * <p>The plan is NOT a first-class entity - it's a CONCLUSION from a query.
 *
 * <p>Problems: - Plan has no history (would require versioning ALL source entities) - Plan has no
 * moment of change - Plan cannot be modified independently - Plan is a fragile artifact of SQL JOIN
 * - Changing ANY source entity changes "the plan" - No single source of truth for "what was the
 * plan on date X?"
 */
public class DeliveryPlanService {

    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final WorkingCalendar workingCalendar;
    private final DriverAvailabilityRepository driverRepository;

    public DeliveryPlanService(
            CustomerRepository customerRepository,
            WarehouseRepository warehouseRepository,
            WorkingCalendar workingCalendar,
            DriverAvailabilityRepository driverRepository) {
        this.customerRepository = customerRepository;
        this.warehouseRepository = warehouseRepository;
        this.workingCalendar = workingCalendar;
        this.driverRepository = driverRepository;
    }

    /**
     * This method "calculates" the plan by joining multiple sources.
     *
     * <p>The plan is IMPLICIT - it exists only as a result of this query. It has no identity, no
     * lifecycle, no versioning.
     *
     * <p>What happens when: - Customer SLA changes? → "plan" changes, but we don't know what it WAS
     * - Warehouse capacity changes? → "plan" changes, but we have no history - Calendar is updated?
     * → "plan" changes, but we can't compare old vs new - Driver availability changes? → "plan"
     * changes, but we lose the original intention
     */
    public LocalDate calculateDeliveryPlan(Long orderId, Long customerId, LocalDate orderDate) {
        // Step 1: Get customer SLA (from one table)
        Customer customer = customerRepository.findById(customerId);
        int slaDeliveryDays = customer.getSlaDeliveryDays();

        // Step 2: Get warehouse capacity (from another table)
        Warehouse warehouse = warehouseRepository.findByRegion(customer.getRegion());

        // Step 3: Check working calendar (from another table)
        LocalDate tentativeDate = workingCalendar.addWorkingDays(orderDate, slaDeliveryDays);

        // Step 4: Check driver availability (from yet another table)
        List<DriverAvailability> drivers = driverRepository.findAvailableDriversOn(tentativeDate);

        // Step 5: If no capacity or drivers, shift the date
        while (!warehouse.hasCapacityFor(1) || drivers.isEmpty()) {
            tentativeDate = tentativeDate.plusDays(1);
            if (!workingCalendar.isWorkingDay(tentativeDate)) {
                continue;
            }
            drivers = driverRepository.findAvailableDriversOn(tentativeDate);
        }

        // THE RESULT IS THE "PLAN" - but it's not stored anywhere!
        // It's reconstructed every time.
        // It has no version, no history, no identity.

        return tentativeDate;
    }

    /**
     * When someone asks: "What was the delivery plan for order X on date Y?" We CANNOT answer!
     * Because: - We don't know what the customer SLA was on date Y - We don't know what the
     * warehouse capacity was on date Y - We don't know what the calendar looked like on date Y - We
     * don't know what driver availability was on date Y
     *
     * <p>The plan is LOST. It only exists "now", never "then".
     */
    public LocalDate recalculateHistoricalPlan(
            Long orderId, Long customerId, LocalDate orderDate, LocalDate asOf) {
        // IMPOSSIBLE! We have no historical data for:
        // - customer.slaDeliveryDays as of 'asOf' date
        // - warehouse.dailyCapacity as of 'asOf' date
        // - workingCalendar holidays as of 'asOf' date
        // - driverAvailability as of 'asOf' date

        throw new UnsupportedOperationException(
                "Cannot reconstruct historical plan - no versioning of source entities!");
    }
}
