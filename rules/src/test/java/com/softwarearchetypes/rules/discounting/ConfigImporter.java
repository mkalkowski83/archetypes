package com.softwarearchetypes.rules.discounting;

import com.softwarearchetypes.rules.discounting.client.ClientContext;
import com.softwarearchetypes.rules.discounting.config.DiscountRepository;
import com.softwarearchetypes.rules.discounting.config.reflection.Config;
import com.softwarearchetypes.rules.discounting.config.reflection.Discount;
import com.softwarearchetypes.rules.discounting.config.reflection.DiscountParam;
import com.softwarearchetypes.rules.discounting.config.reflection.ReflectionBeanWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

public class ConfigImporter {

    private final DiscountRepository discountRepository;
    private final ReflectionBeanWriter beanWriter = new ReflectionBeanWriter();

    public ConfigImporter(DiscountRepository discountRepository) {
        this.discountRepository = discountRepository;
    }

    public void importConfig(Map<OfferItemModifier, Predicate<ClientContext>> configMap) {
        for (Map.Entry<OfferItemModifier, Predicate<ClientContext>> entry : configMap.entrySet()) {
            OfferItemModifier modifier = entry.getKey();

            Map<String, String> params = new HashMap<>();
            beanWriter.writeBean(Config.MODIFIER_PREFIX, modifier, params);

            String name = humanReadableName(modifier);

            UUID discountId = discountRepository.insert(new Discount(null, name));
            for (Map.Entry<String, String> p : params.entrySet()) {
                discountRepository.insertParam(
                        new DiscountParam(discountId, p.getKey(), p.getValue()));
            }

            params.clear();
            Predicate<ClientContext> clientPredicate = entry.getValue();

            beanWriter.writeBean(Config.CLIENT_PREDICATE_PREFIX, clientPredicate, params);
            for (Map.Entry<String, String> p : params.entrySet()) {
                discountRepository.insertParam(
                        new DiscountParam(discountId, p.getKey(), p.getValue()));
            }
        }
    }

    private String humanReadableName(OfferItemModifier modifier) {
        try {
            Method m = modifier.getClass().getMethod("getName");
            Object result = m.invoke(modifier);
            if (result != null) {
                return result.toString();
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return modifier.getClass().getSimpleName();
    }
}
