package com.softwarearchetypes.planvsexecution.productionanalysis.tolerance;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating tolerance strategies. Fluent API for composing different tolerance
 * interpretations.
 */
public class ToleranceBuilder {

    private final List<ToleranceStrategy> strategies = new ArrayList<>();

    public static ToleranceStrategy exact() {
        return new ExactMatch();
    }

    public static ToleranceStrategy quantityTolerance(
            double percentageTolerance, int absoluteTolerance) {
        return new QuantityTolerance(percentageTolerance, absoluteTolerance);
    }

    public static ToleranceBuilder tolerance() {
        return new ToleranceBuilder();
    }

    public ToleranceBuilder quantity(double percentageTolerance, int absoluteTolerance) {
        strategies.add(new QuantityTolerance(percentageTolerance, absoluteTolerance));
        return this;
    }

    public ToleranceStrategy build() {
        if (strategies.isEmpty()) {
            return new ExactMatch();
        }
        if (strategies.size() == 1) {
            return strategies.get(0);
        }
        return new CombinedTolerance(strategies);
    }
}
