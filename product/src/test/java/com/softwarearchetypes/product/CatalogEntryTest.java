package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.quantity.Unit;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for CatalogEntry - commercial offering position. */
class CatalogEntryTest {

    private ProductType sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct =
                ProductType.identical(
                        UuidProductIdentifier.random(),
                        ProductName.of("Sample Product"),
                        ProductDescription.of("A sample product for testing"),
                        Unit.pieces());
    }

    @Nested
    class BuilderTests {

        @Test
        void shouldBuildCatalogEntryWithAllFields() {
            CatalogEntryId id = CatalogEntryId.generate();
            Validity validity =
                    Validity.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(id)
                            .displayName("Sample Display Name")
                            .description("Sample description for catalog")
                            .product(sampleProduct)
                            .categories(Set.of("electronics", "gadgets"))
                            .validity(validity)
                            .metadata(Map.of("featured", "true", "badge", "new"))
                            .build();

            assertEquals(id, entry.id());
            assertEquals("Sample Display Name", entry.displayName());
            assertEquals("Sample description for catalog", entry.description());
            assertEquals(sampleProduct, entry.product());
            assertEquals(Set.of("electronics", "gadgets"), entry.categories());
            assertEquals(validity, entry.validity());
            assertEquals(Map.of("featured", "true", "badge", "new"), entry.metadata());
        }

        @Test
        void shouldBuildCatalogEntryWithMinimalFields() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Minimal Entry")
                            .description("Minimal description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .build();

            assertEquals("Minimal Entry", entry.displayName());
            assertTrue(entry.categories().isEmpty());
            assertTrue(entry.metadata().isEmpty());
        }

        @Test
        void shouldAddSingleCategory() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Entry with category")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .category("electronics")
                            .category("gadgets")
                            .build();

            assertTrue(entry.categories().contains("electronics"));
            assertTrue(entry.categories().contains("gadgets"));
        }

        @Test
        void shouldAddSingleMetadata() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Entry with metadata")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .withMetadata("featured", "true")
                            .withMetadata("badge", "new")
                            .build();

            assertEquals("true", entry.metadata().get("featured"));
            assertEquals("new", entry.metadata().get("badge"));
        }

        @Test
        void shouldRejectNullId() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            CatalogEntry.builder()
                                    .id(null)
                                    .displayName("Name")
                                    .description("Description")
                                    .product(sampleProduct)
                                    .validity(Validity.always())
                                    .build());
        }

        @Test
        void shouldRejectNullDisplayName() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            CatalogEntry.builder()
                                    .id(CatalogEntryId.generate())
                                    .displayName(null)
                                    .description("Description")
                                    .product(sampleProduct)
                                    .validity(Validity.always())
                                    .build());
        }

        @Test
        void shouldRejectBlankDisplayName() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            CatalogEntry.builder()
                                    .id(CatalogEntryId.generate())
                                    .displayName("   ")
                                    .description("Description")
                                    .product(sampleProduct)
                                    .validity(Validity.always())
                                    .build());
        }

        @Test
        void shouldRejectNullDescription() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            CatalogEntry.builder()
                                    .id(CatalogEntryId.generate())
                                    .displayName("Name")
                                    .description(null)
                                    .product(sampleProduct)
                                    .validity(Validity.always())
                                    .build());
        }

        @Test
        void shouldRejectNullProduct() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            CatalogEntry.builder()
                                    .id(CatalogEntryId.generate())
                                    .displayName("Name")
                                    .description("Description")
                                    .product(null)
                                    .validity(Validity.always())
                                    .build());
        }

        @Test
        void shouldRejectNullValidity() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            CatalogEntry.builder()
                                    .id(CatalogEntryId.generate())
                                    .displayName("Name")
                                    .description("Description")
                                    .product(sampleProduct)
                                    .validity(null)
                                    .build());
        }
    }

    @Nested
    class AvailabilityTests {

        @Test
        void shouldBeAvailableWithinValidityPeriod() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Seasonal Product")
                            .description("Available in 2024")
                            .product(sampleProduct)
                            .validity(
                                    Validity.between(
                                            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                            .build();

            assertTrue(entry.isAvailableAt(LocalDate.of(2024, 1, 1)));
            assertTrue(entry.isAvailableAt(LocalDate.of(2024, 6, 15)));
            assertTrue(entry.isAvailableAt(LocalDate.of(2024, 12, 31)));
        }

        @Test
        void shouldNotBeAvailableOutsideValidityPeriod() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Seasonal Product")
                            .description("Available in 2024")
                            .product(sampleProduct)
                            .validity(
                                    Validity.between(
                                            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                            .build();

            assertFalse(entry.isAvailableAt(LocalDate.of(2023, 12, 31)));
            assertFalse(entry.isAvailableAt(LocalDate.of(2025, 1, 1)));
        }

        @Test
        void shouldAlwaysBeAvailableWithAlwaysValidity() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Always Available")
                            .description("No restrictions")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .build();

            assertTrue(entry.isAvailableAt(LocalDate.of(1900, 1, 1)));
            assertTrue(entry.isAvailableAt(LocalDate.of(2100, 12, 31)));
        }
    }

    @Nested
    class CategoryTests {

        @Test
        void shouldBeInCategory() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Categorized Product")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .categories(Set.of("electronics", "gadgets", "sale"))
                            .build();

            assertTrue(entry.isInCategory("electronics"));
            assertTrue(entry.isInCategory("gadgets"));
            assertTrue(entry.isInCategory("sale"));
        }

        @Test
        void shouldNotBeInUnassignedCategory() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Categorized Product")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .categories(Set.of("electronics"))
                            .build();

            assertFalse(entry.isInCategory("clothing"));
            assertFalse(entry.isInCategory("ELECTRONICS"));
        }

        @Test
        void shouldNotBeInAnyCategoryWhenEmpty() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Uncategorized Product")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .build();

            assertFalse(entry.isInCategory("electronics"));
            assertTrue(entry.categories().isEmpty());
        }
    }

    @Nested
    class MetadataTests {

        @Test
        void shouldGetMetadataValue() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Product with metadata")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .metadata(Map.of("featured", "true", "badge", "new"))
                            .build();

            Optional<String> featured = entry.getMetadata("featured");
            Optional<String> badge = entry.getMetadata("badge");

            assertTrue(featured.isPresent());
            assertEquals("true", featured.get());
            assertTrue(badge.isPresent());
            assertEquals("new", badge.get());
        }

        @Test
        void shouldReturnEmptyForMissingMetadata() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Product")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .build();

            Optional<String> missing = entry.getMetadata("nonexistent");

            assertFalse(missing.isPresent());
        }

        @Test
        void shouldGetMetadataOrDefault() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Product")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .metadata(Map.of("featured", "true"))
                            .build();

            String featured = entry.getMetadataOrDefault("featured", "false");
            String missing = entry.getMetadataOrDefault("nonexistent", "default");

            assertEquals("true", featured);
            assertEquals("default", missing);
        }

        @Test
        void shouldCheckIfHasMetadata() {
            CatalogEntry entry =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Product")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .metadata(Map.of("featured", "true"))
                            .build();

            assertTrue(entry.hasMetadata("featured"));
            assertFalse(entry.hasMetadata("nonexistent"));
        }
    }

    @Nested
    class ImmutableCopyTests {

        @Test
        void shouldCreateCopyWithNewValidity() {
            CatalogEntry original =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Product")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .categories(Set.of("electronics"))
                            .metadata(Map.of("featured", "true"))
                            .build();

            Validity newValidity = Validity.until(LocalDate.of(2024, 12, 31));
            CatalogEntry updated = original.withValidity(newValidity);

            assertEquals(newValidity, updated.validity());
            assertEquals(original.id(), updated.id());
            assertEquals(original.displayName(), updated.displayName());
            assertEquals(original.categories(), updated.categories());
            assertEquals(original.metadata(), updated.metadata());
        }

        @Test
        void shouldCreateCopyWithNewMetadata() {
            CatalogEntry original =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Product")
                            .description("Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .metadata(Map.of("featured", "true"))
                            .build();

            Map<String, String> newMetadata = Map.of("featured", "false", "badge", "sale");
            CatalogEntry updated = original.withMetadata(newMetadata);

            assertEquals(newMetadata, updated.metadata());
            assertEquals(original.id(), updated.id());
            assertEquals(original.displayName(), updated.displayName());
            assertEquals(original.validity(), updated.validity());
        }
    }

    @Nested
    class EqualityTests {

        @Test
        void shouldBeEqualById() {
            CatalogEntryId id = CatalogEntryId.generate();

            CatalogEntry entry1 =
                    CatalogEntry.builder()
                            .id(id)
                            .displayName("Product 1")
                            .description("Description 1")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .build();

            CatalogEntry entry2 =
                    CatalogEntry.builder()
                            .id(id)
                            .displayName("Product 2")
                            .description("Description 2")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .build();

            assertEquals(entry1, entry2);
            assertEquals(entry1.hashCode(), entry2.hashCode());
        }

        @Test
        void shouldNotBeEqualWithDifferentIds() {
            CatalogEntry entry1 =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Same Name")
                            .description("Same Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .build();

            CatalogEntry entry2 =
                    CatalogEntry.builder()
                            .id(CatalogEntryId.generate())
                            .displayName("Same Name")
                            .description("Same Description")
                            .product(sampleProduct)
                            .validity(Validity.always())
                            .build();

            assertFalse(entry1.equals(entry2));
        }
    }
}
