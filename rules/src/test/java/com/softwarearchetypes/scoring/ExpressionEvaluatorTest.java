package com.softwarearchetypes.scoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softwarearchetypes.scoring.algebra.AlgebraicVisitor;
import com.softwarearchetypes.scoring.algebra.score.Score;
import com.softwarearchetypes.scoring.algebra.score.ScoreAlgebra;
import com.softwarearchetypes.scoring.algebra.score.simplified.ExpressionEvaluator;
import com.softwarearchetypes.scoring.algebra.score.simplified.ScoringAlgebra;
import com.softwarearchetypes.scoring.algebra.score.simplified.SimpleScoringAlgebra;
import com.softwarearchetypes.scoring.ast.CmpOp;
import com.softwarearchetypes.scoring.ast.Expression;
import com.softwarearchetypes.scoring.ast.Metric;
import com.softwarearchetypes.scoring.context.WindowContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ExpressionEvaluatorTest {

    @Test
    public void simplified_ScoringAlgebraTimeWindowTest() {
        WindowContext ctx =
                new WindowContext(
                        null,
                        null,
                        null,
                        null,
                        Map.of(
                                Metric.YEARLY_PURCHASE_AMOUNT,
                                20000.0,
                                Metric.QUARTERLY_COMPLAINT_COUNT,
                                5.0));
        Expression rule = yearlyAndQuarterlyRule();
        ScoringAlgebra alg = new SimpleScoringAlgebra();

        Score score = ExpressionEvaluator.eval(rule, ctx, alg);

        assertEquals(new Score(20), score);
    }

    @Test
    public void scoringAlgebraTimeWindowTest() {
        WindowContext ctx =
                new WindowContext(
                        null,
                        null,
                        null,
                        null,
                        Map.of(
                                Metric.YEARLY_PURCHASE_AMOUNT,
                                20000.0,
                                Metric.QUARTERLY_COMPLAINT_COUNT,
                                5.0));
        Expression rule = yearlyAndQuarterlyRule();
        AlgebraicVisitor<Score> visitor = new AlgebraicVisitor<>(ctx, new ScoreAlgebra());

        Score score = rule.accept(visitor);

        assertEquals(new Score(20), score);
    }

    private Expression yearlyAndQuarterlyRule() {
        Expression highTurnoverRule =
                new Expression.IfThenElse(
                        new Expression.MetricCmp(Metric.YEARLY_PURCHASE_AMOUNT, CmpOp.GT, 10_000.0),
                        new Expression.ConstScore(50),
                        new Expression.ConstScore(0));

        Expression tooManyComplaintsRule =
                new Expression.IfThenElse(
                        new Expression.MetricCmp(Metric.QUARTERLY_COMPLAINT_COUNT, CmpOp.GT, 3.0),
                        new Expression.ConstScore(-30),
                        new Expression.ConstScore(0));

        return new Expression.Sum(List.of(highTurnoverRule, tooManyComplaintsRule));
    }
}
