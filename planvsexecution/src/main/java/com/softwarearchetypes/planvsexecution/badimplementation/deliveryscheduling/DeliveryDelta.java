package com.softwarearchetypes.planvsexecution.badimplementation.deliveryscheduling;

public record DeliveryDelta(long dateDifferenceInDays, int quantityDifference, DeltaType type) {}
