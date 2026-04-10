package com.softwarearchetypes.rules.discounting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softwarearchetypes.quantity.Quantity;
import com.softwarearchetypes.quantity.Unit;
import com.softwarearchetypes.quantity.money.Money;
import com.softwarearchetypes.rules.discounting.client.ClientContext;
import com.softwarearchetypes.rules.discounting.client.ClientContextRepository;
import com.softwarearchetypes.rules.discounting.client.ClientStatus;
import com.softwarearchetypes.rules.discounting.config.DiscountRepository;
import com.softwarearchetypes.rules.discounting.config.SampleStaticConfig;
import com.softwarearchetypes.rules.discounting.config.reflection.ReflectionDynamicConfig;
import com.softwarearchetypes.rules.discounting.offer.OfferItem;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class OfferItemModifierFactoryTest {
    private ClientContextRepository clientContextRepository =
            new ClientContextRepository() {
                @Override
                public ClientContext loadClientContext(UUID clientId) {
                    return new ClientContext(
                            clientId,
                            ClientStatus.VIP,
                            Money.pln(1000000),
                            LocalDate.of(2020, 1, 1));
                }
            };

    private DiscountRepository discountRepository = new FakeDiscountRepository();

    @Test
    public void testVipDiscount() {
        OfferItem item = anyItemPriced(100);
        OfferItem modified =
                new OfferItemModifierFactory(null, null)
                        .createDiscountModifier2(ClientStatus.VIP)
                        .modify(item);
        assertEquals(anyItemPriced(85).getFinalPrice(), modified.getFinalPrice());
    }

    @Test
    public void testConfig() {
        OfferItemModifierFactory factory =
                new OfferItemModifierFactory(clientContextRepository, new SampleStaticConfig());

        OfferItemModifier modifier = factory.createDiscountModifier3(UUID.randomUUID());
        var modified = modifier.modify(anyItemPriced(100));

        assertEquals(anyItemPriced(80).getFinalPrice(), modified.getFinalPrice());
    }

    @Test
    public void testReflectionDynamicConfig() {
        SampleStaticConfig config = new SampleStaticConfig();
        var configMap = config.loadConfig();
        // save to DB
        ConfigImporter importer = new ConfigImporter(discountRepository);
        importer.importConfig(configMap);

        OfferItemModifierFactory factory =
                new OfferItemModifierFactory(
                        clientContextRepository, new ReflectionDynamicConfig(discountRepository));
        OfferItemModifier modifier = factory.createDiscountModifier3(UUID.randomUUID());

        var modified = modifier.modify(anyItemPriced(100));
        assertEquals(anyItemPriced(80).getFinalPrice(), modified.getFinalPrice());
    }

    private OfferItem anyItemPriced(double amount) {
        return new OfferItem(
                UUID.randomUUID(), Quantity.of(1, Unit.kilograms()), Money.pln(amount));
    }
}
