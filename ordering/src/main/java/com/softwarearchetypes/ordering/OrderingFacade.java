package com.softwarearchetypes.ordering;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.ordering.commands.AddOrderLineCommand;
import com.softwarearchetypes.ordering.commands.CancelOrderCommand;
import com.softwarearchetypes.ordering.commands.ChangeOrderLineQuantityCommand;
import com.softwarearchetypes.ordering.commands.ConfirmOrderCommand;
import com.softwarearchetypes.ordering.commands.CreateOrderCommand;
import com.softwarearchetypes.ordering.commands.PriceOrderCommand;
import com.softwarearchetypes.ordering.commands.RemoveOrderLineCommand;
import com.softwarearchetypes.ordering.commands.SetArbitraryLinePriceCommand;
import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import com.softwarearchetypes.quantity.money.Money;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderingFacade {

    private final OrderRepository orderRepository;
    private final OrderFactory orderFactory;

    OrderingFacade(OrderRepository orderRepository, OrderFactory orderFactory) {
        this.orderRepository = orderRepository;
        this.orderFactory = orderFactory;
    }

    public Result<String, OrderView> handle(CreateOrderCommand command) {
        try {
            OrderParties orderParties = toOrderParties(command.parties());

            Order.Builder builder = orderFactory.newOrder(OrderId.generate(), orderParties);
            for (CreateOrderCommand.OrderLineData lineData : command.lines()) {
                builder.addLine(
                        line -> {
                            Order.LineBuilder lb =
                                    line.productId(ProductIdentifier.of(lineData.productId()))
                                            .quantity(
                                                    Quantity.of(
                                                            lineData.quantity(),
                                                            Unit.of(
                                                                    lineData.unit(),
                                                                    lineData.unit())));
                            if (lineData.specification() != null
                                    && !lineData.specification().isEmpty()) {
                                lb.specification(
                                        OrderLineSpecification.of(lineData.specification()));
                            }
                            if (lineData.parties() != null && !lineData.parties().isEmpty()) {
                                lb.parties(toOrderLineParties(lineData.parties()));
                            }
                            return lb;
                        });
            }

            Order order = builder.build();
            orderRepository.save(order);
            return Result.success(OrderView.from(order));
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }

    public Result<String, OrderView> handle(AddOrderLineCommand command) {
        return findAndModify(
                command.orderId(),
                order -> {
                    OrderLine line =
                            new OrderLine(
                                    OrderLineId.generate(),
                                    ProductIdentifier.of(command.productId()),
                                    Quantity.of(
                                            command.quantity(),
                                            Unit.of(command.unit(), command.unit())),
                                    command.specification() != null
                                                    && !command.specification().isEmpty()
                                            ? OrderLineSpecification.of(command.specification())
                                            : OrderLineSpecification.empty(),
                                    null);
                    order.addLine(line);
                });
    }

    public Result<String, OrderView> handle(RemoveOrderLineCommand command) {
        return findAndModify(command.orderId(), order -> order.removeLine(command.lineId()));
    }

    public Result<String, OrderView> handle(ChangeOrderLineQuantityCommand command) {
        return findAndModify(
                command.orderId(),
                order ->
                        order.changeLineQuantity(
                                command.lineId(),
                                Quantity.of(
                                        command.newQuantity(),
                                        Unit.of(command.unit(), command.unit()))));
    }

    public Result<String, OrderView> handle(PriceOrderCommand command) {
        return findAndModify(command.orderId(), Order::priceLines);
    }

    public Result<String, OrderView> handle(SetArbitraryLinePriceCommand command) {
        return findAndModify(
                command.orderId(),
                order ->
                        order.applyArbitraryPrice(
                                command.lineId(),
                                Money.of(command.unitPrice(), command.currency()),
                                Money.of(command.totalPrice(), command.currency()),
                                command.reason()));
    }

    public Result<String, OrderView> handle(ConfirmOrderCommand command) {
        return findAndModify(command.orderId(), Order::confirm);
    }

    public Result<String, OrderView> handle(CancelOrderCommand command) {
        return findAndModify(command.orderId(), Order::cancel);
    }

    public Result<String, OrderView> handle(FulfillmentUpdated event) {
        return findAndModify(
                event.orderId(), order -> order.updateFulfillmentStatus(event.status()));
    }

    private Result<String, OrderView> findAndModify(
            OrderId orderId, OrderModification modification) {
        try {
            Order order =
                    orderRepository
                            .findById(orderId)
                            .orElseThrow(
                                    () -> new IllegalStateException("Order not found: " + orderId));
            modification.apply(order);
            orderRepository.save(order);
            return Result.success(OrderView.from(order));
        } catch (Exception e) {
            return Result.failure(e.getMessage());
        }
    }

    private OrderParties toOrderParties(List<CreateOrderCommand.OrderPartyData> partyDataList) {
        List<PartyInOrder> parties = partyDataList.stream().map(this::toPartyInOrder).toList();
        return OrderParties.forOrder(parties);
    }

    private OrderParties toOrderLineParties(List<CreateOrderCommand.OrderPartyData> partyDataList) {
        List<PartyInOrder> parties = partyDataList.stream().map(this::toPartyInOrder).toList();
        return OrderParties.forOrderLine(parties);
    }

    private PartyInOrder toPartyInOrder(CreateOrderCommand.OrderPartyData data) {
        PartySnapshot snapshot =
                PartySnapshot.of(PartyId.of(data.partyId()), data.name(), data.contactInfo());
        Set<RoleInOrder> roles =
                data.roles().stream().map(RoleInOrder::valueOf).collect(Collectors.toSet());
        return PartyInOrder.of(snapshot, roles);
    }

    @FunctionalInterface
    private interface OrderModification {
        void apply(Order order);
    }
}
