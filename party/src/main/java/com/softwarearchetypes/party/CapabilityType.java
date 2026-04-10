package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkArgument;
import static com.softwarearchetypes.common.StringUtils.isNotBlank;

/**
 * Represents a type/category of capability. Examples: "MedicalImaging", "GoodsDelivery",
 * "SoftwareDevelopment"
 */
record CapabilityType(String name) {

    public CapabilityType {
        checkArgument(isNotBlank(name), "Capability type name cannot be blank");
    }

    public static CapabilityType of(String name) {
        return new CapabilityType(name);
    }

    public String asString() {
        return name;
    }
}
