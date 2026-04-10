package com.softwarearchetypes.ordering;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Aggregate managing parties and their roles in an order.
 *
 * <p>Parties can be specified at two levels: 1. Order level - applies to all order lines by default
 * 2. OrderLine level - overrides or extends order-level parties for specific lines
 *
 * <p>Different roles have different cardinality rules enforced by validation policies.
 */
class OrderParties {
    private final List<PartyInOrder> parties;
    private final RoleValidationPolicy validationPolicy;

    private OrderParties(List<PartyInOrder> parties, RoleValidationPolicy validationPolicy) {
        this.parties = List.copyOf(parties);
        this.validationPolicy = validationPolicy;
        this.validationPolicy.validate(this.parties);
    }

    /** Creates OrderParties for order level with order-level validation policy. */
    public static OrderParties forOrder(List<PartyInOrder> parties) {
        return new OrderParties(parties, new OrderLevelRolePolicy());
    }

    /** Creates OrderParties for order line level with line-level validation policy. */
    public static OrderParties forOrderLine(List<PartyInOrder> parties) {
        if (parties.isEmpty()) {
            return new OrderParties(parties, new OrderLineLevelRolePolicy());
        }
        return new OrderParties(parties, new OrderLineLevelRolePolicy());
    }

    /** Convenience factory for simple orders where one party plays all roles. */
    public static OrderParties singleParty(PartySnapshot customer, PartySnapshot executor) {
        return forOrder(
                List.of(
                        PartyInOrder.of(
                                customer,
                                RoleInOrder.ORDERER,
                                RoleInOrder.PAYER,
                                RoleInOrder.RECEIVER),
                        PartyInOrder.of(executor, RoleInOrder.EXECUTOR)));
    }

    /** Convenience factory for corporate orders. */
    public static OrderParties corporate(
            PartySnapshot company, PartySnapshot executor, PartySnapshot branch) {
        return forOrder(
                List.of(
                        PartyInOrder.of(company, RoleInOrder.ORDERER, RoleInOrder.PAYER),
                        PartyInOrder.of(executor, RoleInOrder.EXECUTOR),
                        PartyInOrder.of(branch, RoleInOrder.RECEIVER)));
    }

    public List<PartyInOrder> parties() {
        return parties;
    }

    /** Returns all parties that have the specified role. */
    public List<PartyInOrder> partiesWithRole(RoleInOrder role) {
        return parties.stream().filter(p -> p.hasRole(role)).toList();
    }

    /**
     * Returns the party with the specified role (if the role has cardinality 1).
     *
     * @throws IllegalStateException if there's not exactly one party with this role
     */
    public PartyInOrder partyWithRole(RoleInOrder role) {
        List<PartyInOrder> found = partiesWithRole(role);
        if (found.isEmpty()) {
            throw new IllegalStateException("No party found with role: " + role);
        }
        if (found.size() > 1) {
            throw new IllegalStateException(
                    "Expected exactly one party with role " + role + ", found: " + found.size());
        }
        return found.get(0);
    }

    /**
     * Merges order-level parties with line-level parties. Line-level parties override order-level
     * parties for the same role.
     *
     * <p>Example: - Order level: Customer A is RECEIVER - Line level: Customer B is RECEIVER -
     * Result: Customer B is RECEIVER (line level overrides)
     *
     * <p>But for roles not specified at line level, order-level parties are used: - Order level:
     * Customer A is ORDERER, PAYER, RECEIVER - Line level: Customer B is RECEIVER - Result:
     * Customer A is ORDERER and PAYER, Customer B is RECEIVER
     */
    public static OrderParties merge(OrderParties orderLevel, OrderParties lineLevel) {
        // Get all roles specified at line level
        Set<RoleInOrder> lineLevelRoles =
                lineLevel.parties.stream()
                        .flatMap(p -> p.roles().stream())
                        .collect(Collectors.toSet());

        // Keep order-level parties only for roles NOT specified at line level
        List<PartyInOrder> orderLevelFiltered =
                orderLevel.parties.stream()
                        .map(
                                p -> {
                                    Set<RoleInOrder> remainingRoles =
                                            p.roles().stream()
                                                    .filter(role -> !lineLevelRoles.contains(role))
                                                    .collect(Collectors.toSet());

                                    if (remainingRoles.isEmpty()) {
                                        return null; // This party has no roles left after filtering
                                    }
                                    return PartyInOrder.of(p.party(), remainingRoles);
                                })
                        .filter(Objects::nonNull)
                        .toList();

        // Merge: order-level (filtered) + line-level
        List<PartyInOrder> merged =
                Stream.concat(orderLevelFiltered.stream(), lineLevel.parties.stream()).toList();

        // Return merged parties without validation (already validated separately)
        return new OrderParties(merged, parties -> {});
    }

    public boolean isEmpty() {
        return parties.isEmpty();
    }
}
