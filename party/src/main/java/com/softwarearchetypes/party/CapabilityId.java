package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkArgument;
import static com.softwarearchetypes.common.StringUtils.isNotBlank;

import java.util.UUID;

public record CapabilityId(String value) {

    public CapabilityId {
        checkArgument(isNotBlank(value), "CapabilityId cannot be blank");
    }

    public static CapabilityId random() {
        return new CapabilityId(UUID.randomUUID().toString());
    }

    public static CapabilityId of(String value) {
        return new CapabilityId(value);
    }

    public String asString() {
        return value;
    }
}
