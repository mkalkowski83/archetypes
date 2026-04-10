package com.softwarearchetypes.ordering;

import static com.softwarearchetypes.common.Preconditions.checkArgument;

import com.softwarearchetypes.quantity.Quantity;
import java.util.List;

class OrderLine {

    private final OrderLineId id;
    private final ProductIdentifier productId;
    private final Quantity quantity;
    private final OrderLineSpecification specification;
    private final OrderParties parties;
    private OrderLinePricing pricing;

    OrderLine(
            OrderLineId id,
            ProductIdentifier productId,
            Quantity quantity,
            OrderLineSpecification specification,
            OrderParties parties) {
        checkArgument(id != null, "OrderLineId must be defined");
        checkArgument(productId != null, "ProductIdentifier must be defined");
        checkArgument(quantity != null, "Quantity must be defined");
        checkArgument(specification != null, "Specification must be defined");

        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.specification = specification;
        this.parties = parties != null ? parties : OrderParties.forOrderLine(List.of());
        this.pricing = new NotPricedYet();
    }

    OrderLine(
            OrderLineId id,
            ProductIdentifier productId,
            Quantity quantity,
            OrderLineSpecification specification,
            OrderParties parties,
            OrderLinePricing pricing) {
        this(id, productId, quantity, specification, parties);
        this.pricing = pricing != null ? pricing : new NotPricedYet();
    }

    public OrderLineId id() {
        return id;
    }

    public ProductIdentifier productId() {
        return productId;
    }

    public Quantity quantity() {
        return quantity;
    }

    public OrderLineSpecification specification() {
        return specification;
    }

    public OrderParties parties() {
        return parties;
    }

    public OrderLinePricing pricing() {
        return pricing;
    }

    public boolean isPriced() {
        return !(pricing instanceof NotPricedYet);
    }

    public boolean hasDefinitivePrice() {
        return pricing.isDefinitive();
    }

    void applyPricing(OrderLinePricing pricing) {
        checkArgument(pricing != null, "Pricing must be defined");
        this.pricing = pricing;
    }

    public boolean hasLineLevelParties() {
        return parties != null && !parties.isEmpty();
    }

    @Override
    public String toString() {
        return "OrderLine{id=%s, productId=%s, quantity=%s, spec=%s, pricing=%s, parties=%s}"
                .formatted(
                        id,
                        productId,
                        quantity,
                        specification,
                        pricing.getClass().getSimpleName(),
                        hasLineLevelParties() ? parties : "inherited");
    }
}
