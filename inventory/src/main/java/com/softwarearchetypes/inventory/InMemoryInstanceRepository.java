package com.softwarearchetypes.inventory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory implementation of InstanceRepository for testing. */
class InMemoryInstanceRepository implements InstanceRepository {

    private final Map<InstanceId, ProductInstance> instances = new ConcurrentHashMap<>();

    @Override
    public void save(ProductInstance instance) {
        instances.put(instance.id(), instance);
    }

    @Override
    public Optional<ProductInstance> findById(InstanceId id) {
        return Optional.ofNullable(instances.get(id));
    }

    @Override
    public Optional<ProductInstance> findBySerialNumber(SerialNumber serialNumber) {
        return instances.values().stream()
                .filter(i -> i.serialNumber().isPresent())
                .filter(i -> i.serialNumber().get().value().equals(serialNumber.value()))
                .findFirst();
    }

    @Override
    public List<ProductInstance> findByBatchId(BatchId batchId) {
        return instances.values().stream()
                .filter(i -> i.batchId().isPresent())
                .filter(i -> i.batchId().get().equals(batchId))
                .toList();
    }

    @Override
    public List<ProductInstance> findByProductId(ProductIdentifier productId) {
        return instances.values().stream().filter(i -> i.productId().equals(productId)).toList();
    }

    @Override
    public List<ProductInstance> findAll() {
        return List.copyOf(instances.values());
    }

    @Override
    public void delete(InstanceId id) {
        instances.remove(id);
    }
}
