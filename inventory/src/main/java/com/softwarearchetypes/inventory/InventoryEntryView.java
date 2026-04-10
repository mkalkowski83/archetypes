package com.softwarearchetypes.inventory;

import com.softwarearchetypes.inventory.availability.ResourceId;
import java.util.Map;
import java.util.Set;

public record InventoryEntryView(
        InventoryEntryId id,
        ProductIdentifier productId,
        String productName,
        Set<InstanceId> instanceIds,
        Map<InstanceId, ResourceId> instanceToResource) {

    static InventoryEntryView from(InventoryEntry entry) {
        return new InventoryEntryView(
                entry.id(),
                entry.productId(),
                entry.product().name(),
                entry.instances(),
                entry.instanceToResourceMap());
    }
}
