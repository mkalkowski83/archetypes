package com.softwarearchetypes.product;

import com.softwarearchetypes.common.Result;
import java.util.function.Supplier;

class ProductRelationshipFactory {

    private static final ProductRelationshipDefiningPolicy DEFAULT_POLICY =
            new AlwaysAllowProductRelationshipDefiningPolicy();
    private final ProductRelationshipDefiningPolicy policy;
    private final Supplier<ProductRelationshipId> idSupplier;

    ProductRelationshipFactory(
            ProductRelationshipDefiningPolicy policy, Supplier<ProductRelationshipId> idSupplier) {
        this.policy = policy != null ? policy : DEFAULT_POLICY;
        this.idSupplier = idSupplier != null ? idSupplier : ProductRelationshipId::random;
    }

    ProductRelationshipFactory(Supplier<ProductRelationshipId> idSupplier) {
        this(null, idSupplier);
    }

    Result<String, ProductRelationship> defineFor(
            ProductIdentifier from, ProductIdentifier to, ProductRelationshipType type) {
        if (policy.canDefineFor(from, to, type)) {
            return Result.success(ProductRelationship.of(idSupplier.get(), from, to, type));
        } else {
            return Result.failure("POLICIES_NOT_MET");
        }
    }
}
