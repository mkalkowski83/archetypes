package com.softwarearchetypes.ordering;

record OrderServices(
        PricingService pricing,
        InventoryService inventory,
        PaymentService payment,
        FulfillmentService fulfillment) {}
