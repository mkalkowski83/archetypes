package com.softwarearchetypes.inventory;

import com.softwarearchetypes.common.Result;
import java.util.Map;

/**
 * Validates instance data against product definition.
 *
 * <p>This interface is intended to be implemented by calling the Product archetype to verify that:
 * - The product identifier exists - The tracking strategy matches the product definition - The
 * features are valid for this product type - Required features are provided - Feature values match
 * the allowed values/ranges defined in the product type
 *
 * <p>Example implementation:
 *
 * <pre>
 * class ProductArchetypeValidator implements ProductDefinitionValidator {
 *     private final ProductFacade productFacade;
 *
 *     @Override
 *     public Result<String, Void> validate(ProductIdentifier productId,
 *                                          ProductTrackingStrategy strategy,
 *                                          Map<String, String> features) {
 *         // Call product archetype to validate
 *         return productFacade.validateInstanceData(productId, strategy, features);
 *     }
 * }
 * </pre>
 */
interface ProductDefinitionValidator {

    /**
     * Validates instance data against the product definition.
     *
     * @param productId the product identifier
     * @param strategy the tracking strategy being used
     * @param features the feature values for this instance
     * @return success if valid, failure with error message if invalid
     */
    Result<String, Void> validate(
            ProductIdentifier productId,
            ProductTrackingStrategy strategy,
            Map<String, String> features);

    /**
     * A permissive validator that accepts all data. Useful for testing or when product validation
     * is not required.
     */
    static ProductDefinitionValidator alwaysValid() {
        return (productId, strategy, features) -> Result.success(null);
    }
}
