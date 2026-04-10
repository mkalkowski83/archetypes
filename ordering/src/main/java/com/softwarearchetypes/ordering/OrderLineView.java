package com.softwarearchetypes.ordering;

import java.util.List;
import java.util.Map;

public record OrderLineView(
        OrderLineId id,
        String productId,
        String quantity,
        Map<String, String> specification,
        List<PartyInOrderView> parties,
        String pricingType,
        String unitPrice,
        String totalPrice,
        List<PriceBreakdownView> breakdown) {

    public record PriceBreakdownView(
            String componentName, String amount, List<PriceBreakdownView> children) {

        static PriceBreakdownView from(PriceBreakdown breakdown) {
            return new PriceBreakdownView(
                    breakdown.componentName(),
                    breakdown.amount().toString(),
                    breakdown.children().stream().map(PriceBreakdownView::from).toList());
        }
    }

    static OrderLineView from(OrderLine line) {
        OrderLinePricing pricing = line.pricing();
        String pricingType = pricing.getClass().getSimpleName();
        String unitPrice = line.isPriced() ? pricing.unitPrice().toString() : null;
        String totalPrice = line.isPriced() ? pricing.totalPrice().toString() : null;
        List<PriceBreakdownView> breakdown =
                pricing.breakdown().stream().map(PriceBreakdownView::from).toList();

        return new OrderLineView(
                line.id(),
                line.productId().value(),
                line.quantity().toString(),
                line.specification().attributes(),
                line.hasLineLevelParties()
                        ? line.parties().parties().stream().map(PartyInOrderView::from).toList()
                        : List.of(),
                pricingType,
                unitPrice,
                totalPrice,
                breakdown);
    }
}
