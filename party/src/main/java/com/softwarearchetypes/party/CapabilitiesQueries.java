package com.softwarearchetypes.party;

import com.softwarearchetypes.party.OperatingScope.LocationScope;
import com.softwarearchetypes.party.OperatingScope.SkillLevelScope;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Queries for finding parties based on their capabilities. */
public class CapabilitiesQueries {

    private final CapabilitiesRepository repository;

    CapabilitiesQueries(CapabilitiesRepository repository) {
        this.repository = repository;
    }

    public List<Capability> findByPartyId(PartyId partyId) {
        return repository.findByPartyId(partyId).stream()
                .filter(Capability::isCurrentlyValid)
                .toList();
    }

    public List<Capability> findByType(CapabilityType type) {
        return repository.findByType(type).stream().filter(Capability::isCurrentlyValid).toList();
    }

    public List<Capability> findByType(String typeName) {
        return findByType(CapabilityType.of(typeName));
    }

    public Optional<Capability> findById(CapabilityId id) {
        return repository.findById(id);
    }

    /** Find all parties that have a capability of given type. */
    public List<PartyId> findPartiesWithCapability(CapabilityType type) {
        return repository.findByType(type).stream()
                .filter(Capability::isCurrentlyValid)
                .map(Capability::partyId)
                .distinct()
                .toList();
    }

    /** Find all parties that have a capability satisfying the given requirement. */
    public List<PartyId> findPartiesSatisfying(CapabilityRequirement requirement) {
        return repository.findByType(requirement.requiredType()).stream()
                .filter(Capability::isCurrentlyValid)
                .filter(cap -> cap.satisfies(requirement))
                .map(Capability::partyId)
                .distinct()
                .toList();
    }

    /** Find all parties that have capabilities at a specific location. */
    public List<PartyId> findPartiesAtLocation(CapabilityType type, String location) {
        return repository.findByType(type).stream()
                .filter(Capability::isCurrentlyValid)
                .filter(
                        cap ->
                                cap.scope(LocationScope.class)
                                        .map(scope -> scope.includes(location))
                                        .orElse(false))
                .map(Capability::partyId)
                .distinct()
                .toList();
    }

    /** Find all parties with capability of given type and at least given skill level. */
    public List<PartyId> findPartiesWithSkillLevel(CapabilityType type, SkillLevelScope minLevel) {
        return repository.findByType(type).stream()
                .filter(Capability::isCurrentlyValid)
                .filter(
                        cap ->
                                cap.scope(SkillLevelScope.class)
                                        .map(scope -> scope.isAtLeast(minLevel))
                                        .orElse(false))
                .map(Capability::partyId)
                .distinct()
                .toList();
    }

    /** Find all parties with capability matching custom predicate. */
    public List<PartyId> findPartiesMatching(CapabilityType type, Predicate<Capability> predicate) {
        return repository.findByType(type).stream()
                .filter(Capability::isCurrentlyValid)
                .filter(predicate)
                .map(Capability::partyId)
                .distinct()
                .toList();
    }

    /** Find capabilities at location with minimum skill level. */
    public List<Capability> findCapabilitiesAtLocationWithSkill(
            CapabilityType type, String location, SkillLevelScope minLevel) {
        return repository.findByType(type).stream()
                .filter(Capability::isCurrentlyValid)
                .filter(
                        cap ->
                                cap.scope(LocationScope.class)
                                        .map(scope -> scope.includes(location))
                                        .orElse(false))
                .filter(
                        cap ->
                                cap.scope(SkillLevelScope.class)
                                        .map(scope -> scope.isAtLeast(minLevel))
                                        .orElse(false))
                .toList();
    }
}
