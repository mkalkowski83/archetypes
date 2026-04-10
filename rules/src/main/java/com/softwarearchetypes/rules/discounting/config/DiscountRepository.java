package com.softwarearchetypes.rules.discounting.config;

import com.softwarearchetypes.rules.discounting.config.reflection.Discount;
import com.softwarearchetypes.rules.discounting.config.reflection.DiscountParam;
import java.util.List;
import java.util.UUID;

public interface DiscountRepository {
    List<Discount> findAllDiscounts();

    List<DiscountParam> findParamsByDiscountId(UUID id);

    UUID insert(Discount discount);

    void insertParam(DiscountParam discountParam);
}
