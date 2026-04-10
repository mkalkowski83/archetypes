package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkArgument;
import static com.softwarearchetypes.common.StringUtils.isNotBlank;

/** Represents a requirement for a specific scope within a capability requirement. */
record ScopeRequirement(String scopeType, OperatingScope requiredScope) {

    public ScopeRequirement {
        checkArgument(isNotBlank(scopeType), "Scope type cannot be blank");
        checkArgument(requiredScope != null, "Required scope cannot be null");
    }
}
