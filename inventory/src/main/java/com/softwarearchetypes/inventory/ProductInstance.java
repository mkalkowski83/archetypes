package com.softwarearchetypes.inventory;

import com.softwarearchetypes.quantity.Quantity;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * ProductInstance represents a specific instance/exemplar of a ProductType in inventory.
 *
 * <p>Examples: - ProductType: "iPhone 15 Pro 256GB" -> ProductInstance: specific phone with serial
 * ABC123 - ProductType: "Organic Milk 1L" -> ProductInstance: specific bottle from batch
 * LOT-2024-001 - ProductType: "Consulting" -> ProductInstance: 8.5 hours of consulting delivered
 *
 * <p>Each instance must be tracked by at least one of: - SerialNumber (individual tracking) - Batch
 * (group tracking) - Or both
 *
 * <p>Optional quantity tracks the amount for this specific instance (e.g., 8.5 hours, 3.2 kg).
 *
 * <p>Features map contains actual values for features defined in the ProductType (e.g., color=red,
 * size=L, yearOfProduction=2023).
 */
class ProductInstance implements Instance {

    private final InstanceId id;
    private final ProductIdentifier productId;
    private final SerialNumber serialNumber;
    private final BatchId batchId;
    private final Quantity quantity;
    private final Map<String, String> features;

    ProductInstance(
            InstanceId id,
            ProductIdentifier productId,
            SerialNumber serialNumber,
            BatchId batchId,
            Quantity quantity,
            Map<String, String> features) {
        this.id = Objects.requireNonNull(id, "InstanceId cannot be null");
        this.productId = Objects.requireNonNull(productId, "ProductIdentifier cannot be null");
        this.serialNumber = serialNumber;
        this.batchId = batchId;
        this.quantity = quantity;
        this.features = features != null ? Map.copyOf(features) : Map.of();
    }

    @Override
    public InstanceId id() {
        return id;
    }

    @Override
    public ProductIdentifier productId() {
        return productId;
    }

    @Override
    public Optional<SerialNumber> serialNumber() {
        return Optional.ofNullable(serialNumber);
    }

    @Override
    public Optional<BatchId> batchId() {
        return Optional.ofNullable(batchId);
    }

    public Optional<Quantity> quantity() {
        return Optional.ofNullable(quantity);
    }

    public Map<String, String> features() {
        return features;
    }

    public Optional<String> feature(String name) {
        return Optional.ofNullable(features.get(name));
    }

    @Override
    public String toString() {
        return "ProductInstance{id=%s, productId=%s, serial=%s, batch=%s, quantity=%s}"
                .formatted(
                        id,
                        productId,
                        serialNumber != null ? serialNumber : "none",
                        batchId != null ? batchId : "none",
                        quantity != null ? quantity : "implicit");
    }
}
