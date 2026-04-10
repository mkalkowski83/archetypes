package com.softwarearchetypes.ordering;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Policy interface for validating role configurations in orders. Different policies apply at
 * different levels (Order vs OrderLine).
 */
interface RoleValidationPolicy {

    /**
     * Validates that the party configuration meets the policy requirements.
     *
     * @throws IllegalStateException if validation fails
     */
    void validate(List<PartyInOrder> parties);

    /** Helper method to count occurrences of each role across all parties. */
    default Map<RoleInOrder, Long> countByRole(List<PartyInOrder> parties) {
        return parties.stream()
                .flatMap(p -> p.roles().stream())
                .collect(Collectors.groupingBy(role -> role, Collectors.counting()));
    }
}
