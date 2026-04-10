package com.softwarearchetypes.inventory;

import com.softwarearchetypes.quantity.Quantity;
import java.util.Map;
import java.util.Objects;

/** Command for creating a new ProductInstance. */
public record CreateInstance(
        ProductIdentifier productId,
        SerialNumber serialNumber,
        BatchId batchId,
        Quantity quantity,
        Map<String, String> features) {

    public CreateInstance {
        Objects.requireNonNull(productId, "ProductIdentifier cannot be null");
        features = features != null ? Map.copyOf(features) : Map.of();
    }

    public static Builder forProduct(ProductIdentifier productId) {
        return new Builder(productId);
    }

    public static class Builder {
        private final ProductIdentifier productId;
        private SerialNumber serialNumber;
        private BatchId batchId;
        private Quantity quantity;
        private Map<String, String> features = Map.of();

        private Builder(ProductIdentifier productId) {
            this.productId = productId;
        }

        public Builder withSerial(SerialNumber serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public Builder withSerial(String serialNumber) {
            this.serialNumber = SerialNumber.of(serialNumber);
            return this;
        }

        public Builder withBatch(BatchId batchId) {
            this.batchId = batchId;
            return this;
        }

        public Builder withQuantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder withFeatures(Map<String, String> features) {
            this.features = features;
            return this;
        }

        public CreateInstance build() {
            return new CreateInstance(productId, serialNumber, batchId, quantity, features);
        }
    }
}
