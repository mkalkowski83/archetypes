package com.softwarearchetypes.rules.discounting.config.reflection;

import java.util.UUID;

public record DiscountParam(UUID discountId, String paramName, String paramValue) {}
