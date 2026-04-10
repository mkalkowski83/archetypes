package com.softwarearchetypes.pricing;

import com.softwarearchetypes.quantity.money.Money;
import java.util.List;

/**
 * Breakdown showing individual component contributions to total price. Forms a tree structure
 * mirroring the component hierarchy.
 */
record ComponentBreakdown(String name, Money contribution, List<ComponentBreakdown> children) {

    public ComponentBreakdown {
        children = List.copyOf(children);
    }

    public ComponentBreakdown(String name, Money contribution) {
        this(name, contribution, List.of());
    }

    /**
     * Returns the total amount for this breakdown. This is the contribution of this component
     * (which for composite components is already the sum of all children).
     */
    public Money total() {
        return contribution;
    }

    /** Format breakdown as indented text for display. */
    public String format() {
        return formatWithIndent(0);
    }

    private String formatWithIndent(int level) {
        String indent = "  ".repeat(level);
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append(name).append(": ").append(contribution);

        if (!children.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < children.size(); i++) {
                sb.append(children.get(i).formatWithIndent(level + 1));
                if (i < children.size() - 1) {
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }
}
