package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for SelectionRule implementations. */
class SelectionRuleTest {

    private ProductIdentifier laptop;
    private ProductIdentifier mouse;
    private ProductIdentifier keyboard;
    private ProductIdentifier monitor;
    private ProductIdentifier warranty;
    private ProductIdentifier insurance;

    @BeforeEach
    void setUp() {
        laptop = UuidProductIdentifier.random();
        mouse = UuidProductIdentifier.random();
        keyboard = UuidProductIdentifier.random();
        monitor = UuidProductIdentifier.random();
        warranty = UuidProductIdentifier.random();
        insurance = UuidProductIdentifier.random();
    }

    // IsSubsetOf tests

    @Test
    void isSubsetOf_shouldAcceptSelectionWithinRange() {
        ProductSet accessories = ProductSet.of("accessories", mouse, keyboard, monitor);
        SelectionRule rule = SelectionRule.isSubsetOf(accessories, 1, 2);

        // Valid: 1 item
        List<SelectedProduct> selection1 = List.of(new SelectedProduct(mouse, 1));
        assertTrue(rule.isSatisfiedBy(selection1));

        // Valid: 2 items
        List<SelectedProduct> selection2 =
                List.of(new SelectedProduct(mouse, 1), new SelectedProduct(keyboard, 1));
        assertTrue(rule.isSatisfiedBy(selection2));
    }

    @Test
    void isSubsetOf_shouldRejectSelectionOutsideRange() {
        ProductSet accessories = ProductSet.of("accessories", mouse, keyboard, monitor);
        SelectionRule rule = SelectionRule.isSubsetOf(accessories, 1, 2);

        // Invalid: 0 items
        List<SelectedProduct> selection0 = List.of();
        assertFalse(rule.isSatisfiedBy(selection0));

        // Invalid: 3 items (too many)
        List<SelectedProduct> selection3 =
                List.of(
                        new SelectedProduct(mouse, 1),
                        new SelectedProduct(keyboard, 1),
                        new SelectedProduct(monitor, 1));
        assertFalse(rule.isSatisfiedBy(selection3));
    }

    @Test
    void isSubsetOf_shouldRejectProductsNotInSet() {
        ProductSet accessories = ProductSet.of("accessories", mouse, keyboard);
        SelectionRule rule = SelectionRule.isSubsetOf(accessories, 1, 2);

        // Invalid: monitor not in accessories set
        List<SelectedProduct> selectionWithMonitor = List.of(new SelectedProduct(monitor, 1));
        assertFalse(rule.isSatisfiedBy(selectionWithMonitor));
    }

    @Test
    void isSubsetOf_shouldCountQuantities() {
        ProductSet accessories = ProductSet.of("accessories", mouse, keyboard);
        SelectionRule rule = SelectionRule.isSubsetOf(accessories, 2, 3);

        // Valid: 2 distinct products with quantity 1 each = 2 total
        List<SelectedProduct> selection =
                List.of(new SelectedProduct(mouse, 1), new SelectedProduct(keyboard, 1));
        assertTrue(rule.isSatisfiedBy(selection));

        // Valid: 1 product with quantity 2 = 2 total
        List<SelectedProduct> selectionWithQuantity = List.of(new SelectedProduct(mouse, 2));
        assertTrue(rule.isSatisfiedBy(selectionWithQuantity));

        // Invalid: 1 product with quantity 1 = 1 total (below min)
        List<SelectedProduct> selectionTooFew = List.of(new SelectedProduct(mouse, 1));
        assertFalse(rule.isSatisfiedBy(selectionTooFew));
    }

    // Required tests

    @Test
    void required_shouldAcceptSelectionWithProduct() {
        ProductSet laptopSet = ProductSet.of("laptopSet", laptop);
        SelectionRule rule = SelectionRule.required(laptopSet);

        List<SelectedProduct> selection = List.of(new SelectedProduct(laptop, 1));
        assertTrue(rule.isSatisfiedBy(selection));
    }

    @Test
    void required_shouldRejectEmptySelection() {
        ProductSet laptopSet = ProductSet.of("laptopSet", laptop);
        SelectionRule rule = SelectionRule.required(laptopSet);

        List<SelectedProduct> emptySelection = List.of();
        assertFalse(rule.isSatisfiedBy(emptySelection));
    }

    // Optional tests

    @Test
    void optional_shouldAcceptBothWithAndWithoutProduct() {
        ProductSet warrantySet = ProductSet.of("warranty", warranty);
        SelectionRule rule = SelectionRule.optional(warrantySet);

        // Valid: with warranty
        List<SelectedProduct> withWarranty = List.of(new SelectedProduct(warranty, 1));
        assertTrue(rule.isSatisfiedBy(withWarranty));

        // Valid: without warranty
        List<SelectedProduct> withoutWarranty = List.of();
        assertTrue(rule.isSatisfiedBy(withoutWarranty));
    }

    // Single tests

    @Test
    void single_shouldAcceptExactlyOne() {
        ProductSet laptopSet = ProductSet.of("laptop", laptop);
        SelectionRule rule = SelectionRule.single(laptopSet);

        // Valid: exactly 1
        List<SelectedProduct> selection = List.of(new SelectedProduct(laptop, 1));
        assertTrue(rule.isSatisfiedBy(selection));
    }

    @Test
    void single_shouldRejectZeroOrMultiple() {
        ProductSet laptopSet = ProductSet.of("laptop", laptop);
        SelectionRule rule = SelectionRule.single(laptopSet);

        // Invalid: 0
        List<SelectedProduct> selection0 = List.of();
        assertFalse(rule.isSatisfiedBy(selection0));

        // Invalid: 2
        List<SelectedProduct> selection2 = List.of(new SelectedProduct(laptop, 2));
        assertFalse(rule.isSatisfiedBy(selection2));
    }

    // AndRule tests

    @Test
    void and_shouldRequireAllRulesToBeSatisfied() {
        ProductSet laptopSet = ProductSet.of("laptop", laptop);
        ProductSet mouseSet = ProductSet.of("mouse", mouse);

        SelectionRule rule =
                SelectionRule.and(
                        SelectionRule.required(laptopSet), SelectionRule.required(mouseSet));

        // Valid: both laptop and mouse
        List<SelectedProduct> bothSelected =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(mouse, 1));
        assertTrue(rule.isSatisfiedBy(bothSelected));

        // Invalid: only laptop
        List<SelectedProduct> onlyLaptop = List.of(new SelectedProduct(laptop, 1));
        assertFalse(rule.isSatisfiedBy(onlyLaptop));

        // Invalid: only mouse
        List<SelectedProduct> onlyMouse = List.of(new SelectedProduct(mouse, 1));
        assertFalse(rule.isSatisfiedBy(onlyMouse));

        // Invalid: neither
        List<SelectedProduct> neither = List.of();
        assertFalse(rule.isSatisfiedBy(neither));
    }

    @Test
    void and_shouldWorkWithThreeRules() {
        SelectionRule rule =
                SelectionRule.and(
                        SelectionRule.required(ProductSet.of("laptop", laptop)),
                        SelectionRule.required(ProductSet.of("mouse", mouse)),
                        SelectionRule.required(ProductSet.of("keyboard", keyboard)));

        // Valid: all three
        List<SelectedProduct> allThree =
                List.of(
                        new SelectedProduct(laptop, 1),
                        new SelectedProduct(mouse, 1),
                        new SelectedProduct(keyboard, 1));
        assertTrue(rule.isSatisfiedBy(allThree));

        // Invalid: missing keyboard
        List<SelectedProduct> missingKeyboard =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(mouse, 1));
        assertFalse(rule.isSatisfiedBy(missingKeyboard));
    }

    // OrRule tests

    @Test
    void or_shouldRequireAtLeastOneRuleToBeSatisfied() {
        ProductSet mouseSet = ProductSet.of("mouse", mouse);
        ProductSet keyboardSet = ProductSet.of("keyboard", keyboard);

        SelectionRule rule =
                SelectionRule.or(
                        SelectionRule.required(mouseSet), SelectionRule.required(keyboardSet));

        // Valid: mouse only
        List<SelectedProduct> onlyMouse = List.of(new SelectedProduct(mouse, 1));
        assertTrue(rule.isSatisfiedBy(onlyMouse));

        // Valid: keyboard only
        List<SelectedProduct> onlyKeyboard = List.of(new SelectedProduct(keyboard, 1));
        assertTrue(rule.isSatisfiedBy(onlyKeyboard));

        // Valid: both
        List<SelectedProduct> both =
                List.of(new SelectedProduct(mouse, 1), new SelectedProduct(keyboard, 1));
        assertTrue(rule.isSatisfiedBy(both));

        // Invalid: neither
        List<SelectedProduct> neither = List.of();
        assertFalse(rule.isSatisfiedBy(neither));
    }

    @Test
    void or_shouldWorkWithThreeRules() {
        SelectionRule rule =
                SelectionRule.or(
                        SelectionRule.required(ProductSet.of("mouse", mouse)),
                        SelectionRule.required(ProductSet.of("keyboard", keyboard)),
                        SelectionRule.required(ProductSet.of("monitor", monitor)));

        // Valid: any one
        List<SelectedProduct> onlyMouse = List.of(new SelectedProduct(mouse, 1));
        assertTrue(rule.isSatisfiedBy(onlyMouse));

        List<SelectedProduct> onlyKeyboard = List.of(new SelectedProduct(keyboard, 1));
        assertTrue(rule.isSatisfiedBy(onlyKeyboard));

        List<SelectedProduct> onlyMonitor = List.of(new SelectedProduct(monitor, 1));
        assertTrue(rule.isSatisfiedBy(onlyMonitor));

        // Valid: combination
        List<SelectedProduct> combo =
                List.of(new SelectedProduct(mouse, 1), new SelectedProduct(monitor, 1));
        assertTrue(rule.isSatisfiedBy(combo));

        // Invalid: none
        List<SelectedProduct> none = List.of();
        assertFalse(rule.isSatisfiedBy(none));
    }

    // NotRule tests

    @Test
    void not_shouldInvertRuleResult() {
        ProductSet insuranceSet = ProductSet.of("insurance", insurance);
        SelectionRule rule = SelectionRule.not(SelectionRule.required(insuranceSet));

        // Valid: no insurance (NOT required = satisfied when absent)
        List<SelectedProduct> withoutInsurance = List.of();
        assertTrue(rule.isSatisfiedBy(withoutInsurance));

        // Invalid: with insurance (NOT required = failed when present)
        List<SelectedProduct> withInsurance = List.of(new SelectedProduct(insurance, 1));
        assertFalse(rule.isSatisfiedBy(withInsurance));
    }

    @Test
    void not_shouldWorkWithComplexRules() {
        // NOT (laptop AND mouse) = allow anything except "laptop AND mouse together"
        SelectionRule rule =
                SelectionRule.not(
                        SelectionRule.and(
                                SelectionRule.required(ProductSet.of("laptop", laptop)),
                                SelectionRule.required(ProductSet.of("mouse", mouse))));

        // Valid: neither
        List<SelectedProduct> neither = List.of();
        assertTrue(rule.isSatisfiedBy(neither));

        // Valid: only laptop
        List<SelectedProduct> onlyLaptop = List.of(new SelectedProduct(laptop, 1));
        assertTrue(rule.isSatisfiedBy(onlyLaptop));

        // Valid: only mouse
        List<SelectedProduct> onlyMouse = List.of(new SelectedProduct(mouse, 1));
        assertTrue(rule.isSatisfiedBy(onlyMouse));

        // Invalid: both laptop and mouse
        List<SelectedProduct> both =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(mouse, 1));
        assertFalse(rule.isSatisfiedBy(both));
    }

    // ConditionalRule tests

    @Test
    void ifThen_shouldEnforceThenRulesWhenConditionIsMet() {
        // IF laptop THEN warranty required
        SelectionRule rule =
                SelectionRule.ifThen(
                        SelectionRule.required(ProductSet.of("laptop", laptop)),
                        SelectionRule.required(ProductSet.of("warranty", warranty)));

        // Valid: no laptop (condition not met)
        List<SelectedProduct> noLaptop = List.of();
        assertTrue(rule.isSatisfiedBy(noLaptop));

        // Valid: laptop + warranty (condition met, then rule satisfied)
        List<SelectedProduct> laptopWithWarranty =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(warranty, 1));
        assertTrue(rule.isSatisfiedBy(laptopWithWarranty));

        // Invalid: laptop without warranty (condition met, then rule not satisfied)
        List<SelectedProduct> laptopWithoutWarranty = List.of(new SelectedProduct(laptop, 1));
        assertFalse(rule.isSatisfiedBy(laptopWithoutWarranty));
    }

    @Test
    void ifThen_shouldWorkWithMultipleThenRules() {
        // IF laptop THEN (warranty AND insurance) required
        SelectionRule rule =
                SelectionRule.ifThen(
                        SelectionRule.required(ProductSet.of("laptop", laptop)),
                        SelectionRule.required(ProductSet.of("warranty", warranty)),
                        SelectionRule.required(ProductSet.of("insurance", insurance)));

        // Valid: no laptop
        List<SelectedProduct> noLaptop = List.of();
        assertTrue(rule.isSatisfiedBy(noLaptop));

        // Valid: laptop + warranty + insurance
        List<SelectedProduct> complete =
                List.of(
                        new SelectedProduct(laptop, 1),
                        new SelectedProduct(warranty, 1),
                        new SelectedProduct(insurance, 1));
        assertTrue(rule.isSatisfiedBy(complete));

        // Invalid: laptop + warranty (missing insurance)
        List<SelectedProduct> missingInsurance =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(warranty, 1));
        assertFalse(rule.isSatisfiedBy(missingInsurance));

        // Invalid: laptop + insurance (missing warranty)
        List<SelectedProduct> missingWarranty =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(insurance, 1));
        assertFalse(rule.isSatisfiedBy(missingWarranty));
    }

    // Complex combinations

    @Test
    void shouldCombineAndOrNotInComplexScenario() {
        // (laptop OR monitor) AND NOT insurance
        SelectionRule rule =
                SelectionRule.and(
                        SelectionRule.or(
                                SelectionRule.required(ProductSet.of("laptop", laptop)),
                                SelectionRule.required(ProductSet.of("monitor", monitor))),
                        SelectionRule.not(
                                SelectionRule.required(ProductSet.of("insurance", insurance))));

        // Valid: laptop, no insurance
        List<SelectedProduct> laptopNoInsurance = List.of(new SelectedProduct(laptop, 1));
        assertTrue(rule.isSatisfiedBy(laptopNoInsurance));

        // Valid: monitor, no insurance
        List<SelectedProduct> monitorNoInsurance = List.of(new SelectedProduct(monitor, 1));
        assertTrue(rule.isSatisfiedBy(monitorNoInsurance));

        // Valid: both laptop and monitor, no insurance
        List<SelectedProduct> bothNoInsurance =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(monitor, 1));
        assertTrue(rule.isSatisfiedBy(bothNoInsurance));

        // Invalid: laptop WITH insurance
        List<SelectedProduct> laptopWithInsurance =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(insurance, 1));
        assertFalse(rule.isSatisfiedBy(laptopWithInsurance));

        // Invalid: neither laptop nor monitor
        List<SelectedProduct> neither = List.of();
        assertFalse(rule.isSatisfiedBy(neither));
    }

    @Test
    void shouldCombineIfThenWithAndOr() {
        // IF laptop THEN (warranty OR insurance)
        SelectionRule rule =
                SelectionRule.ifThen(
                        SelectionRule.required(ProductSet.of("laptop", laptop)),
                        SelectionRule.or(
                                SelectionRule.required(ProductSet.of("warranty", warranty)),
                                SelectionRule.required(ProductSet.of("insurance", insurance))));

        // Valid: no laptop
        List<SelectedProduct> noLaptop = List.of();
        assertTrue(rule.isSatisfiedBy(noLaptop));

        // Valid: laptop + warranty
        List<SelectedProduct> laptopWithWarranty =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(warranty, 1));
        assertTrue(rule.isSatisfiedBy(laptopWithWarranty));

        // Valid: laptop + insurance
        List<SelectedProduct> laptopWithInsurance =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(insurance, 1));
        assertTrue(rule.isSatisfiedBy(laptopWithInsurance));

        // Valid: laptop + both
        List<SelectedProduct> laptopWithBoth =
                List.of(
                        new SelectedProduct(laptop, 1),
                        new SelectedProduct(warranty, 1),
                        new SelectedProduct(insurance, 1));
        assertTrue(rule.isSatisfiedBy(laptopWithBoth));

        // Invalid: laptop without any protection
        List<SelectedProduct> laptopNoProtection = List.of(new SelectedProduct(laptop, 1));
        assertFalse(rule.isSatisfiedBy(laptopNoProtection));
    }

    @Test
    void shouldHandleNestedConditionals() {
        // IF laptop THEN (IF monitor THEN warranty)
        SelectionRule innerIfThen =
                SelectionRule.ifThen(
                        SelectionRule.required(ProductSet.of("monitor", monitor)),
                        SelectionRule.required(ProductSet.of("warranty", warranty)));

        SelectionRule rule =
                SelectionRule.ifThen(
                        SelectionRule.required(ProductSet.of("laptop", laptop)), innerIfThen);

        // Valid: no laptop
        List<SelectedProduct> noLaptop = List.of();
        assertTrue(rule.isSatisfiedBy(noLaptop));

        // Valid: laptop without monitor
        List<SelectedProduct> laptopOnly = List.of(new SelectedProduct(laptop, 1));
        assertTrue(rule.isSatisfiedBy(laptopOnly));

        // Valid: laptop + monitor + warranty
        List<SelectedProduct> complete =
                List.of(
                        new SelectedProduct(laptop, 1),
                        new SelectedProduct(monitor, 1),
                        new SelectedProduct(warranty, 1));
        assertTrue(rule.isSatisfiedBy(complete));

        // Invalid: laptop + monitor without warranty
        List<SelectedProduct> missingWarranty =
                List.of(new SelectedProduct(laptop, 1), new SelectedProduct(monitor, 1));
        assertFalse(rule.isSatisfiedBy(missingWarranty));
    }

    @Test
    void shouldHandleRealWorldBankingScenario() {
        ProductIdentifier basicCard = UuidProductIdentifier.random();
        ProductIdentifier premiumCard = UuidProductIdentifier.random();
        ProductIdentifier basicInsurance = UuidProductIdentifier.random();
        ProductIdentifier extendedInsurance = UuidProductIdentifier.random();
        ProductIdentifier investmentAccount = UuidProductIdentifier.random();

        // Rule: (basicCard OR premiumCard) required
        //       AND IF premiumCard THEN (extendedInsurance AND investmentAccount)
        //       AND NOT basicInsurance when premiumCard selected
        SelectionRule rule =
                SelectionRule.and(
                        SelectionRule.or(
                                SelectionRule.required(ProductSet.of("basic", basicCard)),
                                SelectionRule.required(ProductSet.of("premium", premiumCard))),
                        SelectionRule.ifThen(
                                SelectionRule.required(ProductSet.of("premium", premiumCard)),
                                SelectionRule.required(
                                        ProductSet.of("extendedInsurance", extendedInsurance)),
                                SelectionRule.required(
                                        ProductSet.of("investmentAccount", investmentAccount)),
                                SelectionRule.not(
                                        SelectionRule.required(
                                                ProductSet.of("basicInsurance", basicInsurance)))));

        // Valid: basic card only
        List<SelectedProduct> basicOnly = List.of(new SelectedProduct(basicCard, 1));
        assertTrue(rule.isSatisfiedBy(basicOnly));

        // Valid: basic card with basic insurance
        List<SelectedProduct> basicWithInsurance =
                List.of(new SelectedProduct(basicCard, 1), new SelectedProduct(basicInsurance, 1));
        assertTrue(rule.isSatisfiedBy(basicWithInsurance));

        // Valid: premium card with extended insurance and investment
        List<SelectedProduct> premiumComplete =
                List.of(
                        new SelectedProduct(premiumCard, 1),
                        new SelectedProduct(extendedInsurance, 1),
                        new SelectedProduct(investmentAccount, 1));
        assertTrue(rule.isSatisfiedBy(premiumComplete));

        // Invalid: premium card without extended insurance
        List<SelectedProduct> premiumIncomplete =
                List.of(
                        new SelectedProduct(premiumCard, 1),
                        new SelectedProduct(investmentAccount, 1));
        assertFalse(rule.isSatisfiedBy(premiumIncomplete));

        // Invalid: premium card with basic insurance (NOT allowed)
        List<SelectedProduct> premiumWithBasicInsurance =
                List.of(
                        new SelectedProduct(premiumCard, 1),
                        new SelectedProduct(extendedInsurance, 1),
                        new SelectedProduct(investmentAccount, 1),
                        new SelectedProduct(basicInsurance, 1));
        assertFalse(rule.isSatisfiedBy(premiumWithBasicInsurance));
    }
}
