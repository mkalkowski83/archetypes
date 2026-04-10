package com.softwarearchetypes.ordering;

import java.util.List;
import java.util.Map;

/**
 * Validates role configuration at the OrderLine level.
 *
 * <p>Rules: - ORDERER, EXECUTOR, PAYER: not allowed at line level (order-level only) - RECEIVER: 0
 * or more (optional, can override order-level receivers) - DELIVERY_CONTACT: 0 or more (optional) -
 * PICKUP_AUTHORIZED: 0 or more (optional)
 */
class OrderLineLevelRolePolicy implements RoleValidationPolicy {

    @Override
    public void validate(List<PartyInOrder> parties) {
        Map<RoleInOrder, Long> roleCounts = countByRole(parties);

        // ORDERER – not allowed at line level
        long ordererCount = roleCounts.getOrDefault(RoleInOrder.ORDERER, 0L);
        if (ordererCount > 0) {
            throw new IllegalStateException(
                    "ORDERER role is only allowed at order level, found: "
                            + ordererCount
                            + " at line level");
        }

        // EXECUTOR – not allowed at line level
        long executorCount = roleCounts.getOrDefault(RoleInOrder.EXECUTOR, 0L);
        if (executorCount > 0) {
            throw new IllegalStateException(
                    "EXECUTOR role is only allowed at order level, found: "
                            + executorCount
                            + " at line level");
        }

        // PAYER – not allowed at line level
        long payerCount = roleCounts.getOrDefault(RoleInOrder.PAYER, 0L);
        if (payerCount > 0) {
            throw new IllegalStateException(
                    "PAYER role is only allowed at order level, found: "
                            + payerCount
                            + " at line level");
        }

        // RECEIVER, DELIVERY_CONTACT, PICKUP_AUTHORIZED – allowed (0 or more)
        // No validation needed - any count is valid
    }
}
