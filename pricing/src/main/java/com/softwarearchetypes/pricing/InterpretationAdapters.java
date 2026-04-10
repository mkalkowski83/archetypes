package com.softwarearchetypes.pricing;

/**
 * Utility for wrapping calculators with interpretation adapters. Provides automatic conversion
 * between TOTAL, UNIT, and MARGINAL interpretations.
 */
class InterpretationAdapters {

    private InterpretationAdapters() {
        // Utility class - no instantiation
    }

    /**
     * Wraps calculator with appropriate adapter to convert from its interpretation to target
     * interpretation. If calculator already has target interpretation, returns it unchanged.
     *
     * @param calc calculator to potentially wrap
     * @param target target interpretation
     * @param adapterNameSuffix suffix for adapter name (e.g., "-to-total")
     * @return calculator or adapter that produces values in target interpretation
     */
    static Calculator adapt(Calculator calc, Interpretation target, String adapterNameSuffix) {
        return switch (target) {
            case TOTAL ->
                    switch (calc.interpretation()) {
                        case TOTAL -> calc;
                        case UNIT -> UnitToTotalAdapter.wrap(calc.name() + adapterNameSuffix, calc);
                        case MARGINAL ->
                                MarginalToTotalAdapter.wrap(calc.name() + adapterNameSuffix, calc);
                    };
            case UNIT ->
                    switch (calc.interpretation()) {
                        case UNIT -> calc;
                        case TOTAL ->
                                TotalToUnitAdapter.wrap(calc.name() + adapterNameSuffix, calc);
                        case MARGINAL ->
                                MarginalToUnitAdapter.wrap(calc.name() + adapterNameSuffix, calc);
                    };
            case MARGINAL ->
                    switch (calc.interpretation()) {
                        case MARGINAL -> calc;
                        case UNIT ->
                                UnitToMarginalAdapter.wrap(calc.name() + adapterNameSuffix, calc);
                        case TOTAL ->
                                TotalToMarginalAdapter.wrap(calc.name() + adapterNameSuffix, calc);
                    };
        };
    }

    /**
     * Wraps calculator with appropriate adapter to convert from its interpretation to target
     * interpretation. Uses default adapter name suffix based on target interpretation.
     *
     * @param calc calculator to potentially wrap
     * @param target target interpretation
     * @return calculator or adapter that produces values in target interpretation
     */
    static Calculator adapt(Calculator calc, Interpretation target) {
        String suffix =
                switch (target) {
                    case TOTAL -> "-to-total";
                    case UNIT -> "-to-unit";
                    case MARGINAL -> "-to-marginal";
                };
        return adapt(calc, target, suffix);
    }
}
