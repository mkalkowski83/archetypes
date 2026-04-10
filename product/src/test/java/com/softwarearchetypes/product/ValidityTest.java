package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for Validity - time period during which something is valid. */
class ValidityTest {

    @Nested
    class FactoryMethods {

        @Test
        void shouldCreateValidityFrom() {
            LocalDate from = LocalDate.of(2024, 1, 1);

            Validity validity = Validity.from(from);

            assertEquals(from, validity.from());
            assertNull(validity.to());
        }

        @Test
        void shouldCreateValidityUntil() {
            LocalDate to = LocalDate.of(2024, 12, 31);

            Validity validity = Validity.until(to);

            assertNull(validity.from());
            assertEquals(to, validity.to());
        }

        @Test
        void shouldCreateValidityBetween() {
            LocalDate from = LocalDate.of(2024, 1, 1);
            LocalDate to = LocalDate.of(2024, 12, 31);

            Validity validity = Validity.between(from, to);

            assertEquals(from, validity.from());
            assertEquals(to, validity.to());
        }

        @Test
        void shouldCreateValidityAlways() {
            Validity validity = Validity.always();

            assertNull(validity.from());
            assertNull(validity.to());
        }

        @Test
        void shouldAllowSameFromAndTo() {
            LocalDate date = LocalDate.of(2024, 6, 15);

            Validity validity = Validity.between(date, date);

            assertEquals(date, validity.from());
            assertEquals(date, validity.to());
        }

        @Test
        void shouldRejectFromAfterTo() {
            LocalDate from = LocalDate.of(2024, 12, 31);
            LocalDate to = LocalDate.of(2024, 1, 1);

            assertThrows(IllegalArgumentException.class, () -> Validity.between(from, to));
        }
    }

    @Nested
    class IsValidAtTests {

        @Test
        void shouldBeValidWithinRange() {
            Validity validity =
                    Validity.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

            assertTrue(validity.isValidAt(LocalDate.of(2024, 1, 1)));
            assertTrue(validity.isValidAt(LocalDate.of(2024, 6, 15)));
            assertTrue(validity.isValidAt(LocalDate.of(2024, 12, 31)));
        }

        @Test
        void shouldNotBeValidOutsideRange() {
            Validity validity =
                    Validity.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

            assertFalse(validity.isValidAt(LocalDate.of(2023, 12, 31)));
            assertFalse(validity.isValidAt(LocalDate.of(2025, 1, 1)));
        }

        @Test
        void shouldBeValidAfterFromDate() {
            Validity validity = Validity.from(LocalDate.of(2024, 1, 1));

            assertTrue(validity.isValidAt(LocalDate.of(2024, 1, 1)));
            assertTrue(validity.isValidAt(LocalDate.of(2024, 6, 15)));
            assertTrue(validity.isValidAt(LocalDate.of(2100, 12, 31)));

            assertFalse(validity.isValidAt(LocalDate.of(2023, 12, 31)));
        }

        @Test
        void shouldBeValidBeforeToDate() {
            Validity validity = Validity.until(LocalDate.of(2024, 12, 31));

            assertTrue(validity.isValidAt(LocalDate.of(2024, 12, 31)));
            assertTrue(validity.isValidAt(LocalDate.of(2024, 6, 15)));
            assertTrue(validity.isValidAt(LocalDate.of(1900, 1, 1)));

            assertFalse(validity.isValidAt(LocalDate.of(2025, 1, 1)));
        }

        @Test
        void shouldAlwaysBeValidWithNoBoundaries() {
            Validity validity = Validity.always();

            assertTrue(validity.isValidAt(LocalDate.of(1900, 1, 1)));
            assertTrue(validity.isValidAt(LocalDate.of(2024, 6, 15)));
            assertTrue(validity.isValidAt(LocalDate.of(2100, 12, 31)));
        }

        @Test
        void shouldNotBeValidForNullDate() {
            Validity validity = Validity.always();

            assertFalse(validity.isValidAt(null));
        }

        @Test
        void shouldBeValidOnSingleDay() {
            LocalDate singleDay = LocalDate.of(2024, 6, 15);
            Validity validity = Validity.between(singleDay, singleDay);

            assertTrue(validity.isValidAt(singleDay));
            assertFalse(validity.isValidAt(LocalDate.of(2024, 6, 14)));
            assertFalse(validity.isValidAt(LocalDate.of(2024, 6, 16)));
        }
    }

    @Nested
    class EqualityTests {

        @Test
        void shouldBeEqualWithSameBoundaries() {
            Validity validity1 =
                    Validity.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
            Validity validity2 =
                    Validity.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

            assertEquals(validity1, validity2);
            assertEquals(validity1.hashCode(), validity2.hashCode());
        }

        @Test
        void shouldBeEqualForAlways() {
            Validity validity1 = Validity.always();
            Validity validity2 = Validity.always();

            assertEquals(validity1, validity2);
            assertEquals(validity1.hashCode(), validity2.hashCode());
        }

        @Test
        void shouldNotBeEqualWithDifferentBoundaries() {
            Validity validity1 =
                    Validity.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
            Validity validity2 =
                    Validity.between(LocalDate.of(2024, 1, 1), LocalDate.of(2025, 12, 31));

            assertFalse(validity1.equals(validity2));
        }
    }

    @Nested
    class ToStringTests {

        @Test
        void shouldFormatAlwaysAsAlways() {
            Validity validity = Validity.always();

            assertEquals("always", validity.toString());
        }

        @Test
        void shouldFormatFromDateOnly() {
            Validity validity = Validity.from(LocalDate.of(2024, 1, 1));

            assertEquals("from 2024-01-01", validity.toString());
        }

        @Test
        void shouldFormatToDateOnly() {
            Validity validity = Validity.until(LocalDate.of(2024, 12, 31));

            assertEquals("until 2024-12-31", validity.toString());
        }

        @Test
        void shouldFormatBothDates() {
            Validity validity =
                    Validity.between(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

            assertEquals("2024-01-01 to 2024-12-31", validity.toString());
        }
    }
}
