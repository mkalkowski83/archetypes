package com.softwarearchetypes.product;

import static com.softwarearchetypes.common.Preconditions.checkArgument;
import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Set;

/**
 * ProductSet represents a named collection of products available for selection in a package. This
 * is the "raw material" - a pool of product options without any selection constraints.
 *
 * <p>Example: A laptop package might have ProductSets like "Memory Options" (4GB, 8GB, 16GB),
 * "Storage Options" (256GB SSD, 512GB SSD, 1TB SSD), "Accessories" (mouse, keyboard, bag).
 */
class ProductSet {
    private final String name;
    private final Set<ProductIdentifier> products;

    ProductSet(String name, Set<ProductIdentifier> products) {
        checkArgument(name != null && !name.isBlank(), "ProductSet name must be defined");
        checkArgument(
                products != null && !products.isEmpty(),
                "ProductSet must contain at least one product");
        this.name = name;
        this.products = Set.copyOf(products);
    }

    static ProductSet singleOf(String name, ProductIdentifier id) {
        return new ProductSet(name, Set.of(id));
    }

    static ProductSet of(String name, ProductIdentifier... ids) {
        return new ProductSet(name, Arrays.stream(ids).collect(toSet()));
    }

    String name() {
        return name;
    }

    Set<ProductIdentifier> products() {
        return products;
    }

    boolean contains(ProductIdentifier productId) {
        return products.contains(productId);
    }

    @Override
    public String toString() {
        return "ProductSet{name='%s', products=%s}".formatted(name, products);
    }
}
