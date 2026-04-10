package com.softwarearchetypes.inventory;

import java.util.List;
import java.util.Optional;

interface InventoryEntryRepository {

    void save(InventoryEntry entry);

    Optional<InventoryEntry> findById(InventoryEntryId id);

    Optional<InventoryEntry> findByProductId(ProductIdentifier productId);

    List<InventoryEntry> findAll();

    void delete(InventoryEntryId id);
}
