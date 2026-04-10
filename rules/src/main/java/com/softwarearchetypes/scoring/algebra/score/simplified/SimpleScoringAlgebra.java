package com.softwarearchetypes.scoring.algebra.score.simplified;

import com.softwarearchetypes.scoring.algebra.score.Score;
import com.softwarearchetypes.scoring.ast.CmpOp;
import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.WindowContext;
import java.util.List;

public class SimpleScoringAlgebra implements ScoringAlgebra {

    @Override
    public Score and(Score a, Score b) {
        return new Score(Math.min(a.value(), b.value()));
    }

    @Override
    public Score or(Score a, Score b) {
        return new Score(Math.max(a.value(), b.value()));
    }

    @Override
    public Score not(Score a) {
        int v = a.value();
        return v <= 0 ? new Score(1) : new Score(0);
    }

    @Override
    public Score metricCmp(WindowContext ctx, Metric metric, CmpOp op, double value) {
        double metricValue = ctx.getMetric(metric);
        boolean result = op.compare(metricValue, value);
        return new Score(result ? 1 : 0);
    }

    @Override
    public Score constScore(int value) {
        return new Score(value);
    }

    @Override
    public Score sum(List<Score> children) {
        int total = 0;
        for (Score s : children) {
            total += s.value();
        }
        return new Score(total);
    }

    @Override
    public Score ifThenElse(Score cond, Score thenScore, Score elseScore) {
        return cond.value() > 0 ? thenScore : elseScore;
    }
}
