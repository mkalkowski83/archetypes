package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines capability requirements for a specific role. Used to verify if a party has all required
 * capabilities to assume a role.
 *
 * <p>Example: Senior Radiologist role requires: - MedicalImaging capability with SkillLevelScope at
 * least "Senior" - Valid medical license (ProtocolScope)
 */
record RoleRequirements(Role role, List<CapabilityRequirement> capabilityRequirements) {

    public RoleRequirements {
        checkArgument(role != null, "Role cannot be null");
        capabilityRequirements =
                capabilityRequirements != null ? List.copyOf(capabilityRequirements) : List.of();
    }

    public static Builder forRole(Role role) {
        return new Builder(role);
    }

    public static Builder forRole(String roleName) {
        return new Builder(Role.of(roleName));
    }

    /** Checks if the given capabilities satisfy all requirements for this role. */
    public boolean isSatisfiedBy(List<Capability> capabilities) {
        for (CapabilityRequirement requirement : capabilityRequirements) {
            boolean satisfied = capabilities.stream().anyMatch(cap -> cap.satisfies(requirement));
            if (!satisfied) {
                return false;
            }
        }
        return true;
    }

    /** Returns list of unsatisfied capability requirements. */
    public List<CapabilityRequirement> findMissing(List<Capability> capabilities) {
        return capabilityRequirements.stream()
                .filter(req -> capabilities.stream().noneMatch(cap -> cap.satisfies(req)))
                .toList();
    }

    public static final class Builder {
        private final Role role;
        private final List<CapabilityRequirement> requirements = new ArrayList<>();

        private Builder(Role role) {
            this.role = role;
        }

        public Builder requireCapability(CapabilityRequirement requirement) {
            requirements.add(requirement);
            return this;
        }

        public RoleRequirements build() {
            return new RoleRequirements(role, requirements);
        }
    }
}
