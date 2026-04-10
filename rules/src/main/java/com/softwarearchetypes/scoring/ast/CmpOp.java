package com.softwarearchetypes.scoring.ast;

public enum CmpOp {
    GT,
    GTE,
    LT,
    LTE,
    EQ;

    public boolean compare(double left, double right) {
        return switch (this) {
            case GT -> left > right;
            case GTE -> left >= right;
            case LT -> left < right;
            case LTE -> left <= right;
            case EQ -> Double.compare(left, right) == 0;
        };
    }
}
