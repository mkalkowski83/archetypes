package com.softwarearchetypes.scoring.algebra.explained;

import com.softwarearchetypes.scoring.algebra.Algebra;
import com.softwarearchetypes.scoring.ast.CmpOp;
import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.WindowContext;
import java.util.ArrayList;
import java.util.List;

public class ExplainableAlgebra implements Algebra<ExplainedScore> {

    @Override
    public ExplainedScore and(ExplainedScore a, ExplainedScore b) {
        int v = Math.min(a.total(), b.total());
        return new ExplainedScore(v, merge(a, b));
    }

    @Override
    public ExplainedScore or(ExplainedScore a, ExplainedScore b) {
        int v = Math.max(a.total(), b.total());
        return new ExplainedScore(v, merge(a, b));
    }

    @Override
    public ExplainedScore not(ExplainedScore a) {
        int v = a.total() > 0 ? 0 : 1;
        return new ExplainedScore(v, a.contributions());
    }

    @Override
    public ExplainedScore metricCmp(WindowContext ctx, Metric metric, CmpOp op, double value) {
        double mv = ctx.getMetric(metric);
        boolean ok = op.compare(mv, value);
        return new ExplainedScore(ok ? 1 : 0, List.of());
    }

    @Override
    public ExplainedScore constScore(int value) {
        return new ExplainedScore(value, List.of());
    }

    @Override
    public ExplainedScore sum(List<ExplainedScore> children) {
        int total = 0;
        List<Contribution> all = new ArrayList<>();
        for (ExplainedScore es : children) {
            total += es.total();
            all.addAll(es.contributions());
        }
        return new ExplainedScore(total, all);
    }

    @Override
    public ExplainedScore ifThenElse(
            ExplainedScore cond, ExplainedScore thenV, ExplainedScore elseV) {
        return cond.total() > 0 ? thenV : elseV;
    }

    @Override
    public ExplainedScore label(String label, ExplainedScore inner) {
        List<Contribution> list = new ArrayList<>(inner.contributions());
        if (inner.total() != 0) {
            list.add(new Contribution(label, inner.total()));
        }
        return new ExplainedScore(inner.total(), list);
    }

    private List<Contribution> merge(ExplainedScore a, ExplainedScore b) {
        List<Contribution> merged = new ArrayList<>(a.contributions());
        merged.addAll(b.contributions());
        return merged;
    }
}
