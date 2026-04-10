package com.softwarearchetypes.ordering;

/**
 * Roles that parties can play in an order. Some roles are order-level (ORDERER, PAYER, EXECUTOR),
 * others can be specified per order line (RECEIVER, DELIVERY_CONTACT).
 */
enum RoleInOrder {
    /** Party that places the order (exactly 1 required at order level) */
    ORDERER,

    /** Party that will pay for the order (0 or 1 at order level) */
    PAYER,

    /** Party that will receive the order (0..n at order level, 0..n at line level) */
    RECEIVER,

    /** Party that executes/fulfills the order (exactly 1 required at order level) */
    EXECUTOR,

    /** Contact person for delivery (0..n at line level only) */
    DELIVERY_CONTACT,

    /** Person authorized to pick up the goods (0..n at line level only) */
    PICKUP_AUTHORIZED
}
