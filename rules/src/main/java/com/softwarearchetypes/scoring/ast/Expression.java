package com.softwarearchetypes.scoring.ast;

import java.util.List;

public interface Expression {

    <R> R accept(ExpressionVisitor<R> vistor);

    record And(Expression left, Expression right) implements Expression {
        @Override
        public <R> R accept(ExpressionVisitor<R> vistor) {
            return vistor.visit(this);
        }
    }

    record Or(Expression left, Expression right) implements Expression {

        @Override
        public <R> R accept(ExpressionVisitor<R> vistor) {
            return vistor.visit(this);
        }
    }

    record Not(Expression inner) implements Expression {
        @Override
        public <R> R accept(ExpressionVisitor<R> vistor) {
            return vistor.visit(this);
        }
    }

    record MetricCmp(Metric metric, CmpOp op, double value) implements Expression {
        @Override
        public <R> R accept(ExpressionVisitor<R> vistor) {
            return vistor.visit(this);
        }
    }

    record ConstScore(int value) implements Expression {
        @Override
        public <R> R accept(ExpressionVisitor<R> vistor) {
            return vistor.visit(this);
        }
    }

    record Sum(List<Expression> children) implements Expression {
        @Override
        public <R> R accept(ExpressionVisitor<R> vistor) {
            return vistor.visit(this);
        }
    }

    record IfThenElse(Expression cond, Expression thenBranch, Expression elseBranch)
            implements Expression {
        @Override
        public <R> R accept(ExpressionVisitor<R> vistor) {
            return vistor.visit(this);
        }
    }

    record Labeled(String label, Expression inner) implements Expression {
        @Override
        public <R> R accept(ExpressionVisitor<R> visitor) {
            return visitor.visit(this);
        }
    }
}
