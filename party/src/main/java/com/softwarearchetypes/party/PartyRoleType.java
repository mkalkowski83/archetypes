package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkArgument;
import static com.softwarearchetypes.common.StringUtils.isNotBlank;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * PartyRoleType is a definition/template for a role that parties can play. It defines what the role
 * means and what capabilities are required to assume it.
 *
 * <p>Examples: - "Senior Radiologist" - requires MedicalImaging capability with Senior skill level
 * - "Hazmat Driver" - requires GoodsDelivery capability with ADR certification - "Customer" - no
 * specific capability requirements
 *
 * <p>TODO: Integration with PartyRole This class should be connected to PartyRole so that: 1.
 * PartyRole references PartyRoleType instead of simple Role string 2. When assigning a role to
 * Party, the system validates RoleRequirements against Party's Capabilities 3. PartyRole should
 * have Validity period
 *
 * <p>Current PartyRole structure: PartyRole(partyId, role) - simple string-based
 *
 * <p>Target PartyRole structure (see lesson for details): PartyRole { partyRoleId: PartyRoleId
 * partyId: PartyId partyRoleTypeId: PartyRoleTypeId // reference to this type validity: Validity }
 *
 * <p>See: diagrams/party-role-requirements.puml
 */
final class PartyRoleType {

    private final PartyRoleTypeId id;
    private final String name;
    private final String description;
    private final RoleRequirements requirements;

    private PartyRoleType(
            PartyRoleTypeId id, String name, String description, RoleRequirements requirements) {
        checkArgument(id != null, "PartyRoleTypeId cannot be null");
        checkArgument(isNotBlank(name), "Name cannot be blank");
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.requirements = requirements;
    }

    public static PartyRoleType create(String name) {
        return new PartyRoleType(PartyRoleTypeId.random(), name, null, null);
    }

    public static PartyRoleType create(String name, String description) {
        return new PartyRoleType(PartyRoleTypeId.random(), name, description, null);
    }

    public static PartyRoleType create(String name, RoleRequirements requirements) {
        return new PartyRoleType(PartyRoleTypeId.random(), name, null, requirements);
    }

    public static PartyRoleType create(
            String name, String description, RoleRequirements requirements) {
        return new PartyRoleType(PartyRoleTypeId.random(), name, description, requirements);
    }

    public static PartyRoleType withId(
            PartyRoleTypeId id, String name, String description, RoleRequirements requirements) {
        return new PartyRoleType(id, name, description, requirements);
    }

    public PartyRoleTypeId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Optional<RoleRequirements> requirements() {
        return Optional.ofNullable(requirements);
    }

    public boolean hasRequirements() {
        return requirements != null && !requirements.capabilityRequirements().isEmpty();
    }

    /** Checks if a party with given capabilities can assume this role. */
    public boolean canBeAssumedWith(List<Capability> capabilities) {
        if (requirements == null) {
            return true;
        }
        return requirements.isSatisfiedBy(capabilities);
    }

    /** Returns missing capability requirements for given capabilities. */
    public List<CapabilityRequirement> findMissingRequirements(List<Capability> capabilities) {
        if (requirements == null) {
            return List.of();
        }
        return requirements.findMissing(capabilities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartyRoleType that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PartyRoleType{id=%s, name='%s'}".formatted(id, name);
    }
}
