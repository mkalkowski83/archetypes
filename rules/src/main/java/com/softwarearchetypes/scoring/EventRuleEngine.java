package com.softwarearchetypes.scoring;

import com.softwarearchetypes.scoring.algebra.Algebra;
import com.softwarearchetypes.scoring.algebra.AlgebraicVisitor;
import com.softwarearchetypes.scoring.algebra.score.Score;
import com.softwarearchetypes.scoring.ast.EventRule;
import com.softwarearchetypes.scoring.ast.Expression;
import com.softwarearchetypes.scoring.ast.ExpressionVisitor;
import com.softwarearchetypes.scoring.context.EventWindowContext;
import com.softwarearchetypes.scoring.context.WindowContext;
import com.softwarearchetypes.scoring.events.CustomerEvent;
import java.util.List;

public class EventRuleEngine {

    private final Algebra<Score> filterAlgebra;
    private final Algebra<Score> scoreAlgebra;

    public EventRuleEngine(Algebra<Score> filterAlgebra, Algebra<Score> scoreAlgebra) {
        this.filterAlgebra = filterAlgebra;
        this.scoreAlgebra = scoreAlgebra;
    }

    public Score evaluateRules(List<EventRule> rules, WindowContext ctx) {
        Score total = Score.ZERO;
        ExpressionVisitor<Score> filterVisitor = new AlgebraicVisitor<Score>(ctx, filterAlgebra);
        for (CustomerEvent event : ctx.getEvents()) {
            EventWindowContext evCtx = new EventWindowContext(ctx, event);
            ExpressionVisitor<Score> scoreVisitor =
                    new AlgebraicVisitor<Score>(evCtx, scoreAlgebra);
            for (EventRule rule : rules) {
                Expression filterExpression = rule.filterExpr();
                Score condScore = filterExpression.accept(filterVisitor);
                if (condScore.value() > 0) { // > 0 means condition met
                    Expression scoreExpression = rule.scoreExpr();
                    Score s = scoreExpression.accept(scoreVisitor);
                    total = total.plus(s);
                }
            }
        }
        return total;
    }
}
