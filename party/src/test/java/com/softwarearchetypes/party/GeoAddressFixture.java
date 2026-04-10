package com.softwarearchetypes.party;

import static com.softwarearchetypes.common.RandomFixture.randomElementOf;
import static com.softwarearchetypes.common.RandomFixture.randomStringWithPrefixOf;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

import com.softwarearchetypes.party.commands.GeoAddressDTO;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class GeoAddressFixture {

    static GeoAddress someGeoAddressFor(PartyId partyId) {
        return someGeoAddressWith(AddressId.random(), partyId);
    }

    static GeoAddress someGeoAddressFor(PartyId partyId, String city) {
        return new GeoAddress(
                AddressId.random(),
                partyId,
                GeoAddress.GeoAddressDetails.from(
                        someName(),
                        someStreet(),
                        someBuilding(),
                        someFlat(),
                        city,
                        someZipCode(),
                        someLocale()),
                someUseTypes());
    }

    static GeoAddress someGeoAddressWith(AddressId addressId, PartyId partyId) {
        return new GeoAddress(addressId, partyId, someGeoAddressDetails(), someUseTypes());
    }

    static GeoAddress.GeoAddressDetails someGeoAddressDetails() {
        return GeoAddress.GeoAddressDetails.from(
                someName(),
                someStreet(),
                someBuilding(),
                someFlat(),
                someCity(),
                someZipCode(),
                someLocale());
    }

    static String someName() {
        return randomStringWithPrefixOf("name");
    }

    static String someStreet() {
        return randomStringWithPrefixOf("street");
    }

    static String someBuilding() {
        return randomNumeric(3);
    }

    static String someFlat() {
        return randomNumeric(3);
    }

    static String someCity() {
        return randomStringWithPrefixOf("city");
    }

    static ZipCode someZipCode() {
        return ZipCode.of(randomNumeric(2) + "-" + randomNumeric(3));
    }

    static Locale someLocale() {
        return Locale.getDefault();
    }

    static AddressUseType someUseType() {
        return randomElementOf(List.of(AddressUseType.values()));
    }

    static Set<AddressUseType> someUseTypes() {
        Set<AddressUseType> useTypes = new HashSet<>();
        useTypes.add(someUseType());
        useTypes.add(someUseType());
        useTypes.add(someUseType());
        return useTypes;
    }

    static Set<AddressUseType> someUseTypesDifferentThan(Set<AddressUseType> useTypes) {
        Set<AddressUseType> newUseTypes = someUseTypes();
        while (newUseTypes.equals(useTypes)) {
            newUseTypes = someUseTypes();
        }
        return newUseTypes;
    }

    // ===== GeoAddressDTO builders for facade commands =====

    static GeoAddressDTO someGeoAddressDTOFor(PartyId partyId) {
        return someGeoAddressDTOFor(partyId, someUseType());
    }

    static GeoAddressDTO someGeoAddressDTOFor(PartyId partyId, AddressUseType useType) {
        return new GeoAddressDTO(
                AddressId.random(),
                partyId,
                someName(),
                someStreet(),
                someBuilding(),
                someFlat(),
                someCity(),
                someZipCode().asString(),
                someLocale(),
                Set.of(useType.name()));
    }

    static GeoAddressDTO someGeoAddressDTOFor(
            PartyId partyId, String name, AddressUseType useType) {
        return new GeoAddressDTO(
                AddressId.random(),
                partyId,
                name,
                someStreet(),
                someBuilding(),
                someFlat(),
                someCity(),
                someZipCode().asString(),
                someLocale(),
                Set.of(useType.name()));
    }

    static GeoAddressDTO someGeoAddressDTOWithId(
            AddressId addressId, PartyId partyId, AddressUseType useType) {
        return new GeoAddressDTO(
                addressId,
                partyId,
                someName(),
                someStreet(),
                someBuilding(),
                someFlat(),
                someCity(),
                someZipCode().asString(),
                someLocale(),
                Set.of(useType.name()));
    }

    static GeoAddressDTO geoAddressDTOWith(
            PartyId partyId, String name, String city, AddressUseType... useTypes) {
        Set<String> useTypeStrings = new HashSet<>();
        for (AddressUseType ut : useTypes) {
            useTypeStrings.add(ut.name());
        }
        return new GeoAddressDTO(
                AddressId.random(),
                partyId,
                name,
                someStreet(),
                someBuilding(),
                someFlat(),
                city,
                someZipCode().asString(),
                someLocale(),
                useTypeStrings);
    }

    static GeoAddressDTO geoAddressDTOWithLocale(
            PartyId partyId, String name, String city, Locale locale, AddressUseType... useTypes) {
        Set<String> useTypeStrings = new HashSet<>();
        for (AddressUseType ut : useTypes) {
            useTypeStrings.add(ut.name());
        }
        return new GeoAddressDTO(
                AddressId.random(),
                partyId,
                name,
                someStreet(),
                someBuilding(),
                someFlat(),
                city,
                someZipCode().asString(),
                locale,
                useTypeStrings);
    }
}
