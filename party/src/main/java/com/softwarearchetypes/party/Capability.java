package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.Preconditions.checkArgument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Capability represents what a specific party can do, constrained by operating scopes and validity
 * period. Each capability is assigned to exactly one party.
 *
 * <p>Examples: - Dr. Smith has MedicalImaging capability at Hospital A, working hours, max 20
 * scans/day, valid until 2025 - FastLogistics company has GoodsDelivery capability for Warsaw
 * region with ADR certification, valid until 2026 - Backend Team has SoftwareDevelopment capability
 * for API Gateway at Senior level, always valid
 */
final class Capability {

    private final CapabilityId id;
    private final PartyId partyId;
    private final CapabilityType type;
    private final List<OperatingScope> scopes;
    private final Validity validity;

    private Capability(
            CapabilityId id,
            PartyId partyId,
            CapabilityType type,
            List<OperatingScope> scopes,
            Validity validity) {
        this.id = Objects.requireNonNull(id, "CapabilityId cannot be null");
        this.partyId = Objects.requireNonNull(partyId, "PartyId cannot be null");
        this.type = Objects.requireNonNull(type, "CapabilityType cannot be null");
        this.scopes = List.copyOf(scopes);
        this.validity = Objects.requireNonNull(validity, "Validity cannot be null");
    }

    public static Builder forParty(PartyId partyId) {
        return new Builder(partyId);
    }

    public CapabilityId id() {
        return id;
    }

    public PartyId partyId() {
        return partyId;
    }

    public CapabilityType type() {
        return type;
    }

    public List<OperatingScope> scopes() {
        return scopes;
    }

    public Validity validity() {
        return validity;
    }

    public boolean isCurrentlyValid() {
        return validity.isCurrentlyValid();
    }

    public boolean isValidAt(Instant instant) {
        return validity.isValidAt(instant);
    }

    @SuppressWarnings("unchecked")
    public <T extends OperatingScope> Optional<T> scope(Class<T> scopeClass) {
        return scopes.stream().filter(scopeClass::isInstance).map(s -> (T) s).findFirst();
    }

    public boolean hasScope(Class<? extends OperatingScope> scopeClass) {
        return scopes.stream().anyMatch(scopeClass::isInstance);
    }

    /**
     * Checks if this capability satisfies a requirement. The capability must be valid, have the
     * same type, and all scopes must satisfy the requirement's scopes.
     */
    public boolean satisfies(CapabilityRequirement requirement) {
        if (!isCurrentlyValid()) {
            return false;
        }
        if (!type.equals(requirement.requiredType())) {
            return false;
        }
        for (ScopeRequirement scopeReq : requirement.scopeRequirements()) {
            if (!satisfiesScopeRequirement(scopeReq)) {
                return false;
            }
        }
        return true;
    }

    /** Checks if this capability satisfies a requirement at a specific instant. */
    public boolean satisfiesAt(CapabilityRequirement requirement, Instant at) {
        if (!isValidAt(at)) {
            return false;
        }
        if (!type.equals(requirement.requiredType())) {
            return false;
        }
        for (ScopeRequirement scopeReq : requirement.scopeRequirements()) {
            if (!satisfiesScopeRequirement(scopeReq)) {
                return false;
            }
        }
        return true;
    }

    private boolean satisfiesScopeRequirement(ScopeRequirement requirement) {
        return scopes.stream()
                .filter(s -> s.scopeType().equals(requirement.scopeType()))
                .anyMatch(s -> s.satisfies(requirement.requiredScope()));
    }

    public static final class Builder {
        private final PartyId partyId;
        private CapabilityType type;
        private final List<OperatingScope> scopes = new ArrayList<>();
        private Validity validity = Validity.ALWAYS;

        private Builder(PartyId partyId) {
            this.partyId = Objects.requireNonNull(partyId, "PartyId cannot be null");
        }

        public Builder type(CapabilityType type) {
            this.type = type;
            return this;
        }

        public Builder type(String typeName) {
            this.type = CapabilityType.of(typeName);
            return this;
        }

        public Builder withScope(OperatingScope scope) {
            this.scopes.add(scope);
            return this;
        }

        public Builder validUntil(Instant validTo) {
            this.validity = Validity.until(validTo);
            return this;
        }

        public Builder validFrom(Instant validFrom) {
            this.validity = Validity.from(validFrom);
            return this;
        }

        public Builder validBetween(Instant validFrom, Instant validTo) {
            this.validity = Validity.between(validFrom, validTo);
            return this;
        }

        public Builder validity(Validity validity) {
            this.validity = validity;
            return this;
        }

        public Capability build() {
            checkArgument(type != null, "Capability type is required");
            return new Capability(CapabilityId.random(), partyId, type, scopes, validity);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Capability that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Capability{partyId=%s, type=%s, scopes=%d, validity=%s}"
                .formatted(partyId, type, scopes.size(), validity);
    }
}
