package com.softwarearchetypes.scoring.algebra.score.simplified;

import com.softwarearchetypes.scoring.algebra.score.Score;
import com.softwarearchetypes.scoring.ast.CmpOp;
import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.WindowContext;
import java.util.List;

// impl alternative to Visitor
// not important code, just sample
public interface ScoringAlgebra {

    Score and(Score a, Score b);

    Score or(Score a, Score b);

    Score not(Score a);

    Score metricCmp(WindowContext ctx, Metric metric, CmpOp op, double value);

    Score constScore(int value);

    Score sum(List<Score> children);

    Score ifThenElse(Score cond, Score thenScore, Score elseScore);
}
