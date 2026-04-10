package com.softwarearchetypes.inventory;

import com.softwarearchetypes.quantity.Quantity;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for creating ProductInstance with fluent API.
 *
 * <p>Usage:
 *
 * <pre>
 * ProductInstance instance = new InstanceBuilder(InstanceId.random(), productId)
 *     .withSerial(SerialNumber.of("ABC123"))
 *     .withQuantity(Quantity.of(1, Unit.pieces()))
 *     .withFeature("color", "silver")
 *     .withFeature("size", "L")
 *     .build();
 *
 * ProductInstance batchInstance = new InstanceBuilder(InstanceId.random(), productId)
 *     .withBatch(BatchId.random())
 *     .build();
 * </pre>
 */
class InstanceBuilder {

    private final InstanceId id;
    private final ProductIdentifier productId;

    private SerialNumber serialNumber;
    private BatchId batchId;
    private Quantity quantity;
    private final Map<String, String> features = new HashMap<>();

    InstanceBuilder(InstanceId id, ProductIdentifier productId) {
        this.id = Objects.requireNonNull(id, "InstanceId cannot be null");
        this.productId = Objects.requireNonNull(productId, "ProductIdentifier cannot be null");
    }

    public InstanceBuilder withSerial(SerialNumber serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public InstanceBuilder withBatch(BatchId batchId) {
        this.batchId = batchId;
        return this;
    }

    public InstanceBuilder withQuantity(Quantity quantity) {
        this.quantity = quantity;
        return this;
    }

    public InstanceBuilder withFeature(String name, String value) {
        this.features.put(name, value);
        return this;
    }

    public InstanceBuilder withFeatures(Map<String, String> features) {
        if (features != null) {
            this.features.putAll(features);
        }
        return this;
    }

    public ProductInstance build() {
        return new ProductInstance(
                id, productId, serialNumber, batchId, quantity, Map.copyOf(features));
    }
}
