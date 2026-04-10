package com.softwarearchetypes.rules.discounting.stock;

import com.softwarearchetypes.quantity.Quantity;
import java.util.UUID;

public record ProductStock(UUID productId, Quantity quantity, int daysInStock) {}
