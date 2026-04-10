package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.product.ProductCommands.AddToOffer;
import com.softwarearchetypes.product.ProductCommands.DiscontinueProduct;
import com.softwarearchetypes.product.ProductCommands.UpdateMetadata;
import com.softwarearchetypes.product.ProductQueries.FindAvailableAtCriteria;
import com.softwarearchetypes.product.ProductQueries.FindByCategoryCriteria;
import com.softwarearchetypes.product.ProductQueries.FindByMetadataCriteria;
import com.softwarearchetypes.product.ProductQueries.FindCatalogEntryCriteria;
import com.softwarearchetypes.product.ProductQueries.SearchCatalogCriteria;
import com.softwarearchetypes.product.ProductViews.CatalogEntryView;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for ProductCatalog - main API for managing commercial product offering. */
class ProductCatalogTest {

    private ProductConfiguration configuration;
    private ProductCatalog catalog;
    private ProductTypeRepository productTypeRepository;

    @BeforeEach
    void setUp() {
        configuration = ProductConfiguration.inMemory();
        catalog = configuration.productCatalog();
        productTypeRepository = configuration.productTypeRepository();
    }

    // ===========================================
    // Adding to offer
    // ===========================================

    @Test
    void shouldAddProductToOfferAndFindItById() {
        // given
        ProductType laptop = thereIsProduct("Business Laptop");

        // when
        Result<String, CatalogEntryId> result =
                catalog.handle(
                        new AddToOffer(
                                laptop.id().toString(),
                                "Premium Laptop",
                                "High-end business laptop",
                                Set.of("electronics"),
                                null,
                                null,
                                Map.of()));

        // then
        assertTrue(result.success());
        CatalogEntryView found =
                catalog.findBy(new FindCatalogEntryCriteria(result.getSuccess().value()))
                        .orElseThrow();
        assertEquals("Premium Laptop", found.displayName());
        assertEquals("High-end business laptop", found.description());
        assertEquals(laptop.id().toString(), found.productTypeId());
    }

    @Test
    void shouldFailToAddNonExistentProductToOffer() {
        // given
        ProductIdentifier nonExistent = UuidProductIdentifier.random();

        // when
        Result<String, CatalogEntryId> result =
                catalog.handle(
                        new AddToOffer(
                                nonExistent.toString(),
                                "Ghost Product",
                                "Does not exist",
                                Set.of(),
                                null,
                                null,
                                Map.of()));

        // then
        assertTrue(result.failure());
        assertTrue(result.getFailure().contains("not found"));
    }

    // ===========================================
    // Finding by category
    // ===========================================

    @Test
    void shouldFindProductsByCategory() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        ProductType phone = thereIsProduct("Phone");
        thereIsCatalogEntry(laptop, "Gaming Laptop", Set.of("electronics", "gaming"));
        thereIsCatalogEntry(phone, "Smartphone", Set.of("electronics", "phones"));

        // when
        Set<CatalogEntryView> electronics =
                catalog.findBy(new FindByCategoryCriteria("electronics"));
        Set<CatalogEntryView> gaming = catalog.findBy(new FindByCategoryCriteria("gaming"));
        Set<CatalogEntryView> phones = catalog.findBy(new FindByCategoryCriteria("phones"));

        // then
        assertEquals(2, electronics.size());
        assertEquals(1, gaming.size());
        assertEquals(1, phones.size());
    }

    @Test
    void shouldReturnEmptySetForNonExistentCategory() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        thereIsCatalogEntry(laptop, "Laptop", Set.of("electronics"));

        // when
        Set<CatalogEntryView> result = catalog.findBy(new FindByCategoryCriteria("non-existent"));

        // then
        assertTrue(result.isEmpty());
    }

    // ===========================================
    // Finding by availability
    // ===========================================

    @Test
    void shouldFindProductsAvailableAtDate() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        ProductType phone = thereIsProduct("Phone");
        thereIsCatalogEntryWithValidity(
                laptop, "2024 Laptop", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        thereIsCatalogEntryWithValidity(phone, "Always Phone", null, null);

        // when
        Set<CatalogEntryView> inJune2024 =
                catalog.findBy(new FindAvailableAtCriteria(LocalDate.of(2024, 6, 15)));
        Set<CatalogEntryView> inJune2025 =
                catalog.findBy(new FindAvailableAtCriteria(LocalDate.of(2025, 6, 15)));

        // then
        assertEquals(2, inJune2024.size());
        assertEquals(1, inJune2025.size());
        assertTrue(inJune2025.stream().anyMatch(e -> e.displayName().equals("Always Phone")));
    }

    @Test
    void shouldNotFindDiscontinuedProducts() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        CatalogEntryId entryId =
                thereIsCatalogEntryWithValidity(
                        laptop, "Old Laptop", LocalDate.of(2020, 1, 1), null);
        catalog.handle(new DiscontinueProduct(entryId.value(), LocalDate.of(2023, 12, 31)));

        // when
        Set<CatalogEntryView> in2024 =
                catalog.findBy(new FindAvailableAtCriteria(LocalDate.of(2024, 6, 15)));
        Set<CatalogEntryView> in2023 =
                catalog.findBy(new FindAvailableAtCriteria(LocalDate.of(2023, 6, 15)));

        // then
        assertTrue(in2024.isEmpty());
        assertEquals(1, in2023.size());
    }

    // ===========================================
    // Finding by metadata
    // ===========================================

    @Test
    void shouldFindProductsByMetadataKeyAndValue() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        ProductType phone = thereIsProduct("Phone");
        thereIsCatalogEntryWithMetadata(
                laptop, "Featured Laptop", Map.of("featured", "true", "brand", "Dell"));
        thereIsCatalogEntryWithMetadata(
                phone, "Regular Phone", Map.of("featured", "false", "brand", "Samsung"));

        // when
        Set<CatalogEntryView> featured =
                catalog.findBy(new FindByMetadataCriteria("featured", "true"));
        Set<CatalogEntryView> dell = catalog.findBy(new FindByMetadataCriteria("brand", "Dell"));

        // then
        assertEquals(1, featured.size());
        assertTrue(featured.stream().anyMatch(e -> e.displayName().equals("Featured Laptop")));
        assertEquals(1, dell.size());
    }

    @Test
    void shouldFindProductsByMetadataKeyOnly() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        ProductType phone = thereIsProduct("Phone");
        thereIsCatalogEntryWithMetadata(laptop, "Laptop with brand", Map.of("brand", "Dell"));
        thereIsCatalogEntryWithMetadata(phone, "Phone without brand", Map.of());

        // when
        Set<CatalogEntryView> withBrand = catalog.findBy(new FindByMetadataCriteria("brand", null));

        // then
        assertEquals(1, withBrand.size());
        assertTrue(withBrand.stream().anyMatch(e -> e.displayName().equals("Laptop with brand")));
    }

    // ===========================================
    // Searching catalog
    // ===========================================

    @Test
    void shouldSearchByTextInDisplayName() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        ProductType phone = thereIsProduct("Phone");
        thereIsCatalogEntry(laptop, "Gaming Laptop Pro", Set.of());
        thereIsCatalogEntry(phone, "Budget Smartphone", Set.of());

        // when
        Set<CatalogEntryView> laptopResults =
                catalog.findBy(SearchCatalogCriteria.byText("Laptop"));
        Set<CatalogEntryView> smartphoneResults =
                catalog.findBy(SearchCatalogCriteria.byText("Smartphone"));

        // then
        assertEquals(1, laptopResults.size());
        assertEquals(1, smartphoneResults.size());
    }

    @Test
    void shouldSearchByTextCaseInsensitive() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        thereIsCatalogEntry(laptop, "Gaming Laptop", Set.of());

        // when
        Set<CatalogEntryView> upperCase = catalog.findBy(SearchCatalogCriteria.byText("GAMING"));
        Set<CatalogEntryView> lowerCase = catalog.findBy(SearchCatalogCriteria.byText("gaming"));

        // then
        assertEquals(1, upperCase.size());
        assertEquals(1, lowerCase.size());
    }

    @Test
    void shouldReturnAllEntriesWhenNoFilters() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        ProductType phone = thereIsProduct("Phone");
        thereIsCatalogEntry(laptop, "Laptop", Set.of());
        thereIsCatalogEntry(phone, "Phone", Set.of());

        // when
        Set<CatalogEntryView> all = catalog.findBy(SearchCatalogCriteria.all());

        // then
        assertEquals(2, all.size());
    }

    // ===========================================
    // Discontinuing products
    // ===========================================

    @Test
    void shouldDiscontinueProduct() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        CatalogEntryId entryId =
                thereIsCatalogEntryWithValidity(
                        laptop, "Old Laptop", LocalDate.of(2020, 1, 1), null);

        // when
        Result<String, CatalogEntryId> result =
                catalog.handle(new DiscontinueProduct(entryId.value(), LocalDate.of(2024, 6, 30)));

        // then
        assertTrue(result.success());
        CatalogEntryView updated =
                catalog.findBy(new FindCatalogEntryCriteria(entryId.value())).orElseThrow();
        assertEquals(LocalDate.of(2024, 6, 30), updated.availableUntil());
    }

    @Test
    void shouldFailToDiscontinueNonExistentEntry() {
        // when
        Result<String, CatalogEntryId> result =
                catalog.handle(
                        new DiscontinueProduct("non-existent-id", LocalDate.of(2024, 6, 30)));

        // then
        assertTrue(result.failure());
    }

    // ===========================================
    // Updating metadata
    // ===========================================

    @Test
    void shouldUpdateMetadata() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        CatalogEntryId entryId =
                thereIsCatalogEntryWithMetadata(laptop, "Laptop", Map.of("featured", "false"));

        // when
        Result<String, CatalogEntryId> result =
                catalog.handle(
                        new UpdateMetadata(
                                entryId.value(), Map.of("featured", "true", "badge", "sale")));

        // then
        assertTrue(result.success());
        CatalogEntryView updated =
                catalog.findBy(new FindCatalogEntryCriteria(entryId.value())).orElseThrow();
        assertEquals("true", updated.metadata().get("featured"));
        assertEquals("sale", updated.metadata().get("badge"));
    }

    @Test
    void shouldFailToUpdateMetadataForNonExistentEntry() {
        // when
        Result<String, CatalogEntryId> result =
                catalog.handle(new UpdateMetadata("non-existent-id", Map.of("featured", "true")));

        // then
        assertTrue(result.failure());
    }

    // ===========================================
    // View correctness
    // ===========================================

    @Test
    void shouldReturnCorrectViewFields() {
        // given
        ProductType laptop = thereIsProduct("Laptop");
        Result<String, CatalogEntryId> result =
                catalog.handle(
                        new AddToOffer(
                                laptop.id().toString(),
                                "Test Display Name",
                                "Test Description",
                                Set.of("cat1", "cat2"),
                                LocalDate.of(2024, 1, 1),
                                LocalDate.of(2024, 12, 31),
                                Map.of("key1", "value1")));

        // when
        Optional<CatalogEntryView> found =
                catalog.findBy(new FindCatalogEntryCriteria(result.getSuccess().value()));

        // then
        assertTrue(found.isPresent());
        CatalogEntryView view = found.get();
        assertEquals(result.getSuccess().value(), view.catalogEntryId());
        assertEquals("Test Display Name", view.displayName());
        assertEquals("Test Description", view.description());
        assertEquals(laptop.id().toString(), view.productTypeId());
        assertEquals(Set.of("cat1", "cat2"), view.categories());
        assertEquals(LocalDate.of(2024, 1, 1), view.availableFrom());
        assertEquals(LocalDate.of(2024, 12, 31), view.availableUntil());
        assertEquals(Map.of("key1", "value1"), view.metadata());
    }

    // ===========================================
    // Helper methods
    // ===========================================

    private ProductType thereIsProduct(String name) {
        ProductType productType =
                ProductType.define(
                        UuidProductIdentifier.random(),
                        ProductName.of(name),
                        ProductDescription.of("Description of " + name));
        productTypeRepository.save(productType);
        return productType;
    }

    private CatalogEntryId thereIsCatalogEntry(
            ProductType product, String displayName, Set<String> categories) {
        Result<String, CatalogEntryId> result =
                catalog.handle(
                        new AddToOffer(
                                product.id().toString(),
                                displayName,
                                "Description of " + displayName,
                                categories,
                                null,
                                null,
                                Map.of()));
        return result.getSuccess();
    }

    private CatalogEntryId thereIsCatalogEntryWithValidity(
            ProductType product, String displayName, LocalDate from, LocalDate to) {
        Result<String, CatalogEntryId> result =
                catalog.handle(
                        new AddToOffer(
                                product.id().toString(),
                                displayName,
                                "Description of " + displayName,
                                Set.of(),
                                from,
                                to,
                                Map.of()));
        return result.getSuccess();
    }

    private CatalogEntryId thereIsCatalogEntryWithMetadata(
            ProductType product, String displayName, Map<String, String> metadata) {
        Result<String, CatalogEntryId> result =
                catalog.handle(
                        new AddToOffer(
                                product.id().toString(),
                                displayName,
                                "Description of " + displayName,
                                Set.of(),
                                null,
                                null,
                                metadata));
        return result.getSuccess();
    }
}
