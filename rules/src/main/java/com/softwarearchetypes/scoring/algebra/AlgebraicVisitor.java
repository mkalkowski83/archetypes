package com.softwarearchetypes.scoring.algebra;

import com.softwarearchetypes.scoring.ast.Expression;
import com.softwarearchetypes.scoring.ast.ExpressionVisitor;
import com.softwarearchetypes.scoring.context.WindowContext;
import java.util.ArrayList;
import java.util.List;

public class AlgebraicVisitor<R> implements ExpressionVisitor<R> {

    private final WindowContext ctx;
    private final Algebra<R> algebra;

    public AlgebraicVisitor(WindowContext ctx, Algebra<R> algebra) {
        this.ctx = ctx;
        this.algebra = algebra;
    }

    @Override
    public R visit(Expression.And expr) {
        R left = expr.left().accept(this);
        R right = expr.right().accept(this);
        return algebra.and(left, right);
    }

    @Override
    public R visit(Expression.Or expr) {
        R left = expr.left().accept(this);
        R right = expr.right().accept(this);
        return algebra.or(left, right);
    }

    @Override
    public R visit(Expression.Not expr) {
        R inner = expr.inner().accept(this);
        return algebra.not(inner);
    }

    @Override
    public R visit(Expression.MetricCmp expr) {
        return algebra.metricCmp(ctx, expr.metric(), expr.op(), expr.value());
    }

    @Override
    public R visit(Expression.ConstScore expr) {
        return algebra.constScore(expr.value());
    }

    @Override
    public R visit(Expression.Sum expr) {
        List<R> list = new ArrayList<>();
        for (Expression child : expr.children()) {
            list.add(child.accept(this));
        }
        return algebra.sum(list);
    }

    @Override
    public R visit(Expression.IfThenElse expr) {
        R cond = expr.cond().accept(this);
        R thenV = expr.thenBranch().accept(this);
        R elseV = expr.elseBranch().accept(this);
        return algebra.ifThenElse(cond, thenV, elseV);
    }

    @Override
    public R visit(Expression.Labeled expr) {
        R inner = expr.inner().accept(this);
        return algebra.label(expr.label(), inner);
    }
}
