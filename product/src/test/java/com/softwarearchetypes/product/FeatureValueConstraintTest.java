package com.softwarearchetypes.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for FeatureValueConstraint implementations. */
class FeatureValueConstraintTest {

    @Nested
    class NumericRangeConstraintTests {

        @Test
        void shouldAcceptValueWithinRange() {
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100);

            assertTrue(constraint.isValid(1));
            assertTrue(constraint.isValid(50));
            assertTrue(constraint.isValid(100));
        }

        @Test
        void shouldRejectValueOutsideRange() {
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100);

            assertFalse(constraint.isValid(0));
            assertFalse(constraint.isValid(101));
            assertFalse(constraint.isValid(-5));
        }

        @Test
        void shouldRejectNonIntegerValues() {
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100);

            assertFalse(constraint.isValid("50"));
            assertFalse(constraint.isValid(50.5));
            assertFalse(constraint.isValid(null));
        }

        @Test
        void shouldRejectInvalidRange() {
            assertThrows(
                    IllegalArgumentException.class, () -> NumericRangeConstraint.between(100, 1));
        }

        @Test
        void shouldAllowSameMinAndMax() {
            FeatureValueConstraint constraint = NumericRangeConstraint.between(42, 42);

            assertTrue(constraint.isValid(42));
            assertFalse(constraint.isValid(41));
            assertFalse(constraint.isValid(43));
        }

        @Test
        void shouldHaveCorrectValueType() {
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100);

            assertEquals(FeatureValueType.INTEGER, constraint.valueType());
            assertEquals("NUMERIC_RANGE", constraint.type());
        }

        @Test
        void shouldConvertFromString() {
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100);

            Object result = constraint.fromString("50");

            assertEquals(50, result);
        }

        @Test
        void shouldRejectInvalidValueFromString() {
            FeatureValueConstraint constraint = NumericRangeConstraint.between(1, 100);

            assertThrows(IllegalArgumentException.class, () -> constraint.fromString("150"));
        }
    }

    @Nested
    class DecimalRangeConstraintTests {

        @Test
        void shouldAcceptValueWithinRange() {
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0");

            assertTrue(constraint.isValid(new BigDecimal("0.5")));
            assertTrue(constraint.isValid(new BigDecimal("50.25")));
            assertTrue(constraint.isValid(new BigDecimal("100.0")));
        }

        @Test
        void shouldRejectValueOutsideRange() {
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0");

            assertFalse(constraint.isValid(new BigDecimal("0.4")));
            assertFalse(constraint.isValid(new BigDecimal("100.1")));
            assertFalse(constraint.isValid(new BigDecimal("-1.0")));
        }

        @Test
        void shouldRejectNonBigDecimalValues() {
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0");

            assertFalse(constraint.isValid("50.0"));
            assertFalse(constraint.isValid(50));
            assertFalse(constraint.isValid(50.0));
            assertFalse(constraint.isValid(null));
        }

        @Test
        void shouldRejectInvalidRange() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> DecimalRangeConstraint.of("100.0", "0.5"));
        }

        @Test
        void shouldAllowSameMinAndMax() {
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("42.5", "42.5");

            assertTrue(constraint.isValid(new BigDecimal("42.5")));
            assertFalse(constraint.isValid(new BigDecimal("42.4")));
            assertFalse(constraint.isValid(new BigDecimal("42.6")));
        }

        @Test
        void shouldHaveCorrectValueType() {
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0");

            assertEquals(FeatureValueType.DECIMAL, constraint.valueType());
            assertEquals("DECIMAL_RANGE", constraint.type());
        }

        @Test
        void shouldConvertFromString() {
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0");

            Object result = constraint.fromString("50.25");

            assertEquals(new BigDecimal("50.25"), result);
        }

        @Test
        void shouldRejectInvalidValueFromString() {
            FeatureValueConstraint constraint = DecimalRangeConstraint.of("0.5", "100.0");

            assertThrows(IllegalArgumentException.class, () -> constraint.fromString("150.0"));
        }
    }

    @Nested
    class DateRangeConstraintTests {

        @Test
        void shouldAcceptDateWithinRange() {
            FeatureValueConstraint constraint =
                    DateRangeConstraint.between("2024-01-01", "2024-12-31");

            assertTrue(constraint.isValid(LocalDate.of(2024, 1, 1)));
            assertTrue(constraint.isValid(LocalDate.of(2024, 6, 15)));
            assertTrue(constraint.isValid(LocalDate.of(2024, 12, 31)));
        }

        @Test
        void shouldRejectDateOutsideRange() {
            FeatureValueConstraint constraint =
                    DateRangeConstraint.between("2024-01-01", "2024-12-31");

            assertFalse(constraint.isValid(LocalDate.of(2023, 12, 31)));
            assertFalse(constraint.isValid(LocalDate.of(2025, 1, 1)));
        }

        @Test
        void shouldRejectNonDateValues() {
            FeatureValueConstraint constraint =
                    DateRangeConstraint.between("2024-01-01", "2024-12-31");

            assertFalse(constraint.isValid("2024-06-15"));
            assertFalse(constraint.isValid(20240615));
            assertFalse(constraint.isValid(null));
        }

        @Test
        void shouldRejectInvalidRange() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> DateRangeConstraint.between("2024-12-31", "2024-01-01"));
        }

        @Test
        void shouldAllowSameFromAndTo() {
            FeatureValueConstraint constraint =
                    DateRangeConstraint.between("2024-06-15", "2024-06-15");

            assertTrue(constraint.isValid(LocalDate.of(2024, 6, 15)));
            assertFalse(constraint.isValid(LocalDate.of(2024, 6, 14)));
            assertFalse(constraint.isValid(LocalDate.of(2024, 6, 16)));
        }

        @Test
        void shouldHaveCorrectValueType() {
            FeatureValueConstraint constraint =
                    DateRangeConstraint.between("2024-01-01", "2024-12-31");

            assertEquals(FeatureValueType.DATE, constraint.valueType());
            assertEquals("DATE_RANGE", constraint.type());
        }

        @Test
        void shouldConvertFromString() {
            FeatureValueConstraint constraint =
                    DateRangeConstraint.between("2024-01-01", "2024-12-31");

            Object result = constraint.fromString("2024-06-15");

            assertEquals(LocalDate.of(2024, 6, 15), result);
        }

        @Test
        void shouldRejectInvalidValueFromString() {
            FeatureValueConstraint constraint =
                    DateRangeConstraint.between("2024-01-01", "2024-12-31");

            assertThrows(IllegalArgumentException.class, () -> constraint.fromString("2025-06-15"));
        }
    }

    @Nested
    class RegexConstraintTests {

        @Test
        void shouldAcceptMatchingValue() {
            FeatureValueConstraint constraint = RegexConstraint.of("^[A-Z]{2}-\\d{4}$");

            assertTrue(constraint.isValid("AB-1234"));
            assertTrue(constraint.isValid("XY-9999"));
            assertTrue(constraint.isValid("PL-0001"));
        }

        @Test
        void shouldRejectNonMatchingValue() {
            FeatureValueConstraint constraint = RegexConstraint.of("^[A-Z]{2}-\\d{4}$");

            assertFalse(constraint.isValid("ab-1234"));
            assertFalse(constraint.isValid("ABC-1234"));
            assertFalse(constraint.isValid("AB-123"));
            assertFalse(constraint.isValid("AB1234"));
        }

        @Test
        void shouldRejectNonStringValues() {
            FeatureValueConstraint constraint = RegexConstraint.of("^\\d+$");

            assertFalse(constraint.isValid(123));
            assertFalse(constraint.isValid(null));
        }

        @Test
        void shouldRejectBlankPattern() {
            assertThrows(IllegalArgumentException.class, () -> RegexConstraint.of(""));
            assertThrows(IllegalArgumentException.class, () -> RegexConstraint.of("   "));
            assertThrows(IllegalArgumentException.class, () -> RegexConstraint.of(null));
        }

        @Test
        void shouldHaveCorrectValueType() {
            FeatureValueConstraint constraint = RegexConstraint.of("^[A-Z]+$");

            assertEquals(FeatureValueType.TEXT, constraint.valueType());
            assertEquals("REGEX", constraint.type());
        }

        @Test
        void shouldConvertFromString() {
            FeatureValueConstraint constraint = RegexConstraint.of("^[A-Z]{2}-\\d{4}$");

            Object result = constraint.fromString("AB-1234");

            assertEquals("AB-1234", result);
        }

        @Test
        void shouldRejectInvalidValueFromString() {
            FeatureValueConstraint constraint = RegexConstraint.of("^[A-Z]{2}-\\d{4}$");

            assertThrows(IllegalArgumentException.class, () -> constraint.fromString("invalid"));
        }
    }

    @Nested
    class AllowedValuesConstraintTests {

        @Test
        void shouldAcceptAllowedValue() {
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("red", "blue", "green");

            assertTrue(constraint.isValid("red"));
            assertTrue(constraint.isValid("blue"));
            assertTrue(constraint.isValid("green"));
        }

        @Test
        void shouldRejectNotAllowedValue() {
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("red", "blue", "green");

            assertFalse(constraint.isValid("yellow"));
            assertFalse(constraint.isValid("RED"));
            assertFalse(constraint.isValid(""));
        }

        @Test
        void shouldRejectNonStringValues() {
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("1", "2", "3");

            assertFalse(constraint.isValid(1));
            assertFalse(constraint.isValid(null));
        }

        @Test
        void shouldRejectEmptyAllowedValues() {
            assertThrows(IllegalArgumentException.class, () -> AllowedValuesConstraint.of());
        }

        @Test
        void shouldWorkWithSingleAllowedValue() {
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("only");

            assertTrue(constraint.isValid("only"));
            assertFalse(constraint.isValid("other"));
        }

        @Test
        void shouldHaveCorrectValueType() {
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("a", "b", "c");

            assertEquals(FeatureValueType.TEXT, constraint.valueType());
            assertEquals("ALLOWED_VALUES", constraint.type());
        }

        @Test
        void shouldConvertFromString() {
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("red", "blue", "green");

            Object result = constraint.fromString("red");

            assertEquals("red", result);
        }

        @Test
        void shouldRejectInvalidValueFromString() {
            FeatureValueConstraint constraint = AllowedValuesConstraint.of("red", "blue", "green");

            assertThrows(IllegalArgumentException.class, () -> constraint.fromString("yellow"));
        }
    }

    @Nested
    class UnconstrainedTests {

        @Test
        void shouldAcceptAnyTextValue() {
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.TEXT);

            assertTrue(constraint.isValid("anything"));
            assertTrue(constraint.isValid(""));
            assertTrue(constraint.isValid("special chars: !@#$%"));
        }

        @Test
        void shouldAcceptAnyIntegerValue() {
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.INTEGER);

            assertTrue(constraint.isValid(0));
            assertTrue(constraint.isValid(-100));
            assertTrue(constraint.isValid(Integer.MAX_VALUE));
        }

        @Test
        void shouldAcceptAnyDecimalValue() {
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.DECIMAL);

            assertTrue(constraint.isValid(new BigDecimal("0")));
            assertTrue(constraint.isValid(new BigDecimal("-100.5")));
            assertTrue(constraint.isValid(new BigDecimal("999999.999")));
        }

        @Test
        void shouldAcceptAnyDateValue() {
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.DATE);

            assertTrue(constraint.isValid(LocalDate.of(1900, 1, 1)));
            assertTrue(constraint.isValid(LocalDate.of(2100, 12, 31)));
            assertTrue(constraint.isValid(LocalDate.now()));
        }

        @Test
        void shouldAcceptAnyBooleanValue() {
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.BOOLEAN);

            assertTrue(constraint.isValid(true));
            assertTrue(constraint.isValid(false));
        }

        @Test
        void shouldRejectWrongType() {
            FeatureValueConstraint textConstraint = new Unconstrained(FeatureValueType.TEXT);
            FeatureValueConstraint intConstraint = new Unconstrained(FeatureValueType.INTEGER);

            assertFalse(textConstraint.isValid(123));
            assertFalse(intConstraint.isValid("123"));
            assertFalse(textConstraint.isValid(null));
        }

        @Test
        void shouldRejectNullValueType() {
            assertThrows(IllegalArgumentException.class, () -> new Unconstrained(null));
        }

        @Test
        void shouldHaveCorrectTypeIdentifier() {
            FeatureValueConstraint constraint = new Unconstrained(FeatureValueType.TEXT);

            assertEquals("UNCONSTRAINED", constraint.type());
        }

        @Test
        void shouldReturnCorrectValueType() {
            FeatureValueConstraint textConstraint = new Unconstrained(FeatureValueType.TEXT);
            FeatureValueConstraint intConstraint = new Unconstrained(FeatureValueType.INTEGER);

            assertEquals(FeatureValueType.TEXT, textConstraint.valueType());
            assertEquals(FeatureValueType.INTEGER, intConstraint.valueType());
        }
    }

    @Nested
    class FeatureValueTypeTests {

        @Test
        void shouldCastTextFromString() {
            Object result = FeatureValueType.TEXT.castFrom("hello");

            assertEquals("hello", result);
        }

        @Test
        void shouldCastIntegerFromString() {
            Object result = FeatureValueType.INTEGER.castFrom("42");

            assertEquals(42, result);
        }

        @Test
        void shouldCastDecimalFromString() {
            Object result = FeatureValueType.DECIMAL.castFrom("42.5");

            assertEquals(new BigDecimal("42.5"), result);
        }

        @Test
        void shouldCastDateFromString() {
            Object result = FeatureValueType.DATE.castFrom("2024-06-15");

            assertEquals(LocalDate.of(2024, 6, 15), result);
        }

        @Test
        void shouldCastBooleanFromString() {
            assertEquals(true, FeatureValueType.BOOLEAN.castFrom("true"));
            assertEquals(false, FeatureValueType.BOOLEAN.castFrom("false"));
        }

        @Test
        void shouldCastTextToString() {
            String result = FeatureValueType.TEXT.castTo("hello");

            assertEquals("hello", result);
        }

        @Test
        void shouldCastIntegerToString() {
            String result = FeatureValueType.INTEGER.castTo(42);

            assertEquals("42", result);
        }

        @Test
        void shouldCastDecimalToString() {
            String result = FeatureValueType.DECIMAL.castTo(new BigDecimal("42.5"));

            assertEquals("42.5", result);
        }

        @Test
        void shouldCastDateToString() {
            String result = FeatureValueType.DATE.castTo(LocalDate.of(2024, 6, 15));

            assertEquals("2024-06-15", result);
        }

        @Test
        void shouldCastBooleanToString() {
            assertEquals("true", FeatureValueType.BOOLEAN.castTo(true));
            assertEquals("false", FeatureValueType.BOOLEAN.castTo(false));
        }

        @Test
        void shouldCheckInstanceCorrectly() {
            assertTrue(FeatureValueType.TEXT.isInstance("hello"));
            assertTrue(FeatureValueType.INTEGER.isInstance(42));
            assertTrue(FeatureValueType.DECIMAL.isInstance(new BigDecimal("42.5")));
            assertTrue(FeatureValueType.DATE.isInstance(LocalDate.now()));
            assertTrue(FeatureValueType.BOOLEAN.isInstance(true));

            assertFalse(FeatureValueType.TEXT.isInstance(42));
            assertFalse(FeatureValueType.INTEGER.isInstance("42"));
        }
    }
}
