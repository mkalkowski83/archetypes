package com.softwarearchetypes.rules.predicates;

public final class AndPredicate<T> extends BinaryLogicalPredicate<T> {
    public AndPredicate(LogicalPredicate<T> left, LogicalPredicate<T> right) {
        super(left, right);
    }

    @Override
    public boolean test(T t) {
        return left().test(t) && right().test(t);
    }
}
