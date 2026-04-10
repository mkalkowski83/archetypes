package com.softwarearchetypes.party;

import static com.softwarearchetypes.party.CommandFixture.someRegisterCompanyCommand;
import static com.softwarearchetypes.party.CommandFixture.someRegisterPersonCommand;
import static com.softwarearchetypes.party.GeoAddressFixture.geoAddressDTOWith;
import static com.softwarearchetypes.party.GeoAddressFixture.geoAddressDTOWithLocale;
import static com.softwarearchetypes.party.GeoAddressFixture.someGeoAddressDTOWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.party.commands.AddOrUpdateGeoAddressCommand;
import com.softwarearchetypes.party.commands.RemoveAddressCommand;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Scenarios for Addresses as a separate aggregate. Addresses (Geo, Email, Phone, Web) are managed
 * independently from Party.
 */
@DisplayName("Addresses Scenarios")
class AddressesScenariosTest {

    private final PartyConfiguration configuration = PartyConfiguration.inMemory();
    private final PartiesFacade partiesFacade = configuration.partiesFacade();
    private final AddressesFacade addressesFacade = configuration.addressesFacade();
    private final AddressesQueries addressesQueries = configuration.addressesQueries();

    // ===== Basic address operations =====

    @Nested
    @DisplayName("Basic GeoAddress operations")
    class BasicGeoAddressScenarios {

        @Test
        @DisplayName("Person can have a residential address")
        void personCanHaveResidentialAddress() {
            PartyId person = registerPerson();

            Result<String, AddressId> result =
                    addressesFacade.handle(
                            new AddOrUpdateGeoAddressCommand(
                                    person,
                                    geoAddressDTOWith(
                                            person, "Home", "Warsaw", AddressUseType.RESIDENTIAL)));

            assertTrue(result.success());
            List<AddressView> addresses = addressesQueries.findAllFor(person);
            assertEquals(1, addresses.size());
        }

        @Test
        @DisplayName("Company can have a business address")
        void companyCanHaveBusinessAddress() {
            PartyId company = registerCompany();

            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            company,
                            geoAddressDTOWith(
                                    company,
                                    "Headquarters",
                                    "Warsaw",
                                    AddressUseType.BILLING,
                                    AddressUseType.MAILING)));

            List<AddressView> addresses = addressesQueries.findAllFor(company);
            assertEquals(1, addresses.size());
        }

        @Test
        @DisplayName("Address can be updated")
        void addressCanBeUpdated() {
            PartyId person = registerPerson();
            AddressId addressId = AddressId.random();

            // Add initial address
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            person,
                            someGeoAddressDTOWithId(
                                    addressId, person, AddressUseType.RESIDENTIAL)));

            // Update same address (same addressId)
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            person,
                            someGeoAddressDTOWithId(
                                    addressId, person, AddressUseType.RESIDENTIAL)));

            List<AddressView> addresses = addressesQueries.findAllFor(person);
            assertEquals(1, addresses.size()); // Still one address, updated
        }

        @Test
        @DisplayName("Address can be removed")
        void addressCanBeRemoved() {
            PartyId person = registerPerson();
            AddressId addressId = AddressId.random();

            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            person,
                            someGeoAddressDTOWithId(
                                    addressId, person, AddressUseType.RESIDENTIAL)));

            // Remove address
            Result<String, AddressId> result =
                    addressesFacade.handle(new RemoveAddressCommand(person, addressId));

            assertTrue(result.success());
            List<AddressView> addresses = addressesQueries.findAllFor(person);
            assertTrue(addresses.isEmpty());
        }
    }

    // ===== Multiple addresses =====

    @Nested
    @DisplayName("Multiple addresses per party")
    class MultipleAddressesScenarios {

        @Test
        @DisplayName("Person can have multiple addresses with different use types")
        void personCanHaveMultipleAddresses() {
            PartyId person = registerPerson();

            // Home address
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            person,
                            geoAddressDTOWith(
                                    person, "Home", "Warsaw", AddressUseType.RESIDENTIAL)));

            // Work address
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            person,
                            geoAddressDTOWith(person, "Work", "Warsaw", AddressUseType.MAILING)));

            // Vacation address
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            person,
                            geoAddressDTOWith(
                                    person, "Vacation", "Sopot", AddressUseType.CONTACT)));

            List<AddressView> addresses = addressesQueries.findAllFor(person);
            assertEquals(3, addresses.size());
        }

        @Test
        @DisplayName("Company can have headquarters and branch offices")
        void companyCanHaveHeadquartersAndBranches() {
            PartyId company = registerCompany();

            // Headquarters - billing and mailing
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            company,
                            geoAddressDTOWith(
                                    company,
                                    "Headquarters",
                                    "Warsaw",
                                    AddressUseType.BILLING,
                                    AddressUseType.MAILING)));

            // Warsaw branch - contact
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            company,
                            geoAddressDTOWith(
                                    company, "Warsaw Branch", "Warsaw", AddressUseType.CONTACT)));

            // Cracow branch - shipping (warehouse)
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            company,
                            geoAddressDTOWith(
                                    company, "Cracow Branch", "Cracow", AddressUseType.SHIPPING)));

            // Gdańsk branch - residential (for legal purposes)
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            company,
                            geoAddressDTOWith(
                                    company,
                                    "Gdańsk Branch",
                                    "Gdańsk",
                                    AddressUseType.RESIDENTIAL)));

            List<AddressView> addresses = addressesQueries.findAllFor(company);
            assertEquals(4, addresses.size());
        }
    }

    // ===== Address use types =====

    @Nested
    @DisplayName("Address use types")
    class AddressUseTypesScenarios {

        @Test
        @DisplayName("Address can have multiple use types")
        void addressCanHaveMultipleUseTypes() {
            PartyId person = registerPerson();

            // Home office - used for everything
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            person,
                            geoAddressDTOWith(
                                    person,
                                    "Home Office",
                                    "Warsaw",
                                    AddressUseType.RESIDENTIAL,
                                    AddressUseType.BILLING,
                                    AddressUseType.MAILING,
                                    AddressUseType.SHIPPING,
                                    AddressUseType.CONTACT)));

            List<AddressView> addresses = addressesQueries.findAllFor(person);
            assertEquals(1, addresses.size());
        }

        @Test
        @DisplayName("Different addresses for billing and shipping")
        void differentAddressesForBillingAndShipping() {
            PartyId customer = registerPerson();

            // Billing address (registered company address)
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            customer,
                            geoAddressDTOWith(
                                    customer, "Billing", "Warsaw", AddressUseType.BILLING)));

            // Shipping address (home)
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            customer,
                            geoAddressDTOWith(
                                    customer,
                                    "Shipping",
                                    "Warsaw",
                                    AddressUseType.SHIPPING,
                                    AddressUseType.RESIDENTIAL)));

            List<AddressView> addresses = addressesQueries.findAllFor(customer);
            assertEquals(2, addresses.size());
        }
    }

    // ===== E-commerce scenarios =====

    @Nested
    @DisplayName("E-commerce: Customer addresses")
    class EcommerceScenarios {

        @Test
        @DisplayName("Customer with multiple address types")
        void customerWithMultipleAddressTypes() {
            PartyId customer = registerPerson();

            // Home delivery - primary shipping address
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            customer,
                            geoAddressDTOWith(
                                    customer,
                                    "Home Delivery",
                                    "Warsaw",
                                    AddressUseType.SHIPPING,
                                    AddressUseType.RESIDENTIAL)));

            // Work address - for contact during work hours
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            customer,
                            geoAddressDTOWith(
                                    customer, "Work Address", "Warsaw", AddressUseType.CONTACT)));

            // Billing address
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            customer,
                            geoAddressDTOWith(
                                    customer, "Billing", "Warsaw", AddressUseType.BILLING)));

            List<AddressView> addresses = addressesQueries.findAllFor(customer);
            assertEquals(3, addresses.size());
        }

        @Test
        @DisplayName("Business customer with invoice and delivery addresses")
        void businessCustomerWithInvoiceAndDeliveryAddresses() {
            PartyId company = registerCompany();

            // Invoice/billing address (official registered address)
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            company,
                            geoAddressDTOWith(
                                    company, "Invoice Address", "Warsaw", AddressUseType.BILLING)));

            // Warehouse delivery
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            company,
                            geoAddressDTOWith(
                                    company, "Warehouse", "Łódź", AddressUseType.SHIPPING)));

            List<AddressView> addresses = addressesQueries.findAllFor(company);
            assertEquals(2, addresses.size());
        }
    }

    // ===== Banking/Financial scenarios =====

    @Nested
    @DisplayName("Banking: Customer address management")
    class BankingScenarios {

        @Test
        @DisplayName("Bank customer with correspondence and residential addresses")
        void bankCustomerWithCorrespondenceAddress() {
            PartyId customer = registerPerson();

            // Residential address (for KYC/verification)
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            customer,
                            geoAddressDTOWith(
                                    customer,
                                    "Residential",
                                    "Warsaw",
                                    AddressUseType.RESIDENTIAL)));

            // Correspondence address (for statements, cards)
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            customer,
                            geoAddressDTOWith(
                                    customer, "Correspondence", "Warsaw", AddressUseType.MAILING)));

            List<AddressView> addresses = addressesQueries.findAllFor(customer);
            assertEquals(2, addresses.size());
        }
    }

    // ===== International addresses =====

    @Nested
    @DisplayName("International addresses")
    class InternationalAddressesScenarios {

        @Test
        @DisplayName("Person with addresses in different countries")
        void personWithAddressesInDifferentCountries() {
            PartyId expat = registerPerson();

            // Poland address
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            expat,
                            geoAddressDTOWithLocale(
                                    expat,
                                    "Poland Home",
                                    "Warsaw",
                                    Locale.forLanguageTag("pl-PL"),
                                    AddressUseType.RESIDENTIAL)));

            // Germany address
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            expat,
                            geoAddressDTOWithLocale(
                                    expat,
                                    "Germany Office",
                                    "Berlin",
                                    Locale.GERMANY,
                                    AddressUseType.MAILING,
                                    AddressUseType.CONTACT)));

            // UK address
            addressesFacade.handle(
                    new AddOrUpdateGeoAddressCommand(
                            expat,
                            geoAddressDTOWithLocale(
                                    expat,
                                    "UK Branch",
                                    "London",
                                    Locale.UK,
                                    AddressUseType.BILLING)));

            List<AddressView> addresses = addressesQueries.findAllFor(expat);
            assertEquals(3, addresses.size());
        }
    }

    // ===== Helper methods =====

    private PartyId registerPerson() {
        return partiesFacade.handle(someRegisterPersonCommand()).getSuccess().partyId();
    }

    private PartyId registerCompany() {
        return partiesFacade.handle(someRegisterCompanyCommand()).getSuccess().partyId();
    }
}
