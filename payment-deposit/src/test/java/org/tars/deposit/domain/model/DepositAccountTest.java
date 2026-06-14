package org.tars.deposit.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DepositAccount Aggregate")
class DepositAccountTest {

    @Nested
    @DisplayName("Opening")
    class Opening {

        @Test
        @DisplayName("should open deposit with valid parameters")
        void shouldOpenDeposit() {
            DepositAccount account = DepositAccount.open("cust-1", BigDecimal.valueOf(10000),
                    BigDecimal.valueOf(12.5), "KZT", 12);

            assertNotNull(account.getId());
            assertEquals("cust-1", account.getCustomerId());
            assertEquals(BigDecimal.valueOf(10000), account.getBalance());
            assertEquals(DepositAccountStatus.ACTIVE, account.getStatus());
            assertEquals(12, account.getTermMonths());
        }

        @Test
        @DisplayName("should reject zero initial amount")
        void shouldRejectZeroAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> DepositAccount.open("cust-1", BigDecimal.ZERO, BigDecimal.valueOf(10), "KZT", 6));
        }

        @Test
        @DisplayName("should reject zero term")
        void shouldRejectZeroTerm() {
            assertThrows(IllegalArgumentException.class,
                    () -> DepositAccount.open("cust-1", BigDecimal.valueOf(1000), BigDecimal.valueOf(10), "KZT", 0));
        }
    }

    @Nested
    @DisplayName("Operations")
    class Operations {

        @Test
        @DisplayName("should credit amount")
        void shouldCredit() {
            DepositAccount account = DepositAccount.open("cust-1", BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(10), "USD", 12);
            account.credit(BigDecimal.valueOf(500));
            assertEquals(BigDecimal.valueOf(1500), account.getBalance());
        }

        @Test
        @DisplayName("should debit amount")
        void shouldDebit() {
            DepositAccount account = DepositAccount.open("cust-1", BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(10), "USD", 12);
            account.debit(BigDecimal.valueOf(300));
            assertEquals(BigDecimal.valueOf(700), account.getBalance());
        }

        @Test
        @DisplayName("should reject debit exceeding balance")
        void shouldRejectOverdraft() {
            DepositAccount account = DepositAccount.open("cust-1", BigDecimal.valueOf(100),
                    BigDecimal.valueOf(10), "USD", 12);
            assertThrows(IllegalStateException.class, () -> account.debit(BigDecimal.valueOf(200)));
        }

        @Test
        @DisplayName("should accrue interest")
        void shouldAccrueInterest() {
            DepositAccount account = DepositAccount.open("cust-1", BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(10), "USD", 12);
            account.accrueInterest(BigDecimal.valueOf(0.27));
            assertEquals(BigDecimal.valueOf(1000.27), account.getBalance());
            assertEquals(BigDecimal.valueOf(0.27), account.getAccruedInterest());
        }

        @Test
        @DisplayName("should close and return final balance")
        void shouldClose() {
            DepositAccount account = DepositAccount.open("cust-1", BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(10), "USD", 12);
            BigDecimal finalBalance = account.close();
            assertEquals(BigDecimal.valueOf(5000), finalBalance);
            assertEquals(BigDecimal.ZERO, account.getBalance());
            assertEquals(DepositAccountStatus.CLOSED, account.getStatus());
        }

        @Test
        @DisplayName("should reject operations on closed account")
        void shouldRejectOperationsOnClosed() {
            DepositAccount account = DepositAccount.open("cust-1", BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(10), "USD", 12);
            account.close();
            assertThrows(IllegalStateException.class, () -> account.credit(BigDecimal.valueOf(100)));
        }
    }
}
