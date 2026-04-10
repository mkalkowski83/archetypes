package com.softwarearchetypes.planvsexecution.productionanalysis.tolerance;

import com.softwarearchetypes.planvsexecution.productionanalysis.ActualProduction;
import com.softwarearchetypes.planvsexecution.productionanalysis.PlannedProduction;
import java.util.List;

/**
 * Strategy for matching planned against actual production. Different strategies = different
 * interpretations of the same delta! Pure numerical comparison.
 */
public interface ToleranceStrategy {
    MatchResult matches(PlannedProduction planned, List<ActualProduction> actual);
}
