package com.softwarearchetypes.inventory;

import java.util.Map;

/** Read-only view of a ProductInstance. */
public record InstanceView(
        InstanceId id,
        ProductIdentifier productId,
        String serialNumber,
        String batchId,
        String quantity,
        Map<String, String> features) {

    static InstanceView from(ProductInstance instance) {
        return new InstanceView(
                instance.id(),
                instance.productId(),
                instance.serialNumber().map(SerialNumber::value).orElse(null),
                instance.batchId().map(BatchId::toString).orElse(null),
                instance.quantity().map(Object::toString).orElse(null),
                instance.features());
    }
}
