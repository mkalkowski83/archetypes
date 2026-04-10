package com.softwarearchetypes.product;

class ProductConfiguration {

    private final ProductFacade productFacade;
    private final ProductRelationshipsFacade productRelationshipsFacade;
    private final ProductTypeRepository productTypeRepository;
    private final ProductCatalog productCatalog;
    private final CatalogEntryRepository catalogEntryRepository;

    ProductConfiguration(
            ProductFacade productFacade,
            ProductRelationshipsFacade productRelationshipsFacade,
            ProductTypeRepository productTypeRepository,
            ProductCatalog productCatalog,
            CatalogEntryRepository catalogEntryRepository) {
        this.productFacade = productFacade;
        this.productRelationshipsFacade = productRelationshipsFacade;
        this.productTypeRepository = productTypeRepository;
        this.productCatalog = productCatalog;
        this.catalogEntryRepository = catalogEntryRepository;
    }

    public static ProductConfiguration inMemory() {
        InMemoryProductTypeRepository productTypeRepository = new InMemoryProductTypeRepository();
        ProductFacade facade = new ProductFacade(productTypeRepository);

        InMemoryProductRelationshipRepository productRelationshipRepository =
                new InMemoryProductRelationshipRepository();
        ProductRelationshipFactory productRelationshipFactory =
                new ProductRelationshipFactory(ProductRelationshipId::random);
        ProductRelationshipsFacade productRelationshipsFacade =
                new ProductRelationshipsFacade(
                        productRelationshipFactory,
                        productRelationshipRepository,
                        productTypeRepository);

        InMemoryCatalogEntryRepository catalogEntryRepository =
                new InMemoryCatalogEntryRepository();
        ProductCatalog productCatalog =
                new ProductCatalog(catalogEntryRepository, productTypeRepository);

        return new ProductConfiguration(
                facade,
                productRelationshipsFacade,
                productTypeRepository,
                productCatalog,
                catalogEntryRepository);
    }

    public ProductFacade productFacade() {
        return productFacade;
    }

    public ProductRelationshipsFacade productRelationshipsFacade() {
        return productRelationshipsFacade;
    }

    public ProductTypeRepository productTypeRepository() {
        return productTypeRepository;
    }

    public ProductCatalog productCatalog() {
        return productCatalog;
    }

    public CatalogEntryRepository catalogEntryRepository() {
        return catalogEntryRepository;
    }
}
