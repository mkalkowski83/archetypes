package com.softwarearchetypes.inventory;

import java.util.List;
import java.util.Optional;

/** Repository for ProductInstance entities. */
interface InstanceRepository {

    void save(ProductInstance instance);

    Optional<ProductInstance> findById(InstanceId id);

    Optional<ProductInstance> findBySerialNumber(SerialNumber serialNumber);

    List<ProductInstance> findByBatchId(BatchId batchId);

    List<ProductInstance> findByProductId(ProductIdentifier productId);

    List<ProductInstance> findAll();

    void delete(InstanceId id);
}
