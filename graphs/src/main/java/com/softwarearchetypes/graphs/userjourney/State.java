package com.softwarearchetypes.graphs.userjourney;

import java.util.HashSet;
import java.util.Set;

record State(Set<Product> products) {

    static State empty() {
        return new State(Set.of());
    }

    static State of(Product... products) {
        return new State(Set.of(products));
    }

    State withProduct(Product product) {
        Set<Product> newProducts = new HashSet<>(this.products);
        newProducts.add(product);
        return new State(newProducts);
    }

    boolean contains(Product.ProductType productType) {
        return products().stream().anyMatch(product -> product.type() == productType);
    }
}
