package org.tars.deposit.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Money Value Object")
class MoneyTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create Money with valid amount and currency")
        void shouldCreateWithValidValues() {
            Money money = Money.of(BigDecimal.valueOf(100.50), "USD");

            assertEquals(BigDecimal.valueOf(100.50), money.amount());
            assertEquals(Currency.getInstance("USD"), money.currency());
        }

        @Test
        @DisplayName("should reject null amount")
        void shouldRejectNullAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Money.of(null, "USD"));
        }

        @Test
        @DisplayName("should reject null currency")
        void shouldRejectNullCurrency() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Money(BigDecimal.TEN, null));
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Money.of(BigDecimal.valueOf(-10), "USD"));
        }

        @Test
        @DisplayName("should allow zero amount")
        void shouldAllowZeroAmount() {
            Money money = Money.of(BigDecimal.ZERO, "USD");
            assertFalse(money.isPositive());
        }
    }

    @Nested
    @DisplayName("Behavior")
    class Behavior {

        @Test
        @DisplayName("should report positive for amount > 0")
        void shouldBePositive() {
            Money money = Money.of(BigDecimal.ONE, "KZT");
            assertTrue(money.isPositive());
        }

        @Test
        @DisplayName("should report not positive for zero")
        void shouldNotBePositiveForZero() {
            Money money = Money.of(BigDecimal.ZERO, "KZT");
            assertFalse(money.isPositive());
        }
    }
}

