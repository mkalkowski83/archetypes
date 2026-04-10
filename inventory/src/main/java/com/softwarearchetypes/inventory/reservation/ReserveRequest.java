package com.softwarearchetypes.inventory.reservation;

import com.softwarearchetypes.inventory.ProductIdentifier;
import com.softwarearchetypes.inventory.ResourceSpecification;
import com.softwarearchetypes.inventory.availability.OwnerId;
import com.softwarearchetypes.quantity.Quantity;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * ReserveRequest is the command from business processes to the reservation layer. It uses product
 * language (ProductIdentifier), not resource language (ResourceId).
 */
public record ReserveRequest(
        ProductIdentifier productId,
        Quantity quantity,
        OwnerId owner,
        ReservationPurpose purpose,
        ResourceSpecification resourceSpecification,
        Duration validFor) {
    public ReserveRequest {
        Objects.requireNonNull(productId, "productId cannot be null");
        Objects.requireNonNull(quantity, "quantity cannot be null");
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(purpose, "purpose cannot be null");
        Objects.requireNonNull(resourceSpecification, "resourceSpecification cannot be null");
        // validFor can be null (indefinite reservation)
    }

    public Optional<Duration> validForDuration() {
        return Optional.ofNullable(validFor);
    }

    public static Builder forProduct(ProductIdentifier productId) {
        return new Builder(productId);
    }

    public static class Builder {
        private final ProductIdentifier productId;
        private Quantity quantity;
        private OwnerId owner;
        private ReservationPurpose purpose = ReservationPurpose.BOOKING;
        private ResourceSpecification resourceSpecification;
        private Duration validFor;

        Builder(ProductIdentifier productId) {
            this.productId = productId;
        }

        public Builder quantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder owner(OwnerId owner) {
            this.owner = owner;
            return this;
        }

        public Builder purpose(ReservationPurpose purpose) {
            this.purpose = purpose;
            return this;
        }

        public Builder resourceSpecification(ResourceSpecification spec) {
            this.resourceSpecification = spec;
            return this;
        }

        public Builder validFor(Duration duration) {
            this.validFor = duration;
            return this;
        }

        public ReserveRequest build() {
            return new ReserveRequest(
                    productId, quantity, owner, purpose, resourceSpecification, validFor);
        }
    }
}
