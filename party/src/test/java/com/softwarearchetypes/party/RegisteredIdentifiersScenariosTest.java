package com.softwarearchetypes.party;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softwarearchetypes.common.Result;
import com.softwarearchetypes.party.commands.AddRegisteredIdentifierCommand;
import com.softwarearchetypes.party.commands.RegisterCompanyCommand;
import com.softwarearchetypes.party.commands.RegisterPersonCommand;
import com.softwarearchetypes.party.commands.RemoveRegisteredIdentifierCommand;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Scenarios for RegisteredIdentifiers: PESEL, NIP, Passport. Tests Validity periods, expiration,
 * and identifier management.
 */
@DisplayName("Registered Identifiers Scenarios")
class RegisteredIdentifiersScenariosTest {

    private final PartyConfiguration configuration = PartyConfiguration.inMemory();
    private final PartiesFacade partiesFacade = configuration.partiesFacade();
    private final PartiesQueries partiesQueries = configuration.partiesQueries();

    // ===== PESEL (PersonalIdentificationNumber) =====

    @Nested
    @DisplayName("PESEL (Personal Identification Number)")
    class PeselScenarios {

        @Test
        @DisplayName("PESEL can be created with valid 11-digit number")
        void peselCanBeCreatedWithValidNumber() {
            PersonalIdentificationNumber pesel = PersonalIdentificationNumber.of("44051401458");

            assertEquals("44051401458", pesel.asString());
            assertEquals("PERSONAL_IDENTIFICATION_NUMBER", pesel.type());
        }

        @Test
        @DisplayName("PESEL never expires - always valid")
        void peselNeverExpires() {
            PersonalIdentificationNumber pesel = PersonalIdentificationNumber.of("44051401458");

            assertEquals(Validity.ALWAYS, pesel.validity());
            assertTrue(pesel.isCurrentlyValid());
            assertTrue(pesel.isValidAt(Instant.parse("2050-01-01T00:00:00Z")));
            assertTrue(pesel.isValidAt(Instant.parse("1990-01-01T00:00:00Z")));
        }

        @Test
        @DisplayName("PESEL with invalid checksum is rejected")
        void peselWithInvalidChecksumIsRejected() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> PersonalIdentificationNumber.of("44051401459")); // wrong checksum
        }

        @Test
        @DisplayName("PESEL with wrong length is rejected")
        void peselWithWrongLengthIsRejected() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> PersonalIdentificationNumber.of("4405140145")); // 10 digits
            assertThrows(
                    IllegalArgumentException.class,
                    () -> PersonalIdentificationNumber.of("440514014580")); // 12 digits
        }

        @Test
        @DisplayName("Person can have PESEL added after registration")
        void personCanHavePeselAddedAfterRegistration() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Maria", "Zielińska", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            PersonalIdentificationNumber pesel = PersonalIdentificationNumber.of("44051401458");
            Result<String, PartyId> result =
                    partiesFacade.handle(new AddRegisteredIdentifierCommand(personId, pesel));

            assertTrue(result.success());
            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertTrue(party.registeredIdentifiers().contains(pesel));
        }
    }

    // ===== NIP (TaxNumber) =====

    @Nested
    @DisplayName("NIP (Tax Number)")
    class NipScenarios {

        @Test
        @DisplayName("NIP can be created with valid 10-digit number")
        void nipCanBeCreatedWithValidNumber() {
            TaxNumber nip = TaxNumber.of("1234563218");

            assertEquals("1234563218", nip.asString());
            assertEquals("TAX_NUMBER", nip.type());
        }

        @Test
        @DisplayName("NIP never expires - permanently assigned")
        void nipNeverExpires() {
            TaxNumber nip = TaxNumber.of("1234563218");

            assertEquals(Validity.ALWAYS, nip.validity());
            assertTrue(nip.isCurrentlyValid());
        }

        @Test
        @DisplayName("NIP with invalid checksum is rejected")
        void nipWithInvalidChecksumIsRejected() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> TaxNumber.of("1234563219")); // wrong checksum
        }

        @Test
        @DisplayName("Company can have NIP added after registration")
        void companyCanHaveNipAddedAfterRegistration() {
            PartyId companyId =
                    partiesFacade
                            .handle(
                                    new RegisterCompanyCommand(
                                            "Tech Solutions Sp. z o.o.", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            TaxNumber nip = TaxNumber.of("1234563218");
            Result<String, PartyId> result =
                    partiesFacade.handle(new AddRegisteredIdentifierCommand(companyId, nip));

            assertTrue(result.success());
        }
    }

    // ===== Passport =====

    @Nested
    @DisplayName("Passport with expiration")
    class PassportScenarios {

        @Test
        @DisplayName("Passport can be created with expiration date")
        void passportCanBeCreatedWithExpirationDate() {
            Validity expiresIn2030 = Validity.until(Instant.parse("2030-12-31T23:59:59Z"));
            Passport passport = Passport.of("AB1234567", expiresIn2030);

            assertEquals("AB1234567", passport.asString());
            assertEquals("PASSPORT", passport.type());
        }

        @Test
        @DisplayName("Valid passport is currently valid")
        void validPassportIsCurrentlyValid() {
            Validity expiresIn2030 = Validity.until(Instant.parse("2030-12-31T23:59:59Z"));
            Passport passport = Passport.of("AB1234567", expiresIn2030);

            assertTrue(passport.isCurrentlyValid());
            assertFalse(passport.validity().equals(Validity.ALWAYS));
        }

        @Test
        @DisplayName("Expired passport is not valid")
        void expiredPassportIsNotValid() {
            Validity expiredValidity = Validity.until(Instant.now().minus(1, ChronoUnit.DAYS));
            Passport expiredPassport = Passport.of("XY7890123", expiredValidity);

            assertFalse(expiredPassport.isCurrentlyValid());
        }

        @Test
        @DisplayName("Passport validity can be checked at specific instant")
        void passportValidityCanBeCheckedAtSpecificInstant() {
            Validity validity = Validity.until(Instant.parse("2025-06-15T00:00:00Z"));
            Passport passport = Passport.of("DE4567890", validity);

            assertTrue(passport.isValidAt(Instant.parse("2024-01-01T00:00:00Z")));
            assertFalse(passport.isValidAt(Instant.parse("2026-01-01T00:00:00Z")));
        }

        @Test
        @DisplayName("Person can have passport with future expiration")
        void personCanHavePassportWithFutureExpiration() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Adam", "Kwiatkowski", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            Passport passport =
                    Passport.of("GH7890123", Validity.until(Instant.parse("2030-06-15T00:00:00Z")));
            partiesFacade.handle(new AddRegisteredIdentifierCommand(personId, passport));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertTrue(party.registeredIdentifiers().contains(passport));
        }
    }

    // ===== Validity =====

    @Nested
    @DisplayName("Validity periods")
    class ValidityScenarios {

        @Test
        @DisplayName("Validity.ALWAYS covers all time")
        void validityAlwaysCoversAllTime() {
            Validity always = Validity.ALWAYS;

            assertTrue(always.isValidAt(Instant.EPOCH));
            assertTrue(always.isValidAt(Instant.now()));
            assertTrue(always.isValidAt(Instant.parse("2100-01-01T00:00:00Z")));
        }

        @Test
        @DisplayName("Validity can be created with start date only")
        void validityCanBeCreatedWithStartDateOnly() {
            Instant startDate = Instant.parse("2024-01-01T00:00:00Z");
            Validity validity = Validity.from(startDate);

            assertFalse(validity.isValidAt(Instant.parse("2023-12-31T00:00:00Z")));
            assertTrue(validity.isValidAt(Instant.parse("2024-01-01T00:00:00Z")));
            assertTrue(validity.isValidAt(Instant.parse("2100-01-01T00:00:00Z")));
        }

        @Test
        @DisplayName("Validity can be created with end date only")
        void validityCanBeCreatedWithEndDateOnly() {
            Instant endDate = Instant.parse("2025-12-31T00:00:00Z");
            Validity validity = Validity.until(endDate);

            assertTrue(validity.isValidAt(Instant.EPOCH));
            assertTrue(validity.isValidAt(Instant.parse("2025-12-30T00:00:00Z")));
            assertFalse(validity.isValidAt(Instant.parse("2025-12-31T00:00:00Z"))); // exclusive
        }

        @Test
        @DisplayName("Validity can be created with date range")
        void validityCanBeCreatedWithDateRange() {
            Instant start = Instant.parse("2024-01-01T00:00:00Z");
            Instant end = Instant.parse("2025-12-31T00:00:00Z");
            Validity validity = Validity.between(start, end);

            assertFalse(validity.isValidAt(Instant.parse("2023-12-31T00:00:00Z")));
            assertTrue(validity.isValidAt(Instant.parse("2024-06-15T00:00:00Z")));
            assertFalse(validity.isValidAt(Instant.parse("2026-01-01T00:00:00Z")));
        }

        @Test
        @DisplayName("Two validity periods can overlap")
        void twoValidityPeriodsCanOverlap() {
            Validity period1 =
                    Validity.between(
                            Instant.parse("2024-01-01T00:00:00Z"),
                            Instant.parse("2024-12-31T00:00:00Z"));
            Validity period2 =
                    Validity.between(
                            Instant.parse("2024-06-01T00:00:00Z"),
                            Instant.parse("2025-06-01T00:00:00Z"));

            assertTrue(period1.overlaps(period2));
            assertTrue(period2.overlaps(period1));
        }

        @Test
        @DisplayName("Non-overlapping validity periods do not overlap")
        void nonOverlappingPeriodsDoNotOverlap() {
            Validity period1 =
                    Validity.between(
                            Instant.parse("2024-01-01T00:00:00Z"),
                            Instant.parse("2024-06-01T00:00:00Z"));
            Validity period2 =
                    Validity.between(
                            Instant.parse("2024-06-01T00:00:00Z"),
                            Instant.parse("2024-12-31T00:00:00Z"));

            assertFalse(period1.overlaps(period2)); // end is exclusive
        }
    }

    // ===== Multiple identifiers =====

    @Nested
    @DisplayName("Multiple identifiers management")
    class MultipleIdentifiersScenarios {

        @Test
        @DisplayName("Party can have multiple registered identifiers")
        void partyCanHaveMultipleIdentifiers() {
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Adam", "Kwiatkowski", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            PersonalIdentificationNumber pesel = PersonalIdentificationNumber.of("44051401458");
            Passport passport =
                    Passport.of("JK3456789", Validity.until(Instant.parse("2030-06-15T00:00:00Z")));

            partiesFacade.handle(new AddRegisteredIdentifierCommand(personId, pesel));
            partiesFacade.handle(new AddRegisteredIdentifierCommand(personId, passport));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertEquals(2, party.registeredIdentifiers().size());
            assertTrue(party.registeredIdentifiers().contains(pesel));
            assertTrue(party.registeredIdentifiers().contains(passport));
        }

        @Test
        @DisplayName("Identifier can be removed from party")
        void identifierCanBeRemovedFromParty() {
            PersonalIdentificationNumber pesel = PersonalIdentificationNumber.of("44051401458");
            PartyId personId =
                    partiesFacade
                            .handle(
                                    new RegisterPersonCommand(
                                            "Ewa", "Kowalska", Set.of(), Set.of(pesel)))
                            .getSuccess()
                            .partyId();

            partiesFacade.handle(new RemoveRegisteredIdentifierCommand(personId, pesel));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            assertFalse(party.registeredIdentifiers().contains(pesel));
        }

        @Test
        @DisplayName("Adding same identifier twice is idempotent")
        void addingSameIdentifierTwiceIsIdempotent() {
            PartyId personId =
                    partiesFacade
                            .handle(new RegisterPersonCommand("Tomek", "Nowak", Set.of(), Set.of()))
                            .getSuccess()
                            .partyId();

            PersonalIdentificationNumber pesel = PersonalIdentificationNumber.of("44051401458");
            partiesFacade.handle(new AddRegisteredIdentifierCommand(personId, pesel));
            partiesFacade.handle(new AddRegisteredIdentifierCommand(personId, pesel));

            PartyView party = partiesQueries.findBy(personId).orElseThrow();
            long peselCount =
                    party.registeredIdentifiers().stream()
                            .filter(id -> id.type().equals("PERSONAL_IDENTIFICATION_NUMBER"))
                            .count();
            assertEquals(1, peselCount);
        }
    }
}
