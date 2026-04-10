package com.softwarearchetypes.scoring.algebra;

import com.softwarearchetypes.scoring.ast.CmpOp;
import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.WindowContext;
import java.util.List;

public interface Algebra<R> {

    R or(R left, R right);

    R and(R left, R right);

    R not(R inner);

    R metricCmp(WindowContext ctx, Metric metric, CmpOp op, double value);

    R constScore(int value);

    R sum(List<R> list);

    R ifThenElse(R cond, R thenV, R elseV);

    default R label(String label, R inner) {
        // ignored in score and fuzzy
        return inner;
    }
}
