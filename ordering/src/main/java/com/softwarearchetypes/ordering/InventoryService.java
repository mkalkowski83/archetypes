package com.softwarearchetypes.ordering;

import static com.softwarearchetypes.common.Preconditions.checkArgument;

import com.softwarearchetypes.quantity.Quantity;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for resource allocation and reservations. Operates in generic "resource
 * language" - doesn't know domain specifics like "doctor" or "courier".
 */
interface InventoryService {

    /** Check availability without making a reservation. */
    AvailabilityResult checkAvailability(AvailabilityQuery query);

    /** Reserve a resource (typically for PRE_ALLOCATED strategy). */
    ReservationResponse reserve(ReservationRequest request);

    /** Allocate resources for an order (typically for ORDER_DRIVEN strategy). */
    AllocationResult allocate(AllocationRequest request);

    /** Validate that a reservation is still valid. */
    void validateReservation(ReservationId reservationId, OrderId orderId);

    /** Commit a reservation to an order (finalize it). */
    void commitReservation(ReservationId reservationId, OrderId orderId);

    /** Fulfill an order from waitlist when resource becomes available. */
    void fulfillFromWaitlist(WaitlistId waitlistId);
}

/**
 * Defines WHAT TO DO when resource is not available. This is typically defined at the Product
 * level.
 */
enum AllocationPolicy {
    /** Reject order if resource unavailable (hard constraint, e.g., flight seat). */
    REJECT,

    /** Reserve with timeout, customer must confirm within time limit (e.g., concert ticket). */
    RESERVE_TIMEOUT,

    /** Put on waitlist, fulfill when resource becomes available (e.g., specialist appointment). */
    WAITLIST,

    /** Retry periodically to find resource (e.g., cloud VM). */
    POLL_RETRY,

    /** Dynamically increase capacity (e.g., cloud storage, bank account numbers). */
    ELASTIC
}

/**
 * Request for resource allocation. Carries full allocation context in generic resource language.
 */
record AllocationRequest(
        ProductIdentifier productId,
        Quantity quantity,
        OrderId orderId,
        AllocationPolicy policy,
        Map<String, String> context) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProductIdentifier productId;
        private Quantity quantity;
        private OrderId orderId;
        private AllocationPolicy policy;
        private Map<String, String> context;

        public Builder productId(ProductIdentifier productId) {
            this.productId = productId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder orderId(OrderId orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder policy(AllocationPolicy policy) {
            this.policy = policy;
            return this;
        }

        public Builder context(Map<String, String> context) {
            this.context = context;
            return this;
        }

        public AllocationRequest build() {
            return new AllocationRequest(productId, quantity, orderId, policy, context);
        }
    }
}

/** Result of allocation attempt. Generic response in resource language. */
record AllocationResult(
        AllocationStatus status, ReservationId reservationId, Map<String, String> attributes) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AllocationStatus status;
        private ReservationId reservationId;
        private Map<String, String> attributes;

        public Builder status(AllocationStatus status) {
            this.status = status;
            return this;
        }

        public Builder reservationId(ReservationId reservationId) {
            this.reservationId = reservationId;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public AllocationResult build() {
            return new AllocationResult(status, reservationId, attributes);
        }
    }
}

/** Status of allocation attempt. Reported by Inventory, interpreted by Ordering. */
enum AllocationStatus {
    /** Resource successfully allocated. */
    ALLOCATED,

    /** Resource not available, order put on waitlist. */
    WAITLISTED,

    /** Partial allocation (some quantity allocated, some not). */
    PARTIAL,

    /** Resource completely unavailable. */
    UNAVAILABLE
}

/** Defines WHEN resource allocation happens. This is typically defined at the Product level. */
enum AllocationStrategy {
    /** No allocation needed (e.g., e-book, digital product). */
    NONE,

    /** Resource must be reserved BEFORE order is created (e.g., courier slot, concert ticket). */
    PRE_ALLOCATED,

    /** Resource is allocated DURING order confirmation (e.g., doctor appointment, bank account). */
    ORDER_DRIVEN,

    /** Allocation is managed externally, outside the system (e.g., partner services). */
    EXTERNAL
}

/** Query to check resource availability without making a reservation. */
record AvailabilityQuery(
        ProductIdentifier productId, Quantity quantity, Map<String, String> context) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProductIdentifier productId;
        private Quantity quantity;
        private Map<String, String> context;

        public Builder productId(ProductIdentifier productId) {
            this.productId = productId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder context(Map<String, String> context) {
            this.context = context;
            return this;
        }

        public AvailabilityQuery build() {
            return new AvailabilityQuery(productId, quantity, context);
        }
    }
}

/** Result of availability check. */
record AvailabilityResult(
        boolean available, Quantity availableQuantity, Map<String, String> attributes) {
    public static AvailabilityResult available(Quantity quantity) {
        return new AvailabilityResult(true, quantity, Map.of());
    }

    public static AvailabilityResult unavailable() {
        return new AvailabilityResult(false, null, Map.of());
    }

    public static AvailabilityResult partial(Quantity availableQuantity) {
        return new AvailabilityResult(false, availableQuantity, Map.of("status", "partial"));
    }
}

record BlockadeId(String value) {

    public static BlockadeId of(String value) {
        return new BlockadeId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

record ReservationId(String value) {

    public static ReservationId of(String value) {
        return new ReservationId(value);
    }

    public static ReservationId generate() {
        return new ReservationId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}

record Reservation(
        ReservationId id,
        OrderId orderId,
        OrderLineId orderLineId,
        BlockadeId blockadeId,
        ResourceId resourceId) {
    public Reservation {
        checkArgument(id != null, "ReservationId must be defined");
        checkArgument(orderId != null, "OrderId must be defined");
        checkArgument(orderLineId != null, "OrderLineId must be defined");
        checkArgument(blockadeId != null, "BlockadeId must be defined");
        checkArgument(resourceId != null, "ResourceId must be defined");
    }
}

record WaitlistId(String value) {
    public static WaitlistId of(String value) {
        return new WaitlistId(value);
    }

    public static WaitlistId generate() {
        return new WaitlistId(UUID.randomUUID().toString());
    }
}

/** Request for resource reservation (PRE_ALLOCATED strategy). */
record ReservationRequest(
        ProductIdentifier productId,
        Quantity quantity,
        Duration timeout,
        Map<String, String> context) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ProductIdentifier productId;
        private Quantity quantity;
        private Duration timeout;
        private Map<String, String> context;

        public Builder productId(ProductIdentifier productId) {
            this.productId = productId;
            return this;
        }

        public Builder quantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder context(Map<String, String> context) {
            this.context = context;
            return this;
        }

        public ReservationRequest build() {
            return new ReservationRequest(productId, quantity, timeout, context);
        }
    }
}

/** Represents a resource reservation response from inventory service. */
record ReservationResponse(
        ReservationId id,
        ProductIdentifier productId,
        Quantity quantity,
        LocalDateTime expiresAt,
        Map<String, String> attributes) {
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}

record ResourceId(String value) {

    public static ResourceId of(String value) {
        return new ResourceId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

class FixableInventoryService implements InventoryService {

    private AllocationResult allocationResult = defaultAllocated();
    private final List<AllocationRequest> allocateRequests = new ArrayList<>();

    public void willReturnOnAllocate(AllocationResult result) {
        this.allocationResult = result;
    }

    public void willFailOnAllocate() {
        this.allocationResult =
                AllocationResult.builder()
                        .status(AllocationStatus.UNAVAILABLE)
                        .attributes(Map.of())
                        .build();
    }

    public void willReturnOnAllocate(AllocationStatus status) {
        this.allocationResult =
                AllocationResult.builder().status(status).attributes(Map.of()).build();
    }

    public void reset() {
        this.allocationResult = defaultAllocated();
        this.allocateRequests.clear();
    }

    public List<AllocationRequest> allocateRequests() {
        return List.copyOf(allocateRequests);
    }

    @Override
    public AvailabilityResult checkAvailability(AvailabilityQuery query) {
        return AvailabilityResult.available(query.quantity());
    }

    @Override
    public ReservationResponse reserve(ReservationRequest request) {
        return new ReservationResponse(
                ReservationId.generate(),
                request.productId(),
                request.quantity(),
                LocalDateTime.now().plus(Duration.ofHours(1)),
                Map.of());
    }

    @Override
    public AllocationResult allocate(AllocationRequest request) {
        allocateRequests.add(request);
        return allocationResult;
    }

    @Override
    public void validateReservation(ReservationId reservationId, OrderId orderId) {}

    @Override
    public void commitReservation(ReservationId reservationId, OrderId orderId) {}

    @Override
    public void fulfillFromWaitlist(WaitlistId waitlistId) {}

    private static AllocationResult defaultAllocated() {
        return AllocationResult.builder()
                .status(AllocationStatus.ALLOCATED)
                .reservationId(ReservationId.generate())
                .attributes(Map.of())
                .build();
    }
}
