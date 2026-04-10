package com.softwarearchetypes.rules.discounting;

import com.softwarearchetypes.quantity.money.Percentage;
import com.softwarearchetypes.rules.discounting.client.ClientContext;
import com.softwarearchetypes.rules.discounting.client.ClientContextRepository;
import com.softwarearchetypes.rules.discounting.client.ClientStatus;
import com.softwarearchetypes.rules.discounting.config.ConfigProvider;
import com.softwarearchetypes.rules.discounting.offer.modifiers.ChainOfferItemModifier;
import com.softwarearchetypes.rules.discounting.offer.modifiers.EmptyModifier;
import com.softwarearchetypes.rules.discounting.offer.modifiers.simple.PercentageOfferItemModifier;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class OfferItemModifierFactory {

    private final ClientContextRepository clientContextRepository;
    private final ConfigProvider configProvider;

    public OfferItemModifierFactory(
            ClientContextRepository clientContextRepository, ConfigProvider configProvider) {
        this.clientContextRepository = clientContextRepository;
        this.configProvider = configProvider;
    }

    public OfferItemModifier createDiscountModifier(ClientStatus status) {
        switch (status) {
            case STANDARD:
                return new PercentageOfferItemModifier("My friend", Percentage.ofFraction(0.05));
            case VIP:
                return new PercentageOfferItemModifier("VIP", Percentage.ofFraction(0.15));
            case GOLD:
                return new PercentageOfferItemModifier("Gold", Percentage.of(25));
            default:
                return new EmptyModifier();
        }
    }

    public OfferItemModifier createDiscountModifier2(ClientStatus status) {
        return status.accept(new OfferItemModifierVisitor());
    }

    public OfferItemModifier createDiscountModifier3(UUID clientId) {
        Map<OfferItemModifier, Predicate<ClientContext>> configuration =
                configProvider.loadConfig();
        ClientContext clientContext = clientContextRepository.loadClientContext(clientId);
        ChainOfferItemModifier modifier = new ChainOfferItemModifier();

        configuration.entrySet().stream()
                .filter(entry -> entry.getValue().test(clientContext))
                .map(Map.Entry::getKey)
                .forEach(modifier::add);

        return modifier;
    }
}
