package com.softwarearchetypes.scoring.algebra.score;

import com.softwarearchetypes.scoring.algebra.Algebra;
import com.softwarearchetypes.scoring.ast.CmpOp;
import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.WindowContext;
import java.util.List;

public class ScoreAlgebra implements Algebra<Score> {

    @Override
    public Score and(Score a, Score b) {
        return new Score(Math.min(a.value(), b.value())); // 0/1 AND
    }

    @Override
    public Score or(Score a, Score b) {
        return new Score(Math.max(a.value(), b.value())); // 0/1 OR
    }

    @Override
    public Score not(Score a) {
        return a.value() > 0 ? new Score(0) : new Score(1);
    }

    @Override
    public Score metricCmp(WindowContext ctx, Metric metric, CmpOp op, double value) {
        double mv = ctx.getMetric(metric);
        boolean ok = op.compare(mv, value);
        return new Score(ok ? 1 : 0); // warunek → 0/1
    }

    @Override
    public Score constScore(int value) {
        return new Score(value); // „prawdziwe” punkty
    }

    @Override
    public Score sum(List<Score> children) {
        int total = 0;
        for (Score s : children) total += s.value();
        return new Score(total);
    }

    @Override
    public Score ifThenElse(Score cond, Score thenValue, Score elseValue) {
        return cond.value() > 0 ? thenValue : elseValue;
    }
}
