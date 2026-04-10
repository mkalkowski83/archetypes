package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a requirement for a capability with specific scopes. Used to check if a party has the
 * required capability to perform a task or assume a role.
 */
record CapabilityRequirement(
        CapabilityType requiredType, List<ScopeRequirement> scopeRequirements) {

    public CapabilityRequirement {
        checkArgument(requiredType != null, "Required type cannot be null");
        scopeRequirements = scopeRequirements != null ? List.copyOf(scopeRequirements) : List.of();
    }

    public static Builder requiring(CapabilityType type) {
        return new Builder(type);
    }

    public static Builder requiring(String typeName) {
        return new Builder(CapabilityType.of(typeName));
    }

    public static final class Builder {
        private final CapabilityType type;
        private final List<ScopeRequirement> scopeRequirements = new ArrayList<>();

        private Builder(CapabilityType type) {
            this.type = type;
        }

        public Builder withScope(OperatingScope requiredScope) {
            scopeRequirements.add(new ScopeRequirement(requiredScope.scopeType(), requiredScope));
            return this;
        }

        public CapabilityRequirement build() {
            return new CapabilityRequirement(type, scopeRequirements);
        }
    }
}
