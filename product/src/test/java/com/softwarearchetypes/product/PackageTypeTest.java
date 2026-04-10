package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.Unit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for PackageType creation and selection validation. */
class PackageTypeTest {

    private ProductType laptop;
    private ProductType mouse;
    private ProductType keyboard;
    private ProductType monitor;
    private ProductType warranty;
    private ProductType insurance;

    @BeforeEach
    void setUp() {
        laptop =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Business Laptop"),
                                ProductDescription.of("Professional laptop"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
                        .build();

        mouse =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Wireless Mouse"),
                                ProductDescription.of("Ergonomic mouse"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .build();

        keyboard =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Mechanical Keyboard"),
                                ProductDescription.of("RGB keyboard"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .build();

        monitor =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("4K Monitor"),
                                ProductDescription.of("27-inch display"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.INDIVIDUALLY_TRACKED)
                        .build();

        warranty =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Extended Warranty"),
                                ProductDescription.of("3-year warranty"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .build();

        insurance =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Device Insurance"),
                                ProductDescription.of("Accidental damage coverage"))
                        .asProductType(Unit.pieces(), ProductTrackingStrategy.IDENTICAL)
                        .build();
    }

    @Test
    void shouldCreateSimplePackageWithRequiredProduct() {
        PackageType bundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Laptop Bundle"),
                                ProductDescription.of("Basic laptop package"))
                        .asPackageType()
                        .withRequiredChoice("laptop", laptop.id())
                        .build();

        assertNotNull(bundle);
        assertEquals(ProductName.of("Laptop Bundle"), bundle.name());
        assertEquals(1, bundle.structure().selectionRules().size());
    }

    @Test
    void shouldValidateSelectionWithRequiredRule() {
        PackageType bundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Laptop Bundle"),
                                ProductDescription.of("Basic laptop package"))
                        .asPackageType()
                        .withRequiredChoice("laptop", laptop.id())
                        .build();

        // Valid: contains required laptop
        List<SelectedProduct> validSelection = List.of(new SelectedProduct(laptop.id(), 1));
        PackageValidationResult result = bundle.validateSelection(validSelection);
        assertTrue(result.isValid(), "Selection with required laptop should be valid");
        assertTrue(result.errors().isEmpty());

        // Invalid: missing required laptop
        List<SelectedProduct> invalidSelection = List.of();
        PackageValidationResult invalidResult = bundle.validateSelection(invalidSelection);
        assertFalse(invalidResult.isValid(), "Empty selection should be invalid");
        assertFalse(invalidResult.errors().isEmpty());
    }

    @Test
    void shouldValidateSelectionWithOptionalRule() {
        PackageType bundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Laptop with Optional Warranty"),
                                ProductDescription.of("Laptop package"))
                        .asPackageType()
                        .withOptionalChoice("warranty", warranty.id())
                        .build();

        // Valid: no warranty
        List<SelectedProduct> withoutWarranty = List.of();
        assertTrue(bundle.validateSelection(withoutWarranty).isValid());

        // Valid: with warranty
        List<SelectedProduct> withWarranty = List.of(new SelectedProduct(warranty.id(), 1));
        assertTrue(bundle.validateSelection(withWarranty).isValid());
    }

    @Test
    void shouldValidateSelectionWithAndRule() {
        PackageType bundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Laptop + Mouse Bundle"),
                                ProductDescription.of("Complete workstation"))
                        .asPackageType()
                        .withRequiredChoice("laptop", laptop.id())
                        .withRequiredChoice("mouse", mouse.id())
                        .build();

        // Valid: both laptop and mouse
        List<SelectedProduct> validSelection =
                List.of(new SelectedProduct(laptop.id(), 1), new SelectedProduct(mouse.id(), 1));
        assertTrue(bundle.validateSelection(validSelection).isValid());

        // Invalid: only laptop
        List<SelectedProduct> onlyLaptop = List.of(new SelectedProduct(laptop.id(), 1));
        assertFalse(bundle.validateSelection(onlyLaptop).isValid());

        // Invalid: only mouse
        List<SelectedProduct> onlyMouse = List.of(new SelectedProduct(mouse.id(), 1));
        assertFalse(bundle.validateSelection(onlyMouse).isValid());
    }

    @Test
    void shouldValidateSelectionWithOrRule() {
        ProductSet mouseSet = ProductSet.of("mouse", mouse.id());
        ProductSet keyboardSet = ProductSet.of("keyboard", keyboard.id());

        SelectionRule rule =
                SelectionRule.or(
                        SelectionRule.required(mouseSet), SelectionRule.required(keyboardSet));

        PackageType bundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Input Device Bundle"),
                                ProductDescription.of("Choose mouse or keyboard"))
                        .asPackageType()
                        .withProductSets(mouseSet, keyboardSet)
                        .withRule(rule)
                        .build();

        // Valid: mouse
        List<SelectedProduct> withMouse = List.of(new SelectedProduct(mouse.id(), 1));
        assertTrue(bundle.validateSelection(withMouse).isValid());

        // Valid: keyboard
        List<SelectedProduct> withKeyboard = List.of(new SelectedProduct(keyboard.id(), 1));
        assertTrue(bundle.validateSelection(withKeyboard).isValid());

        // Valid: both
        List<SelectedProduct> withBoth =
                List.of(new SelectedProduct(mouse.id(), 1), new SelectedProduct(keyboard.id(), 1));
        assertTrue(bundle.validateSelection(withBoth).isValid());

        // Invalid: neither
        List<SelectedProduct> withNeither = List.of();
        assertFalse(bundle.validateSelection(withNeither).isValid());
    }

    @Test
    void shouldValidateSelectionWithNotRule() {
        ProductSet insuranceSet = ProductSet.of("insurance", insurance.id());

        SelectionRule rule = SelectionRule.not(SelectionRule.required(insuranceSet));

        PackageType bundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("No Insurance Bundle"),
                                ProductDescription.of("Insurance not allowed"))
                        .asPackageType()
                        .withProductSet(insuranceSet)
                        .withRule(rule)
                        .build();

        // Valid: no insurance
        List<SelectedProduct> withoutInsurance = List.of();
        assertTrue(bundle.validateSelection(withoutInsurance).isValid());

        // Invalid: with insurance
        List<SelectedProduct> withInsurance = List.of(new SelectedProduct(insurance.id(), 1));
        assertFalse(bundle.validateSelection(withInsurance).isValid());
    }

    @Test
    void shouldValidateSelectionWithConditionalRule() {
        ProductSet laptopSet = ProductSet.of("laptop", laptop.id());
        ProductSet warrantySet = ProductSet.of("warranty", warranty.id());

        // IF laptop THEN warranty required
        SelectionRule rule =
                SelectionRule.ifThen(
                        SelectionRule.required(laptopSet), SelectionRule.required(warrantySet));

        PackageType bundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Laptop with Mandatory Warranty"),
                                ProductDescription.of("Warranty required for laptop"))
                        .asPackageType()
                        .withProductSets(laptopSet, warrantySet)
                        .withRule(rule)
                        .build();

        // Valid: no laptop, no warranty (condition not met)
        List<SelectedProduct> noLaptop = List.of();
        assertTrue(bundle.validateSelection(noLaptop).isValid());

        // Valid: laptop + warranty
        List<SelectedProduct> laptopWithWarranty =
                List.of(new SelectedProduct(laptop.id(), 1), new SelectedProduct(warranty.id(), 1));
        assertTrue(bundle.validateSelection(laptopWithWarranty).isValid());

        // Invalid: laptop without warranty
        List<SelectedProduct> laptopWithoutWarranty = List.of(new SelectedProduct(laptop.id(), 1));
        assertFalse(bundle.validateSelection(laptopWithoutWarranty).isValid());
    }

    @Test
    void shouldValidateComplexSelectionWithMultipleRules() {
        ProductSet laptopSet = ProductSet.of("laptop", laptop.id());
        ProductSet accessoriesSet = ProductSet.of("accessoriesSet", mouse.id(), keyboard.id());
        ProductSet warrantySet = ProductSet.of("warranty", warranty.id());

        // Laptop required + at least one accessory + optional warranty
        SelectionRule rule =
                SelectionRule.and(
                        SelectionRule.required(laptopSet),
                        SelectionRule.isSubsetOf(accessoriesSet, 1, 2),
                        SelectionRule.optional(warrantySet));

        PackageType bundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Complete Workstation"),
                                ProductDescription.of("Laptop with accessories"))
                        .asPackageType()
                        .withProductSets(laptopSet, accessoriesSet, warrantySet)
                        .withRule(rule)
                        .build();

        // Valid: laptop + mouse
        List<SelectedProduct> laptopAndMouse =
                List.of(new SelectedProduct(laptop.id(), 1), new SelectedProduct(mouse.id(), 1));
        assertTrue(bundle.validateSelection(laptopAndMouse).isValid());

        // Valid: laptop + mouse + keyboard + warranty
        List<SelectedProduct> fullBundle =
                List.of(
                        new SelectedProduct(laptop.id(), 1),
                        new SelectedProduct(mouse.id(), 1),
                        new SelectedProduct(keyboard.id(), 1),
                        new SelectedProduct(warranty.id(), 1));
        assertTrue(bundle.validateSelection(fullBundle).isValid());

        // Invalid: laptop only (missing accessories)
        List<SelectedProduct> laptopOnly = List.of(new SelectedProduct(laptop.id(), 1));
        assertFalse(bundle.validateSelection(laptopOnly).isValid());

        // Invalid: no laptop
        List<SelectedProduct> noLaptop = List.of(new SelectedProduct(mouse.id(), 1));
        assertFalse(bundle.validateSelection(noLaptop).isValid());
    }

    @Test
    void shouldValidateIsSubsetOfWithQuantityConstraints() {
        ProductSet accessoriesSet =
                ProductSet.of("accessories", mouse.id(), keyboard.id(), monitor.id());

        // Select 2 to 3 accessories
        SelectionRule rule = SelectionRule.isSubsetOf(accessoriesSet, 2, 3);

        PackageType bundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Accessories Bundle"),
                                ProductDescription.of("Choose 2-3 accessories"))
                        .asPackageType()
                        .withProductSet(accessoriesSet)
                        .withRule(rule)
                        .build();

        // Invalid: too few (1)
        List<SelectedProduct> tooFew = List.of(new SelectedProduct(mouse.id(), 1));
        assertFalse(bundle.validateSelection(tooFew).isValid());

        // Valid: exactly 2
        List<SelectedProduct> exactly2 =
                List.of(new SelectedProduct(mouse.id(), 1), new SelectedProduct(keyboard.id(), 1));
        assertTrue(bundle.validateSelection(exactly2).isValid());

        // Valid: exactly 3
        List<SelectedProduct> exactly3 =
                List.of(
                        new SelectedProduct(mouse.id(), 1),
                        new SelectedProduct(keyboard.id(), 1),
                        new SelectedProduct(monitor.id(), 1));
        assertTrue(bundle.validateSelection(exactly3).isValid());

        // Invalid: too many (4 - but we only have 3 products, so test with quantities)
        List<SelectedProduct> tooMany =
                List.of(
                        new SelectedProduct(mouse.id(), 2),
                        new SelectedProduct(keyboard.id(), 1),
                        new SelectedProduct(monitor.id(), 1));
        assertFalse(bundle.validateSelection(tooMany).isValid());
    }

    @Test
    void shouldCreateNestedPackage() {
        // Inner package: laptop + mouse
        PackageType innerBundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Basic Bundle"),
                                ProductDescription.of("Laptop and mouse"))
                        .asPackageType()
                        .withRequiredChoice("laptop", laptop.id())
                        .withRequiredChoice("mouse", mouse.id())
                        .build();

        // Outer package: basic bundle + optional monitor
        PackageType outerBundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Premium Bundle"),
                                ProductDescription.of("Basic bundle with optional monitor"))
                        .asPackageType()
                        .withRequiredChoice("inner", innerBundle.id())
                        .withOptionalChoice("monitor", monitor.id())
                        .build();

        assertNotNull(outerBundle);
        assertEquals(ProductName.of("Premium Bundle"), outerBundle.name());
        assertEquals(2, outerBundle.structure().selectionRules().size());
    }

    @Test
    void shouldRejectInvalidPackageTypeCreation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    Product.builder(
                                    null, // null ID
                                    ProductName.of("Invalid Package"),
                                    ProductDescription.of("Missing ID"))
                            .asPackageType()
                            .build();
                });

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    Product.builder(
                                    UuidProductIdentifier.random(),
                                    null, // null name
                                    ProductDescription.of("Missing name"))
                            .asPackageType()
                            .build();
                });
    }

    @Test
    void shouldProvideAccessToPackageStructure() {
        ProductSet laptopSet = ProductSet.of("laptop", laptop.id());
        SelectionRule rule1 = SelectionRule.required(laptopSet);
        SelectionRule rule2 = SelectionRule.optional(ProductSet.of("warranty", warranty.id()));

        PackageType bundle =
                Product.builder(
                                UuidProductIdentifier.random(),
                                ProductName.of("Laptop Bundle"),
                                ProductDescription.of("Laptop with optional warranty"))
                        .asPackageType()
                        .withProductSet(laptopSet)
                        .withRule(rule1)
                        .withRule(rule2)
                        .build();

        PackageStructure structure = bundle.structure();
        assertNotNull(structure);
        assertEquals(2, structure.selectionRules().size());
    }
}
