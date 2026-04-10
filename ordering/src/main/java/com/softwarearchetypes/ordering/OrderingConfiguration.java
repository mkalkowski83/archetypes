package com.softwarearchetypes.ordering;

class OrderingConfiguration {

    private final OrderingFacade orderingFacade;
    private final OrderingQueries orderingQueries;
    private final FixableInventoryService inventoryService;
    private final FixablePaymentService paymentService;
    private final FixableFulfillmentService fulfillmentService;
    private final FixablePricingService pricingService;

    private OrderingConfiguration(
            OrderingFacade orderingFacade,
            OrderingQueries orderingQueries,
            FixableInventoryService inventoryService,
            FixablePaymentService paymentService,
            FixableFulfillmentService fulfillmentService,
            FixablePricingService pricingService) {
        this.orderingFacade = orderingFacade;
        this.orderingQueries = orderingQueries;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.fulfillmentService = fulfillmentService;
        this.pricingService = pricingService;
    }

    public static OrderingConfiguration inMemory() {
        FixableInventoryService inventory = new FixableInventoryService();
        FixablePaymentService payment = new FixablePaymentService();
        FixableFulfillmentService fulfillment = new FixableFulfillmentService();
        FixablePricingService pricing = new FixablePricingService();
        OrderServices services = new OrderServices(pricing, inventory, payment, fulfillment);
        OrderFactory factory = new OrderFactory(services);
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        OrderingFacade facade = new OrderingFacade(orderRepository, factory);
        OrderingQueries queries = new OrderingQueries(orderRepository);
        return new OrderingConfiguration(facade, queries, inventory, payment, fulfillment, pricing);
    }

    public OrderingFacade orderingFacade() {
        return orderingFacade;
    }

    public OrderingQueries orderingQueries() {
        return orderingQueries;
    }

    public FixableInventoryService inventoryService() {
        return inventoryService;
    }

    public FixablePaymentService paymentService() {
        return paymentService;
    }

    public FixableFulfillmentService fulfillmentService() {
        return fulfillmentService;
    }

    public FixablePricingService pricingService() {
        return pricingService;
    }
}
