package com.softwarearchetypes.scoring.algebra.score.simplified;

import com.softwarearchetypes.scoring.algebra.score.Score;
import com.softwarearchetypes.scoring.ast.Expression;
import com.softwarearchetypes.scoring.context.WindowContext;
import java.util.ArrayList;
import java.util.List;

public final class ExpressionEvaluator {

    private ExpressionEvaluator() {}

    public static Score eval(Expression expr, WindowContext ctx, ScoringAlgebra alg) {
        if (expr instanceof Expression.And andExpr) {
            Score left = eval(andExpr.left(), ctx, alg);
            Score right = eval(andExpr.right(), ctx, alg);
            return alg.and(left, right);

        } else if (expr instanceof Expression.Or orExpr) {
            Score left = eval(orExpr.left(), ctx, alg);
            Score right = eval(orExpr.right(), ctx, alg);
            return alg.or(left, right);

        } else if (expr instanceof Expression.Not notExpr) {
            Score inner = eval(notExpr.inner(), ctx, alg);
            return alg.not(inner);

        } else if (expr instanceof Expression.MetricCmp mc) {
            return alg.metricCmp(ctx, mc.metric(), mc.op(), mc.value());

        } else if (expr instanceof Expression.ConstScore cs) {
            return alg.constScore(cs.value());

        } else if (expr instanceof Expression.Sum sum) {
            List<Score> scores = new ArrayList<>();
            for (Expression child : sum.children()) {
                scores.add(eval(child, ctx, alg));
            }
            return alg.sum(scores);

        } else if (expr instanceof Expression.IfThenElse ite) {
            Score cond = eval(ite.cond(), ctx, alg);
            Score thenScore = eval(ite.thenBranch(), ctx, alg);
            Score elseScore = eval(ite.elseBranch(), ctx, alg);
            return alg.ifThenElse(cond, thenScore, elseScore);
        }

        throw new IllegalArgumentException("Unknown Expr type: " + expr.getClass());
    }
}
