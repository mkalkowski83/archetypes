package com.softwarearchetypes.inventory;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import java.util.Optional;

/**
 * Instance represents a specific instance/exemplar of a Product. While Product defines WHAT can be
 * sold, Instance represents WHAT WAS actually sold/created.
 *
 * <p>Common interface for: - ProductInstance: specific instance of a ProductType (e.g., iPhone with
 * serial ABC123) - PackageInstance: specific instance of a PackageType (e.g., laptop bundle with
 * customer's choices)
 *
 * <p>Every instance must be tracked by at least one of: - SerialNumber (individual tracking) -
 * Batch (group tracking)
 */
interface Instance {

    InstanceId id();

    ProductIdentifier productId();

    Optional<SerialNumber> serialNumber();

    Optional<BatchId> batchId();

    Optional<Quantity> quantity();

    /**
     * Returns the effective quantity - either the explicit quantity or 1 piece as default. Used for
     * counting/summing instances.
     */
    default Quantity effectiveQuantity() {
        return quantity().orElse(Quantity.of(1, Unit.pieces()));
    }
}
