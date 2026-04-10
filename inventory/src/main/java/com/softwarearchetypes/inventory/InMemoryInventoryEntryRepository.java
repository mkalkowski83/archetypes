package com.softwarearchetypes.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryInventoryEntryRepository implements InventoryEntryRepository {

    private final Map<InventoryEntryId, InventoryEntry> storage = new HashMap<>();

    @Override
    public void save(InventoryEntry entry) {
        storage.put(entry.id(), entry);
    }

    @Override
    public Optional<InventoryEntry> findById(InventoryEntryId id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<InventoryEntry> findByProductId(ProductIdentifier productId) {
        return storage.values().stream().filter(e -> e.productId().equals(productId)).findFirst();
    }

    @Override
    public List<InventoryEntry> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void delete(InventoryEntryId id) {
        storage.remove(id);
    }
}
