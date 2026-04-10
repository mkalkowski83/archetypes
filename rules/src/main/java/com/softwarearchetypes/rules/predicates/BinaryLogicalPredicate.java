package com.softwarearchetypes.rules.predicates;

public abstract class BinaryLogicalPredicate<T> implements LogicalPredicate<T> {
    private final LogicalPredicate<T> left;
    private final LogicalPredicate<T> right;

    protected BinaryLogicalPredicate(LogicalPredicate<T> left, LogicalPredicate<T> right) {
        this.left = left;
        this.right = right;
    }

    public LogicalPredicate<T> left() {
        return left;
    }

    public LogicalPredicate<T> right() {
        return right;
    }
}
