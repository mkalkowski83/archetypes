package com.softwarearchetypes.pricing;

import static com.softwarearchetypes.pricing.ComponentBreakdownAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.softwarearchetypes.quantity.money.Money;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ComponentTest {

    @Test
    void simpleComponentShouldCalculateUsingWrappedCalculator() {
        // given: calculator returning fixed amount
        Calculator calculator =
                new SimpleFixedCalculator("fixed-20", Money.pln(BigDecimal.valueOf(20)));
        SimpleComponent component = SimpleComponent.of("base-fee", calculator);

        // when: calculate
        Money result = component.calculate(Parameters.empty());

        // then: returns calculator result
        assertEquals(Money.pln(BigDecimal.valueOf(20)), result);
        assertEquals(Interpretation.TOTAL, component.interpretation());
    }

    @Test
    void simpleComponentShouldReturnBreakdownWithNoChildren() {
        // given: simple component
        Calculator calculator =
                new SimpleFixedCalculator("fixed-50", Money.pln(BigDecimal.valueOf(50)));
        SimpleComponent component = SimpleComponent.of("service-fee", calculator);

        // when: get breakdown
        ComponentBreakdown breakdown = component.calculateBreakdown(Parameters.empty());

        // then: breakdown has no children
        assertThat(breakdown)
                .hasName("service-fee")
                .hasTotal(Money.pln(BigDecimal.valueOf(50)))
                .hasNoChildren();
    }

    @Test
    void compositeComponentShouldSumChildrenResults() {
        // given: two simple components
        SimpleComponent fee1 =
                SimpleComponent.of(
                        "fee-1",
                        new SimpleFixedCalculator("calc-1", Money.pln(BigDecimal.valueOf(10))));
        SimpleComponent fee2 =
                SimpleComponent.of(
                        "fee-2",
                        new SimpleFixedCalculator("calc-2", Money.pln(BigDecimal.valueOf(30))));

        // and: composite without dependencies
        CompositeComponent composite = CompositeComponent.of("total-fees", fee1, fee2);

        // when: calculate
        Money result = composite.calculate(Parameters.empty());

        // then: sums children (10 + 30)
        assertEquals(Money.pln(BigDecimal.valueOf(40)), result);
    }

    @Test
    void compositeComponentShouldProvideHierarchicalBreakdown() {
        // given: nested structure
        SimpleComponent fee1 =
                SimpleComponent.of(
                        "maintenance",
                        new SimpleFixedCalculator("calc-1", Money.pln(BigDecimal.valueOf(25))));
        SimpleComponent fee2 =
                SimpleComponent.of(
                        "commission",
                        new SimpleFixedCalculator("calc-2", Money.pln(BigDecimal.valueOf(20))));

        CompositeComponent baseFee = CompositeComponent.of("base-fee", fee1, fee2);

        SimpleComponent extra =
                SimpleComponent.of(
                        "extra-charge",
                        new SimpleFixedCalculator("calc-3", Money.pln(BigDecimal.valueOf(5))));

        CompositeComponent total = CompositeComponent.of("total", baseFee, extra);

        // when: get breakdown
        ComponentBreakdown breakdown = total.calculateBreakdown(Parameters.empty());

        // then: hierarchical structure
        assertThat(breakdown)
                .hasName("total")
                .hasTotal(Money.pln(BigDecimal.valueOf(50)))
                .hasChildrenCount(2);

        assertThat(breakdown)
                .child("base-fee")
                .hasTotal(Money.pln(BigDecimal.valueOf(45)))
                .hasChildrenCount(2);

        assertThat(breakdown)
                .child("base-fee")
                .child("maintenance")
                .hasTotal(Money.pln(BigDecimal.valueOf(25)))
                .hasNoChildren();

        assertThat(breakdown)
                .child("extra-charge")
                .hasTotal(Money.pln(BigDecimal.valueOf(5)))
                .hasNoChildren();
    }

    @Test
    void compositeComponentShouldEnrichParametersBasedOnDependencies() {
        // given: base component
        Calculator baseCalculator =
                new SimpleFixedCalculator("base", Money.pln(BigDecimal.valueOf(100)));
        SimpleComponent base = SimpleComponent.of("base-price", baseCalculator);

        // and: dependent component that needs baseAmount parameter
        Calculator percentageCalc = new PercentageCalculator("vat", BigDecimal.valueOf(23));
        SimpleComponent vat = SimpleComponent.of("vat", percentageCalc);

        // and: composite with dependency: vat depends on base-price
        CompositeComponent total =
                CompositeComponent.of(
                        "total-with-vat",
                        Map.of("vat", Map.of("baseAmount", new ValueOf("base-price"))),
                        base,
                        vat);

        // when: calculate
        Money result = total.calculate(Parameters.empty());

        // then: base = 100, vat = 23 (23% of 100), total = 123
        assertEquals(Money.pln(BigDecimal.valueOf(123)), result);
    }

    @Test
    void compositeComponentShouldSupportSumOfDependency() {
        // given: multiple base components
        SimpleComponent fee1 =
                SimpleComponent.of(
                        "fee-1",
                        new SimpleFixedCalculator("c1", Money.pln(BigDecimal.valueOf(50))));
        SimpleComponent fee2 =
                SimpleComponent.of(
                        "fee-2",
                        new SimpleFixedCalculator("c2", Money.pln(BigDecimal.valueOf(30))));

        // and: dependent component calculating percentage of sum
        SimpleComponent tax =
                SimpleComponent.of(
                        "tax", new PercentageCalculator("tax-calc", BigDecimal.valueOf(10)));

        // and: composite where tax depends on sum of fee1 + fee2
        CompositeComponent total =
                CompositeComponent.of(
                        "total-with-tax",
                        Map.of("tax", Map.of("baseAmount", new SumOf("fee-1", "fee-2"))),
                        fee1,
                        fee2,
                        tax);

        // when: calculate
        Money result = total.calculate(Parameters.empty());

        // then: fee1 = 50, fee2 = 30, sum = 80, tax = 8 (10% of 80), total = 88
        assertEquals(Money.pln(BigDecimal.valueOf(88)), result);
    }

    @Test
    void compositeComponentShouldSupportDifferenceOfDependency() {
        // given: revenue and cost components
        SimpleComponent revenue =
                SimpleComponent.of(
                        "revenue",
                        new SimpleFixedCalculator("rev", Money.pln(BigDecimal.valueOf(1000))));
        SimpleComponent costs =
                SimpleComponent.of(
                        "costs",
                        new SimpleFixedCalculator("cost", Money.pln(BigDecimal.valueOf(400))));

        // and: profit calculation based on difference
        SimpleComponent profitTax =
                SimpleComponent.of(
                        "profit-tax", new PercentageCalculator("tax", BigDecimal.valueOf(19)));

        // and: composite where profit-tax depends on (revenue - costs)
        CompositeComponent financials =
                CompositeComponent.of(
                        "financials",
                        Map.of(
                                "profit-tax",
                                Map.of("baseAmount", new DifferenceOf("revenue", "costs"))),
                        revenue,
                        costs,
                        profitTax);

        // when: calculate
        Money result = financials.calculate(Parameters.empty());

        // then: revenue = 1000, costs = 400, profit = 600, tax = 114 (19% of 600)
        // total = 1000 + 400 + 114 = 1514
        assertEquals(Money.pln(BigDecimal.valueOf(1514)), result);
    }

    @Test
    void compositeComponentShouldSupportProductOfDependency() {
        // given: base amount
        SimpleComponent baseAmount =
                SimpleComponent.of(
                        "base",
                        new SimpleFixedCalculator("base", Money.pln(BigDecimal.valueOf(100))));

        // and: component that should receive 150% of base (product with factor 1.5)
        SimpleComponent enhanced =
                SimpleComponent.of(
                        "enhanced", new PercentageCalculator("calc", BigDecimal.valueOf(10)));

        // and: composite where enhanced uses 1.5x base amount
        CompositeComponent total =
                CompositeComponent.of(
                        "total",
                        Map.of(
                                "enhanced",
                                Map.of(
                                        "baseAmount",
                                        new ProductOf("base", BigDecimal.valueOf(1.5)))),
                        baseAmount,
                        enhanced);

        // when: calculate
        Money result = total.calculate(Parameters.empty());

        // then: base = 100, enhanced base = 150 (100 * 1.5), enhanced = 15 (10% of 150)
        // total = 100 + 15 = 115
        assertEquals(Money.pln(BigDecimal.valueOf(115)), result);
    }

    @Test
    void compositeComponentShouldHandleMixedInterpretations() {
        // given: components with different interpretations
        Calculator totalCalc =
                new SimpleFixedCalculator("total", Money.pln(BigDecimal.valueOf(100)));
        SimpleComponent totalComponent = SimpleComponent.of("total-comp", totalCalc);

        Calculator unitCalc =
                new SimpleFixedCalculator(
                        "unit", Money.pln(BigDecimal.valueOf(10)), Interpretation.UNIT);
        SimpleComponent unitComponent = SimpleComponent.of("unit-comp", unitCalc);

        // when: create composite with mixed interpretations - should work now
        CompositeComponent composite =
                CompositeComponent.of("mixed", totalComponent, unitComponent);

        // then: composite always returns TOTAL interpretation
        assertEquals(Interpretation.TOTAL, composite.interpretation());

        // and: calculates correctly by converting children to TOTAL
        // totalComponent = 100 PLN (TOTAL)
        // unitComponent = 10 PLN (UNIT) × 5 quantity = 50 PLN (TOTAL)
        // sum = 150 PLN
        Money result = composite.calculate(Parameters.of("quantity", BigDecimal.valueOf(5)));
        assertEquals(Money.pln(BigDecimal.valueOf(150)), result);
    }

    @Test
    void compositeComponentShouldThrowWhenDependentComponentNotCalculatedYet() {
        // given: two components where second depends on first
        SimpleComponent comp1 =
                SimpleComponent.of(
                        "comp-1",
                        new SimpleFixedCalculator("c1", Money.pln(BigDecimal.valueOf(100))));
        SimpleComponent comp2 =
                SimpleComponent.of(
                        "comp-2", new PercentageCalculator("c2", BigDecimal.valueOf(10)));

        // but: comp-2 is listed BEFORE comp-1 in children list
        // so when comp-2 tries to evaluate, comp-1 hasn't been calculated yet
        List<Component> childrenInWrongOrder = List.of(comp2, comp1);

        CompositeComponent composite =
                CompositeComponent.of(
                        "invalid-order",
                        Map.of("comp-2", Map.of("baseAmount", new ValueOf("comp-1"))),
                        childrenInWrongOrder);

        // when: calculate
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            composite.calculate(Parameters.empty());
                        });

        // then: throws because comp-1 not calculated yet
        assertEquals(
                "Component 'comp-1' has not been calculated yet. Check execution order.",
                exception.getMessage());
    }

    @Test
    void compositeComponentShouldThrowWhenReferencedComponentNotFound() {
        // given: component with dependency on non-existent component
        SimpleComponent comp =
                SimpleComponent.of("comp", new PercentageCalculator("c", BigDecimal.valueOf(10)));

        CompositeComponent composite =
                CompositeComponent.of(
                        "invalid-ref",
                        Map.of("comp", Map.of("baseAmount", new ValueOf("non-existent"))),
                        comp);

        // when: calculate
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            composite.calculate(Parameters.empty());
                        });

        // then: throws because referenced component doesn't exist
        assertEquals("Component 'non-existent' not found", exception.getMessage());
    }

    @Test
    void compositeComponentShouldAlwaysReturnTotalInterpretation() {
        // given: all children have UNIT interpretation
        Calculator calc1 =
                new SimpleFixedCalculator(
                        "c1", Money.pln(BigDecimal.valueOf(10)), Interpretation.UNIT);
        Calculator calc2 =
                new SimpleFixedCalculator(
                        "c2", Money.pln(BigDecimal.valueOf(5)), Interpretation.UNIT);

        SimpleComponent comp1 = SimpleComponent.of("comp1", calc1);
        SimpleComponent comp2 = SimpleComponent.of("comp2", calc2);

        // when: create composite
        CompositeComponent composite = CompositeComponent.of("composite", comp1, comp2);

        // then: composite ALWAYS returns TOTAL (converts children if needed)
        assertEquals(Interpretation.TOTAL, composite.interpretation());

        // and: converts UNIT children to TOTAL before summing
        // comp1 = 10 PLN (UNIT) × 5 = 50 PLN (TOTAL)
        // comp2 = 5 PLN (UNIT) × 5 = 25 PLN (TOTAL)
        // sum = 75 PLN
        Money result = composite.calculate(Parameters.of("quantity", BigDecimal.valueOf(5)));
        assertEquals(Money.pln(BigDecimal.valueOf(75)), result);
    }

    @Test
    void compositeComponentShouldPassParametersToAllChildren() {
        // given: components that use quantity parameter
        Calculator stepCalc1 =
                new StepFunctionCalculator(
                        "step1", Money.pln(BigDecimal.ZERO), BigDecimal.ONE, BigDecimal.valueOf(2));

        Calculator stepCalc2 =
                new StepFunctionCalculator(
                        "step2", Money.pln(BigDecimal.ZERO), BigDecimal.ONE, BigDecimal.valueOf(3));

        SimpleComponent comp1 = SimpleComponent.of("comp1", stepCalc1);
        SimpleComponent comp2 = SimpleComponent.of("comp2", stepCalc2);

        CompositeComponent composite = CompositeComponent.of("total", comp1, comp2);

        // when: calculate with quantity parameter
        Parameters params = Parameters.of("quantity", BigDecimal.valueOf(5));
        Money result = composite.calculate(params);

        // then: both components receive quantity
        // comp1: 5 * 2 = 10
        // comp2: 5 * 3 = 15
        // total = 25
        assertEquals(Money.pln(BigDecimal.valueOf(25)), result);
    }

    @Test
    void compositeComponentShouldThrowWhenEmpty() {
        // given: empty composite
        CompositeComponent empty = CompositeComponent.of("empty", List.of());

        // when: try to calculate
        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            empty.calculate(Parameters.empty());
                        });

        // then: throws descriptive error
        assertEquals("Composite component empty has no children", exception.getMessage());
    }

    @Test
    void simpleComponentShouldMapParameters() {
        // given: calculator that expects "quantity" parameter
        Calculator calculator =
                new StepFunctionCalculator(
                        "calc", Money.pln(BigDecimal.ZERO), BigDecimal.ONE, BigDecimal.valueOf(3));

        // and: component that maps "my_quantity" to "quantity"
        SimpleComponent component =
                SimpleComponent.of("mapped", calculator, Map.of("my_quantity", "quantity"));

        // when: calculate with "my_quantity" parameter
        Money result = component.calculate(Parameters.of("my_quantity", BigDecimal.valueOf(5)));

        // then: calculator receives it as "quantity" and calculates correctly
        // 5 * 3 = 15
        assertEquals(Money.pln(BigDecimal.valueOf(15)), result);
    }

    @Test
    void simpleComponentShouldPassUnmappedParametersThrough() {
        // given: calculator that needs both "quantity" and "time" parameters
        Calculator calculator =
                new StepFunctionCalculator(
                        "calc", Money.pln(BigDecimal.ZERO), BigDecimal.ONE, BigDecimal.valueOf(2));

        // and: component that only maps "my_qty" to "quantity"
        SimpleComponent component =
                SimpleComponent.of("partial-map", calculator, Map.of("my_qty", "quantity"));

        // when: calculate with both mapped and unmapped parameters
        Parameters params =
                Parameters.of("my_qty", BigDecimal.valueOf(10))
                        .with("time", BigDecimal.valueOf(5)); // unmapped, should pass through

        Money result = component.calculate(params);

        // then: works correctly - "my_qty" mapped to "quantity", "time" passed through
        assertEquals(Money.pln(BigDecimal.valueOf(20)), result);
    }

    @Test
    void simpleComponentShouldConvertToTargetInterpretationUsingAdapters() {
        // given: calculator with UNIT interpretation
        Calculator unitPriceCalc =
                new SimpleFixedCalculator(
                        "unit", Money.pln(BigDecimal.valueOf(10)), Interpretation.UNIT);

        SimpleComponent component = SimpleComponent.of("unit-comp", unitPriceCalc);

        // when: calculate as TOTAL (should use adapter)
        Money resultAsTotal =
                component.calculate(
                        Parameters.of("quantity", BigDecimal.valueOf(5)), Interpretation.TOTAL);

        // then: converts UNIT to TOTAL: 10 PLN/unit × 5 units = 50 PLN
        assertEquals(Money.pln(BigDecimal.valueOf(50)), resultAsTotal);

        // when: calculate as UNIT (no conversion needed)
        Money resultAsUnit =
                component.calculate(
                        Parameters.of("quantity", BigDecimal.valueOf(5)), Interpretation.UNIT);

        // then: returns calculator result directly
        assertEquals(Money.pln(BigDecimal.valueOf(10)), resultAsUnit);
    }

    @Test
    void simpleComponentShouldConvertMarginalToTotalUsingAdapter() {
        // given: calculator with MARGINAL interpretation
        Calculator marginalCalc =
                new StepFunctionCalculator(
                        "marginal",
                        Money.pln(BigDecimal.ZERO),
                        BigDecimal.ONE,
                        BigDecimal.valueOf(0.50),
                        Interpretation.MARGINAL);

        SimpleComponent component = SimpleComponent.of("marginal-comp", marginalCalc);

        // when: calculate as TOTAL for 10 units
        Money resultAsTotal =
                component.calculate(
                        Parameters.of("quantity", BigDecimal.valueOf(10)), Interpretation.TOTAL);

        assertEquals(Money.pln(BigDecimal.valueOf(27.5)), resultAsTotal);
    }

    @Test
    void compositeComponentShouldWorkWithChildrenUsingParameterMappings() {
        // given: calculator that expects "quantity" parameter
        Calculator calc1 =
                new StepFunctionCalculator(
                        "calc1", Money.pln(BigDecimal.ZERO), BigDecimal.ONE, BigDecimal.valueOf(2));

        Calculator calc2 =
                new StepFunctionCalculator(
                        "calc2", Money.pln(BigDecimal.ZERO), BigDecimal.ONE, BigDecimal.valueOf(3));

        // and: components with different parameter mappings
        SimpleComponent tier1 = SimpleComponent.of("tier1", calc1, Map.of("tier1_qty", "quantity"));
        SimpleComponent tier2 = SimpleComponent.of("tier2", calc2, Map.of("tier2_qty", "quantity"));

        // and: composite that sums them
        CompositeComponent composite = CompositeComponent.of("total", tier1, tier2);

        // when: calculate with tier-specific parameters
        Parameters params =
                Parameters.of("tier1_qty", BigDecimal.valueOf(5))
                        .with("tier2_qty", BigDecimal.valueOf(3));

        Money result = composite.calculate(params);

        // then: each component receives its mapped parameter
        // tier1: 5 * 2 = 10
        // tier2: 3 * 3 = 9
        // total = 19
        assertEquals(Money.pln(BigDecimal.valueOf(19)), result);
    }

    @Test
    void compositeComponentShouldConvertChildrenWithDifferentInterpretationsToTotal() {
        // given: child with MARGINAL interpretation
        Calculator marginalCalc =
                new StepFunctionCalculator(
                        "marginal",
                        Money.pln(BigDecimal.ZERO),
                        BigDecimal.ONE,
                        BigDecimal.valueOf(1.0),
                        Interpretation.MARGINAL);

        SimpleComponent marginalComp = SimpleComponent.of("marginal", marginalCalc);

        // and: child with UNIT interpretation
        Calculator unitCalc =
                new SimpleFixedCalculator(
                        "unit", Money.pln(BigDecimal.valueOf(5)), Interpretation.UNIT);

        SimpleComponent unitComp = SimpleComponent.of("unit", unitCalc);

        // and: child with TOTAL interpretation
        Calculator totalCalc =
                new SimpleFixedCalculator("total", Money.pln(BigDecimal.valueOf(10)));

        SimpleComponent totalComp = SimpleComponent.of("total", totalCalc);

        // when: create composite with mixed interpretations
        CompositeComponent composite =
                CompositeComponent.of("mixed", marginalComp, unitComp, totalComp);

        // then: composite converts all to TOTAL before summing
        Parameters params = Parameters.of("quantity", BigDecimal.valueOf(3));
        Money result = composite.calculate(params);

        // marginal: MarginalToTotalAdapter sums marginal(1) + marginal(2) + marginal(3)
        //   = 1.0 + 2.0 + 3.0 = 6.0 PLN
        // unit: 5 PLN/unit × 3 units = 15 PLN (converted to TOTAL)
        // total: 10 PLN (already TOTAL)
        // sum = 6 + 15 + 10 = 31 PLN
        assertEquals(Money.pln(BigDecimal.valueOf(31)), result);
    }
}
