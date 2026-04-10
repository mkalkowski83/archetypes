package com.softwarearchetypes.inventory;

import com.softwarearchetypes.inventory.availability.OwnerId;
import com.softwarearchetypes.quantity.Quantity;
import java.util.Objects;

/**
 * LockCommand is sent from ReservationFacade to InventoryFacade. Uses ProductIdentifier -
 * InventoryEntry translates to ResourceId.
 */
public record LockCommand(
        ProductIdentifier productId,
        Quantity quantity,
        OwnerId owner,
        ResourceSpecification resourceSpecification) {
    public LockCommand {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(quantity, "quantity cannot be null");
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(resourceSpecification, "resourceSpecification cannot be null");
    }
}
