package com.softwarearchetypes.scoring.ast;

public record EventRule(Expression filterExpr, Expression scoreExpr) {}
