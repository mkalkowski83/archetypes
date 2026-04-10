package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InterpretationTest {

    @Test
    void shouldHaveTotalAsDefaultInterpretationForSimpleFixed() {
        SimpleFixedCalculator calculator = new SimpleFixedCalculator("test", Money.pln(100));

        assertEquals(Interpretation.TOTAL, calculator.interpretation());
    }

    @Test
    void shouldAllowSettingInterpretationForSimpleFixed() {
        SimpleFixedCalculator calculator =
                new SimpleFixedCalculator("test", Money.pln(100), Interpretation.UNIT);

        assertEquals(Interpretation.UNIT, calculator.interpretation());
    }

    @Test
    void shouldHaveTotalAsDefaultInterpretationForStepFunction() {
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "test", Money.pln(100), new BigDecimal("10"), new BigDecimal("5"));

        assertEquals(Interpretation.TOTAL, calculator.interpretation());
    }

    @Test
    void shouldAllowSettingInterpretationForStepFunction() {
        StepFunctionCalculator calculator =
                new StepFunctionCalculator(
                        "test",
                        Money.pln(100),
                        new BigDecimal("10"),
                        new BigDecimal("5"),
                        Interpretation.MARGINAL);

        assertEquals(Interpretation.MARGINAL, calculator.interpretation());
    }

    @Test
    void shouldHaveTotalAsDefaultInterpretationForDiscretePoints() {
        DiscretePointsCalculator calculator =
                new DiscretePointsCalculator("test", Map.of(new BigDecimal("5"), Money.pln(100)));

        assertEquals(Interpretation.TOTAL, calculator.interpretation());
    }

    @Test
    void shouldAllowSettingInterpretationForDiscretePoints() {
        DiscretePointsCalculator calculator =
                new DiscretePointsCalculator(
                        "test", Map.of(new BigDecimal("5"), Money.pln(100)), Interpretation.UNIT);

        assertEquals(Interpretation.UNIT, calculator.interpretation());
    }

    @Test
    void shouldHaveTotalAsDefaultInterpretationForDailyIncrement() {
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "test", LocalDate.of(2024, 1, 1), Money.pln(100), Money.pln(10));

        assertEquals(Interpretation.TOTAL, calculator.interpretation());
    }

    @Test
    void shouldAllowSettingInterpretationForDailyIncrement() {
        DailyIncrementCalculator calculator =
                new DailyIncrementCalculator(
                        "test",
                        LocalDate.of(2024, 1, 1),
                        Money.pln(100),
                        Money.pln(10),
                        Interpretation.MARGINAL);

        assertEquals(Interpretation.MARGINAL, calculator.interpretation());
    }

    @Test
    void shouldHaveTotalAsDefaultInterpretationForContinuousLinearTime() {
        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "test",
                        LocalDateTime.of(2024, 1, 1, 0, 0),
                        Money.pln(100),
                        LocalDateTime.of(2024, 1, 10, 0, 0),
                        Money.pln(200));

        assertEquals(Interpretation.TOTAL, calculator.interpretation());
    }

    @Test
    void shouldAllowSettingInterpretationForContinuousLinearTime() {
        ContinuousLinearTimeCalculator calculator =
                new ContinuousLinearTimeCalculator(
                        "test",
                        LocalDateTime.of(2024, 1, 1, 0, 0),
                        Money.pln(100),
                        LocalDateTime.of(2024, 1, 10, 0, 0),
                        Money.pln(200),
                        Interpretation.UNIT);

        assertEquals(Interpretation.UNIT, calculator.interpretation());
    }

    @Test
    void shouldProvideDescriptionsForInterpretations() {
        assertEquals("Total price for entire quantity/period", Interpretation.TOTAL.describe());
        assertEquals("Average price per single unit", Interpretation.UNIT.describe());
        assertEquals("Price of n-th specific unit", Interpretation.MARGINAL.describe());
    }
}
