package com.softwarearchetypes.ordering;

import java.util.List;
import java.util.Map;

/**
 * Validates role configuration at the Order level.
 *
 * <p>Rules: - ORDERER: exactly 1 required - EXECUTOR: exactly 1 required - PAYER: 0 or 1 (optional)
 * - RECEIVER: 0 or more (optional at order level, can be specified per line) - DELIVERY_CONTACT,
 * PICKUP_AUTHORIZED: not allowed at order level (line-level only)
 */
class OrderLevelRolePolicy implements RoleValidationPolicy {

    @Override
    public void validate(List<PartyInOrder> parties) {
        Map<RoleInOrder, Long> roleCounts = countByRole(parties);

        // ORDERER – exactly one
        long ordererCount = roleCounts.getOrDefault(RoleInOrder.ORDERER, 0L);
        if (ordererCount != 1) {
            throw new IllegalStateException(
                    "ORDERER role must appear exactly once, found: " + ordererCount);
        }

        // EXECUTOR – exactly one
        long executorCount = roleCounts.getOrDefault(RoleInOrder.EXECUTOR, 0L);
        if (executorCount != 1) {
            throw new IllegalStateException(
                    "EXECUTOR role must appear exactly once, found: " + executorCount);
        }

        // PAYER – optional (0 or 1)
        long payerCount = roleCounts.getOrDefault(RoleInOrder.PAYER, 0L);
        if (payerCount > 1) {
            throw new IllegalStateException(
                    "PAYER role can appear at most once, found: " + payerCount);
        }

        // RECEIVER – optional (0 or more) at order level
        // No validation needed - any count is valid

        // DELIVERY_CONTACT, PICKUP_AUTHORIZED – not allowed at order level
        long deliveryContactCount = roleCounts.getOrDefault(RoleInOrder.DELIVERY_CONTACT, 0L);
        if (deliveryContactCount > 0) {
            throw new IllegalStateException(
                    "DELIVERY_CONTACT role is only allowed at order line level, found: "
                            + deliveryContactCount);
        }

        long pickupAuthorizedCount = roleCounts.getOrDefault(RoleInOrder.PICKUP_AUTHORIZED, 0L);
        if (pickupAuthorizedCount > 0) {
            throw new IllegalStateException(
                    "PICKUP_AUTHORIZED role is only allowed at order line level, found: "
                            + pickupAuthorizedCount);
        }
    }
}
