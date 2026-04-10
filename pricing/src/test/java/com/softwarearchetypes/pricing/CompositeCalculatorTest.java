package com.softwarearchetypes.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompositeCalculatorTest {

    private CalculatorRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCalculatorsRepository();

        // Add calculators for different ranges
        repository.save(new SimpleFixedCalculator("fixed-100", Money.pln(100)));

        repository.save(
                new StepFunctionCalculator(
                        "step-calc", Money.pln(200), new BigDecimal("10"), new BigDecimal("10")));

        repository.save(
                new DiscretePointsCalculator(
                        "discrete-calc",
                        Map.of(
                                new BigDecimal("50"), Money.pln(500),
                                new BigDecimal("75"), Money.pln(700))));
    }

    @Test
    void should_delegate_to_first_range_calculator() {
        // given
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId();
        CalculatorId stepId = repository.findByName("step-calc").get().getId();
        CalculatorId discreteId = repository.findByName("discrete-calc").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                        CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId),
                        CalculatorRange.numeric(
                                new BigDecimal("50"), new BigDecimal("100"), discreteId));

        CompositeFunctionCalculator calculator =
                new CompositeFunctionCalculator("composite", ranges, repository);

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("5")));

        // when
        Money result = calculator.calculate(params);

        // then
        assertEquals(0, new BigDecimal("100.00").compareTo(result.value()));
    }

    @Test
    void should_delegate_to_second_range_calculator() {
        // given
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId();
        CalculatorId stepId = repository.findByName("step-calc").get().getId();
        CalculatorId discreteId = repository.findByName("discrete-calc").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                        CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId),
                        CalculatorRange.numeric(
                                new BigDecimal("50"), new BigDecimal("100"), discreteId));

        CompositeFunctionCalculator calculator =
                new CompositeFunctionCalculator("piecewise-pricing", ranges, repository);

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("15")));

        // when
        Money result = calculator.calculate(params);

        // then - step calculator: base 200 + 1 step (10) = 210
        assertEquals(0, new BigDecimal("210.00").compareTo(result.value()));
    }

    @Test
    void should_delegate_to_third_range_calculator() {
        // given
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId();
        CalculatorId stepId = repository.findByName("step-calc").get().getId();
        CalculatorId discreteId = repository.findByName("discrete-calc").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                        CalculatorRange.numeric(new BigDecimal("10"), new BigDecimal("50"), stepId),
                        CalculatorRange.numeric(
                                new BigDecimal("50"), new BigDecimal("100"), discreteId));

        CompositeFunctionCalculator calculator =
                new CompositeFunctionCalculator("piecewise-pricing", ranges, repository);

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("75")));

        // when
        Money result = calculator.calculate(params);

        // then - discrete calculator returns 700.00 for quantity 75
        assertEquals(0, new BigDecimal("700.00").compareTo(result.value()));
    }

    @Test
    void should_handle_range_boundaries_correctly() {
        // given
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId();
        CalculatorId stepId = repository.findByName("step-calc").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                        CalculatorRange.numeric(
                                new BigDecimal("10"), new BigDecimal("50"), stepId));

        CompositeFunctionCalculator calculator =
                new CompositeFunctionCalculator("piecewise-pricing", ranges, repository);

        // when & then - quantity 10 is in second range [10, 50) - inclusive lower bound
        Parameters params10 = new Parameters(Map.of("quantity", new BigDecimal("10")));
        Money result10 = calculator.calculate(params10);
        assertEquals(0, new BigDecimal("210.00").compareTo(result10.value()));

        // when & then - quantity 9 is in first range [0, 10)
        Parameters params9 = new Parameters(Map.of("quantity", new BigDecimal("9")));
        Money result9 = calculator.calculate(params9);
        assertEquals(0, new BigDecimal("100.00").compareTo(result9.value()));
    }

    @Test
    void should_throw_when_value_outside_all_ranges() {
        // given
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId();
        CalculatorId stepId = repository.findByName("step-calc").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                        CalculatorRange.numeric(
                                new BigDecimal("10"), new BigDecimal("50"), stepId));

        CompositeFunctionCalculator calculator =
                new CompositeFunctionCalculator("piecewise-pricing", ranges, repository);

        Parameters params = new Parameters(Map.of("quantity", new BigDecimal("100")));

        // when & then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> calculator.calculate(params));

        assertTrue(exception.getMessage().contains("No matching range"));
    }

    @Test
    void should_throw_when_referenced_calculator_not_found() {
        // given
        CalculatorId nonExistentId = CalculatorId.generate();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(
                                new BigDecimal("0"), new BigDecimal("10"), nonExistentId));

        // when & then: should fail during construction due to validation
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                new CompositeFunctionCalculator(
                                        "piecewise-pricing", ranges, repository));

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void should_throw_when_parameter_missing() {
        // given
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(
                                new BigDecimal("0"), new BigDecimal("10"), fixedId));

        CompositeFunctionCalculator calculator =
                new CompositeFunctionCalculator("piecewise-pricing", ranges, repository);

        Parameters params = Parameters.empty();

        // when & then
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(params));
    }

    @Test
    void should_return_correct_type() {
        // given
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(
                                new BigDecimal("0"), new BigDecimal("10"), fixedId));

        CompositeFunctionCalculator calculator =
                new CompositeFunctionCalculator("piecewise-pricing", ranges, repository);

        // when & then
        assertEquals(CalculatorType.COMPOSITE, calculator.getType());
    }

    @Test
    void should_provide_description() {
        // given
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId();
        CalculatorId stepId = repository.findByName("step-calc").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                        CalculatorRange.numeric(
                                new BigDecimal("10"), new BigDecimal("50"), stepId));

        CompositeFunctionCalculator calculator =
                new CompositeFunctionCalculator("piecewise-pricing", ranges, repository);

        // when
        String description = calculator.describe();

        // then
        assertTrue(description.contains("Composite function calculator"));
        assertTrue(description.contains("quantity"));
    }

    @Test
    void should_fail_during_construction_when_calculator_not_found() {
        // given: non-existent calculator ID
        CalculatorId nonExistentId = CalculatorId.generate();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(
                                new BigDecimal("0"), new BigDecimal("10"), nonExistentId));

        // when & then: should fail during construction, not during calculate()
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new CompositeFunctionCalculator("composite", ranges, repository));

        assertTrue(exception.getMessage().contains("not found in repository"));
    }

    @Test
    void should_fail_when_component_calculators_have_different_interpretations() {
        // given: create calculators with different interpretations
        repository.save(
                new SimpleFixedCalculator("total-calc", Money.pln(100), Interpretation.TOTAL));
        repository.save(new SimpleFixedCalculator("unit-calc", Money.pln(10), Interpretation.UNIT));

        CalculatorId totalId = repository.findByName("total-calc").get().getId();
        CalculatorId unitId = repository.findByName("unit-calc").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), totalId),
                        CalculatorRange.numeric(
                                new BigDecimal("10"), new BigDecimal("50"), unitId));

        // when & then: should fail because interpretations don't match
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new CompositeFunctionCalculator("composite", ranges, repository));

        assertTrue(exception.getMessage().contains("same interpretation"));
        assertTrue(exception.getMessage().contains("TOTAL"));
        assertTrue(exception.getMessage().contains("UNIT"));
    }

    @Test
    void should_return_shared_interpretation_of_component_calculators() {
        // given: all calculators have UNIT interpretation
        repository.save(new SimpleFixedCalculator("unit-1", Money.pln(10), Interpretation.UNIT));
        repository.save(new SimpleFixedCalculator("unit-2", Money.pln(8), Interpretation.UNIT));

        CalculatorId unit1Id = repository.findByName("unit-1").get().getId();
        CalculatorId unit2Id = repository.findByName("unit-2").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), unit1Id),
                        CalculatorRange.numeric(
                                new BigDecimal("10"), new BigDecimal("50"), unit2Id));

        CompositeFunctionCalculator calculator =
                new CompositeFunctionCalculator("composite", ranges, repository);

        // when & then: composite should return UNIT interpretation
        assertEquals(Interpretation.UNIT, calculator.interpretation());
    }

    @Test
    void should_allow_composite_with_all_total_price_calculators() {
        // given: all calculators have TOTAL interpretation (default)
        CalculatorId fixedId = repository.findByName("fixed-100").get().getId();
        CalculatorId stepId = repository.findByName("step-calc").get().getId();

        Ranges ranges =
                Ranges.of(
                        "quantity",
                        CalculatorRange.numeric(new BigDecimal("0"), new BigDecimal("10"), fixedId),
                        CalculatorRange.numeric(
                                new BigDecimal("10"), new BigDecimal("50"), stepId));

        // when: create composite - should succeed
        CompositeFunctionCalculator calculator =
                new CompositeFunctionCalculator("composite", ranges, repository);

        // then: should have TOTAL interpretation
        assertEquals(Interpretation.TOTAL, calculator.interpretation());
        assertNotNull(calculator);
    }
}
