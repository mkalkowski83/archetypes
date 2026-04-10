package com.softwarearchetypes.ordering.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record CreateOrderCommand(List<OrderPartyData> parties, List<OrderLineData> lines) {

    public record OrderPartyData(
            String partyId, String name, String contactInfo, Set<String> roles) {}

    public record OrderLineData(
            String productId,
            double quantity,
            String unit,
            Map<String, String> specification,
            List<OrderPartyData> parties) {}
}
