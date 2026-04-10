package com.softwarearchetypes.ordering;

import static com.softwarearchetypes.common.Preconditions.checkArgument;
import static com.softwarearchetypes.common.Preconditions.checkState;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.money.Money;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

class Order {

    private final OrderId id;
    private OrderStatus status;
    private final List<OrderLine> lines;
    private final OrderParties parties;
    private final List<Reservation> reservations;
    private final OrderServices services;

    private Order(
            OrderId id,
            OrderStatus status,
            List<OrderLine> lines,
            OrderParties parties,
            OrderServices services) {
        checkArgument(id != null, "OrderId must be defined");
        checkArgument(status != null, "OrderStatus must be defined");
        checkArgument(lines != null, "Lines must be defined");
        checkArgument(parties != null, "OrderParties must be defined");
        checkArgument(services != null, "OrderServices must be defined");

        this.id = id;
        this.status = status;
        this.lines = new ArrayList<>(lines);
        this.parties = parties;
        this.reservations = new ArrayList<>();
        this.services = services;
    }

    public OrderId id() {
        return id;
    }

    public OrderStatus status() {
        return status;
    }

    public List<OrderLine> lines() {
        return List.copyOf(lines);
    }

    public OrderParties parties() {
        return parties;
    }

    public List<Reservation> reservations() {
        return List.copyOf(reservations);
    }

    /**
     * Returns effective parties for a given order line. If the line has line-level parties, they
     * override order-level parties for those roles. Otherwise, order-level parties are used.
     */
    OrderParties getEffectivePartiesFor(OrderLine line) {
        if (line.parties().isEmpty()) {
            return this.parties;
        }
        return OrderParties.merge(this.parties, line.parties());
    }

    void addLine(OrderLine line) {
        checkState(status.canAddLines(), "Cannot add lines in status: " + status);
        checkArgument(line != null, "OrderLine must be defined");
        lines.add(line);
    }

    void removeLine(OrderLineId lineId) {
        checkState(status.canModifyLines(), "Cannot remove lines in status: " + status);
        checkArgument(lineId != null, "OrderLineId must be defined");
        boolean removed = lines.removeIf(l -> l.id().equals(lineId));
        checkState(removed, "Order line not found: " + lineId);
        checkState(!lines.isEmpty(), "Order must have at least one line");
    }

    void changeLineQuantity(OrderLineId lineId, Quantity newQuantity) {
        checkState(status.canModifyLines(), "Cannot modify lines in status: " + status);
        checkArgument(lineId != null, "OrderLineId must be defined");
        checkArgument(newQuantity != null, "Quantity must be defined");
        OrderLine existing =
                lines.stream()
                        .filter(l -> l.id().equals(lineId))
                        .findFirst()
                        .orElseThrow(
                                () -> new IllegalStateException("Order line not found: " + lineId));
        int idx = lines.indexOf(existing);
        lines.set(
                idx,
                new OrderLine(
                        existing.id(),
                        existing.productId(),
                        newQuantity,
                        existing.specification(),
                        existing.parties()));
    }

    // --- Process methods ---

    void priceLines() {
        for (OrderLine line : lines) {
            OrderParties effectiveParties = getEffectivePartiesFor(line);
            PricingContext context = PricingContext.forOrderLine(line, effectiveParties);
            OrderLinePricing pricing = services.pricing().calculatePrice(context);
            line.applyPricing(pricing);
        }
    }

    void applyArbitraryPrice(OrderLineId lineId, Money unitPrice, Money totalPrice, String reason) {
        applyPricing(lineId, new ArbitraryPricing(unitPrice, totalPrice, reason));
    }

    void confirm() {
        checkState(status == OrderStatus.DRAFT, "Only DRAFT orders can be confirmed");

        for (OrderLine line : lines) {
            AllocationResult result =
                    services.inventory()
                            .allocate(
                                    AllocationRequest.builder()
                                            .productId(line.productId())
                                            .quantity(line.quantity())
                                            .orderId(this.id)
                                            .build());
            if (result.status() != AllocationStatus.ALLOCATED) {
                throw new IllegalStateException(
                        "Inventory allocation failed for product: "
                                + line.productId()
                                + ", status: "
                                + result.status());
            }
        }

        PaymentResult paymentResult =
                services.payment()
                        .authorizeAndCapture(
                                PaymentRequest.builder()
                                        .orderId(this.id)
                                        .amount(totalPrice().orElse(Money.zero("PLN")))
                                        .build());
        if (paymentResult.status() != PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Payment failed: " + paymentResult.failureReason());
        }

        this.status = OrderStatus.CONFIRMED;
        services.fulfillment().startFulfillment(this.id);
    }

    void cancel() {
        checkState(status.canCancel(), "Cannot cancel order in status: " + status);
        OrderStatus previousStatus = this.status;
        this.status = OrderStatus.CANCELLED;
        if (previousStatus != OrderStatus.DRAFT) {
            services.fulfillment().cancelFulfillment(this.id);
        }
    }

    void updateFulfillmentStatus(FulfillmentStatus fulfillmentStatus) {
        checkState(
                status == OrderStatus.CONFIRMED || status == OrderStatus.PROCESSING,
                "Cannot update fulfillment in status: " + status);
        if (fulfillmentStatus == FulfillmentStatus.IN_PROGRESS
                || fulfillmentStatus == FulfillmentStatus.PARTIALLY_COMPLETED) {
            this.status = OrderStatus.PROCESSING;
        } else if (fulfillmentStatus == FulfillmentStatus.COMPLETED) {
            this.status = OrderStatus.FULFILLED;
        }
    }

    // --- Query methods ---

    boolean isFullyPriced() {
        return lines.stream().allMatch(OrderLine::isPriced);
    }

    boolean hasAllDefinitivePrices() {
        return lines.stream().allMatch(OrderLine::hasDefinitivePrice);
    }

    Optional<Money> totalPrice() {
        if (!isFullyPriced()) {
            return Optional.empty();
        }
        Money total =
                lines.stream()
                        .map(line -> line.pricing().totalPrice())
                        .reduce(Money::add)
                        .orElse(Money.zero("PLN"));
        return Optional.of(total);
    }

    // --- Private helpers ---

    private void applyPricing(OrderLineId lineId, OrderLinePricing pricing) {
        checkArgument(lineId != null, "OrderLineId must be defined");
        checkArgument(pricing != null, "Pricing must be defined");
        OrderLine line =
                lines.stream()
                        .filter(l -> l.id().equals(lineId))
                        .findFirst()
                        .orElseThrow(
                                () -> new IllegalStateException("Order line not found: " + lineId));
        line.applyPricing(pricing);
    }

    // --- Builder ---

    public static Builder builder(OrderId id, OrderParties parties, OrderServices services) {
        return new Builder(id, parties, services);
    }

    public static class Builder {
        private final OrderId id;
        private final OrderParties parties;
        private final OrderServices services;
        private final List<OrderLine> lines = new ArrayList<>();

        private Builder(OrderId id, OrderParties parties, OrderServices services) {
            this.id = id;
            this.parties = parties;
            this.services = services;
        }

        public Builder addLine(Function<LineBuilder, LineBuilder> lineConfig) {
            OrderLine line = lineConfig.apply(new LineBuilder()).build();
            lines.add(line);
            return this;
        }

        public Builder addLine(ProductIdentifier productId, Quantity quantity) {
            return addLine(line -> line.productId(productId).quantity(quantity));
        }

        public Builder addLine(
                ProductIdentifier productId,
                Quantity quantity,
                OrderLineSpecification specification) {
            return addLine(
                    line ->
                            line.productId(productId)
                                    .quantity(quantity)
                                    .specification(specification));
        }

        public Order build() {
            checkState(!lines.isEmpty(), "Order must have at least one line");
            return new Order(id, OrderStatus.DRAFT, lines, parties, services);
        }
    }

    public static class LineBuilder {
        private ProductIdentifier productId;
        private Quantity quantity;
        private OrderLineSpecification specification = OrderLineSpecification.empty();
        private OrderParties parties;

        public LineBuilder productId(ProductIdentifier productId) {
            this.productId = productId;
            return this;
        }

        public LineBuilder quantity(Quantity quantity) {
            this.quantity = quantity;
            return this;
        }

        public LineBuilder specification(OrderLineSpecification specification) {
            this.specification = specification;
            return this;
        }

        public LineBuilder specification(String key, String value) {
            this.specification = OrderLineSpecification.of(key, value);
            return this;
        }

        public LineBuilder specification(Function<SpecBuilder, SpecBuilder> specConfig) {
            this.specification = specConfig.apply(new SpecBuilder()).build();
            return this;
        }

        public LineBuilder parties(OrderParties parties) {
            this.parties = parties;
            return this;
        }

        public LineBuilder parties(Function<PartiesBuilder, PartiesBuilder> partiesConfig) {
            this.parties = partiesConfig.apply(new PartiesBuilder()).build();
            return this;
        }

        OrderLine build() {
            checkState(productId != null, "ProductId must be defined");
            checkState(quantity != null, "Quantity must be defined");

            return new OrderLine(
                    OrderLineId.generate(), productId, quantity, specification, parties);
        }
    }

    public static class SpecBuilder {
        private final Map<String, String> attributes = new HashMap<>();

        public SpecBuilder add(String key, String value) {
            attributes.put(key, value);
            return this;
        }

        public SpecBuilder addAll(Map<String, String> attrs) {
            attributes.putAll(attrs);
            return this;
        }

        public SpecBuilder component(String componentName, String productId) {
            return add("component." + componentName, productId);
        }

        public SpecBuilder componentFeature(
                String componentName, String featureName, String value) {
            return add(componentName + "." + featureName, value);
        }

        public SpecBuilder preference(String preferenceName, String value) {
            return add("_" + preferenceName, value);
        }

        OrderLineSpecification build() {
            return new OrderLineSpecification(attributes);
        }
    }

    public static class PartiesBuilder {
        private final List<PartyInOrder> parties = new ArrayList<>();

        public PartiesBuilder add(PartyInOrder party) {
            parties.add(party);
            return this;
        }

        public PartiesBuilder receiver(PartySnapshot party) {
            return add(PartyInOrder.of(party, RoleInOrder.RECEIVER));
        }

        public PartiesBuilder deliveryContact(PartySnapshot party) {
            return add(PartyInOrder.of(party, RoleInOrder.DELIVERY_CONTACT));
        }

        public PartiesBuilder pickupAuthorized(PartySnapshot party) {
            return add(PartyInOrder.of(party, RoleInOrder.PICKUP_AUTHORIZED));
        }

        OrderParties build() {
            return OrderParties.forOrderLine(parties);
        }
    }

    @Override
    public String toString() {
        return "Order{id=%s, status=%s, lines=%d, parties=%s}"
                .formatted(id, status, lines.size(), parties);
    }
}
