package com.softwarearchetypes.rules.discounting.config.reflection;

import com.softwarearchetypes.rules.discounting.OfferItemModifier;
import com.softwarearchetypes.rules.discounting.client.ClientContext;
import com.softwarearchetypes.rules.discounting.config.ConfigProvider;
import com.softwarearchetypes.rules.discounting.config.DiscountRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReflectionDynamicConfig implements ConfigProvider {

    private final DiscountRepository repository;

    public ReflectionDynamicConfig(DiscountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Map<OfferItemModifier, Predicate<ClientContext>> loadConfig() {
        Map<OfferItemModifier, Predicate<ClientContext>> config = new HashMap<>();

        for (Discount discount : repository.findAllDiscounts()) {
            List<DiscountParam> paramsList = repository.findParamsByDiscountId(discount.id());
            Map<String, String> params = toParamMap(paramsList);

            ReflectionBeanReader beanReader = new ReflectionBeanReader(params);

            OfferItemModifier modifier =
                    beanReader.readBean(Config.MODIFIER_PREFIX, OfferItemModifier.class);

            Predicate<ClientContext> clientPredicate =
                    beanReader.readBean(Config.CLIENT_PREDICATE_PREFIX, Predicate.class);

            config.put(modifier, clientPredicate);
        }

        return config;
    }

    private Map<String, String> toParamMap(List<DiscountParam> paramsList) {
        return paramsList.stream()
                .collect(Collectors.toMap(DiscountParam::paramName, DiscountParam::paramValue));
    }
}
