package com.softwarearchetypes.rules.predicates;

public final class OrPredicate<T> extends BinaryLogicalPredicate<T> {
    public OrPredicate(LogicalPredicate<T> left, LogicalPredicate<T> right) {
        super(left, right);
    }

    @Override
    public boolean test(T t) {
        return left().test(t) || right().test(t);
    }
}
