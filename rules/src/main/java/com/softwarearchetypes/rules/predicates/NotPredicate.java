package com.softwarearchetypes.rules.predicates;

public final class NotPredicate<T> implements LogicalPredicate<T> {
    private final LogicalPredicate<T> child;

    public NotPredicate(LogicalPredicate<T> child) {
        this.child = child;
    }

    public LogicalPredicate<T> child() {
        return child;
    }

    @Override
    public boolean test(T t) {
        return !child.test(t);
    }
}
