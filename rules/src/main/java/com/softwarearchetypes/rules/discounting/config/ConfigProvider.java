package com.softwarearchetypes.rules.discounting.config;

import com.softwarearchetypes.rules.discounting.OfferItemModifier;
import com.softwarearchetypes.rules.discounting.client.ClientContext;
import java.util.Map;
import java.util.function.Predicate;

public interface ConfigProvider {
    Map<OfferItemModifier, Predicate<ClientContext>> loadConfig();
}
