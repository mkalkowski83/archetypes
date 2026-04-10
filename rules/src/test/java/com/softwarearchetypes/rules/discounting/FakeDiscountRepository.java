package com.softwarearchetypes.rules.discounting;

import com.softwarearchetypes.rules.discounting.config.DiscountRepository;
import com.softwarearchetypes.rules.discounting.config.reflection.Discount;
import com.softwarearchetypes.rules.discounting.config.reflection.DiscountParam;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FakeDiscountRepository implements DiscountRepository {
    private List<Discount> discounts = new ArrayList<>();
    private List<DiscountParam> params = new ArrayList<>();

    @Override
    public List<Discount> findAllDiscounts() {
        return discounts;
    }

    @Override
    public List<DiscountParam> findParamsByDiscountId(UUID id) {
        return params.stream().filter(p -> Objects.equals(p.discountId(), id)).toList();
    }

    @Override
    public UUID insert(Discount discount) {
        Discount d = new Discount(UUID.randomUUID(), discount.name());
        discounts.add(d);
        return d.id();
    }

    @Override
    public void insertParam(DiscountParam discountParam) {
        params.add(discountParam);
    }
}
