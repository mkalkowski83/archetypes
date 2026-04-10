package com.softwarearchetypes.scoring.algebra.fuzzy;

import com.softwarearchetypes.scoring.algebra.Algebra;
import com.softwarearchetypes.scoring.ast.CmpOp;
import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.WindowContext;
import java.util.List;
import java.util.Map;

public class FuzzyAlgebra implements Algebra<FuzzyValue> {

    private final Map<Metric, Double> margins;

    public FuzzyAlgebra(Map<Metric, Double> margins) {
        this.margins = margins;
    }

    @Override
    public FuzzyValue and(FuzzyValue a, FuzzyValue b) {
        return new FuzzyValue(Math.min(a.degree(), b.degree()));
    }

    @Override
    public FuzzyValue or(FuzzyValue a, FuzzyValue b) {
        return new FuzzyValue(Math.max(a.degree(), b.degree()));
    }

    @Override
    public FuzzyValue not(FuzzyValue a) {
        return new FuzzyValue(1.0 - a.degree());
    }

    @Override
    public FuzzyValue metricCmp(WindowContext ctx, Metric metric, CmpOp op, double value) {
        double v = ctx.getMetric(metric);
        double margin = margins.getOrDefault(metric, 0.0);

        if (margin <= 0.0) {
            boolean crisp = op.compare(v, value);
            return new FuzzyValue(crisp ? 1.0 : 0.0);
        }

        return switch (op) {
            case GT -> fuzzyGreater(v, value, margin);
            case GTE -> fuzzyGreater(v, value - 0.1 * margin, margin);
            case LT -> fuzzyLess(v, value, margin);
            case LTE -> fuzzyLess(v, value + 0.1 * margin, margin);
            case EQ -> fuzzyEqual(v, value, margin);
        };
    }

    private FuzzyValue fuzzyGreater(double v, double threshold, double margin) {
        if (v <= threshold) return new FuzzyValue(0.0);
        if (v >= threshold + margin) return new FuzzyValue(1.0);
        return new FuzzyValue((v - threshold) / margin);
    }

    private FuzzyValue fuzzyLess(double v, double threshold, double margin) {
        if (v >= threshold) return new FuzzyValue(0.0);
        if (v <= threshold - margin) return new FuzzyValue(1.0);
        return new FuzzyValue((threshold - v) / margin);
    }

    private FuzzyValue fuzzyEqual(double v, double target, double margin) {
        double diff = Math.abs(v - target);
        if (diff >= margin) return new FuzzyValue(0.0);
        return new FuzzyValue(1.0 - (diff / margin));
    }

    @Override
    public FuzzyValue constScore(int value) {
        // np. 0 → 0.0, reszta → 1.0
        return new FuzzyValue(value == 0 ? 0.0 : 1.0);
    }

    @Override
    public FuzzyValue sum(List<FuzzyValue> children) {
        double total = 0.0;
        for (FuzzyValue fv : children) total += fv.degree();
        if (total > 1.0) total = 1.0;
        return new FuzzyValue(total);
    }

    @Override
    public FuzzyValue ifThenElse(FuzzyValue cond, FuzzyValue thenV, FuzzyValue elseV) {
        double c = cond.degree();
        double res = c * thenV.degree() + (1.0 - c) * elseV.degree();
        return new FuzzyValue(res);
    }
}
