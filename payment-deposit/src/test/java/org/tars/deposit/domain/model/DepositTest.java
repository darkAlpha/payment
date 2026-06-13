package org.tars.deposit.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Deposit Aggregate Root")
class DepositTest {

    private static final String CUSTOMER_ID = "cust-001";
    private static final String ACCOUNT_ID = "acc-001";
    private static final Money VALID_MONEY = Money.of(BigDecimal.valueOf(500), "USD");

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should create deposit in PENDING status")
        void shouldCreateInPendingStatus() {
            Deposit deposit = Deposit.create(CUSTOMER_ID, ACCOUNT_ID, VALID_MONEY, "Test deposit");

            assertNotNull(deposit.getId());
            assertEquals(CUSTOMER_ID, deposit.getCustomerId());
            assertEquals(ACCOUNT_ID, deposit.getAccountId());
            assertEquals(VALID_MONEY, deposit.getMoney());
            assertEquals(DepositStatus.PENDING, deposit.getStatus());
            assertEquals("Test deposit", deposit.getDescription());
            assertNotNull(deposit.getCreatedAt());
        }

        @Test
        @DisplayName("should reject blank customer ID")
        void shouldRejectBlankCustomerId() {
            assertThrows(IllegalArgumentException.class,
                    () -> Deposit.create("", ACCOUNT_ID, VALID_MONEY, "desc"));
        }

        @Test
        @DisplayName("should reject null customer ID")
        void shouldRejectNullCustomerId() {
            assertThrows(IllegalArgumentException.class,
                    () -> Deposit.create(null, ACCOUNT_ID, VALID_MONEY, "desc"));
        }

        @Test
        @DisplayName("should reject blank account ID")
        void shouldRejectBlankAccountId() {
            assertThrows(IllegalArgumentException.class,
                    () -> Deposit.create(CUSTOMER_ID, "", VALID_MONEY, "desc"));
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            Money zeroMoney = Money.of(BigDecimal.ZERO, "USD");
            assertThrows(IllegalArgumentException.class,
                    () -> Deposit.create(CUSTOMER_ID, ACCOUNT_ID, zeroMoney, "desc"));
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("should transition PENDING -> VALIDATED")
        void shouldTransitionToValidated() {
            Deposit deposit = Deposit.create(CUSTOMER_ID, ACCOUNT_ID, VALID_MONEY, "desc");

            deposit.markValidated();

            assertEquals(DepositStatus.VALIDATED, deposit.getStatus());
        }

        @Test
        @DisplayName("should transition VALIDATED -> LEDGER_POSTED")
        void shouldTransitionToLedgerPosted() {
            Deposit deposit = Deposit.create(CUSTOMER_ID, ACCOUNT_ID, VALID_MONEY, "desc");
            deposit.markValidated();

            deposit.markLedgerPosted();

            assertEquals(DepositStatus.LEDGER_POSTED, deposit.getStatus());
        }

        @Test
        @DisplayName("should transition LEDGER_POSTED -> COMPLETED")
        void shouldTransitionToCompleted() {
            Deposit deposit = Deposit.create(CUSTOMER_ID, ACCOUNT_ID, VALID_MONEY, "desc");
            deposit.markValidated();
            deposit.markLedgerPosted();

            deposit.markCompleted();

            assertEquals(DepositStatus.COMPLETED, deposit.getStatus());
        }

        @Test
        @DisplayName("should not allow PENDING -> LEDGER_POSTED directly")
        void shouldNotSkipValidation() {
            Deposit deposit = Deposit.create(CUSTOMER_ID, ACCOUNT_ID, VALID_MONEY, "desc");

            assertThrows(IllegalStateException.class, deposit::markLedgerPosted);
        }

        @Test
        @DisplayName("should not allow PENDING -> COMPLETED directly")
        void shouldNotSkipToCompleted() {
            Deposit deposit = Deposit.create(CUSTOMER_ID, ACCOUNT_ID, VALID_MONEY, "desc");

            assertThrows(IllegalStateException.class, deposit::markCompleted);
        }

        @Test
        @DisplayName("should not allow VALIDATED -> COMPLETED directly")
        void shouldNotSkipLedgerPosted() {
            Deposit deposit = Deposit.create(CUSTOMER_ID, ACCOUNT_ID, VALID_MONEY, "desc");
            deposit.markValidated();

            assertThrows(IllegalStateException.class, deposit::markCompleted);
        }

        @Test
        @DisplayName("should allow marking as FAILED from any state")
        void shouldAllowFailFromAnyState() {
            Deposit deposit = Deposit.create(CUSTOMER_ID, ACCOUNT_ID, VALID_MONEY, "desc");

            deposit.markFailed("Some reason");

            assertEquals(DepositStatus.FAILED, deposit.getStatus());
            assertEquals("Some reason", deposit.getFailureReason());
        }

        @Test
        @DisplayName("should allow marking VALIDATED as FAILED")
        void shouldAllowFailFromValidated() {
            Deposit deposit = Deposit.create(CUSTOMER_ID, ACCOUNT_ID, VALID_MONEY, "desc");
            deposit.markValidated();

            deposit.markFailed("Ledger error");

            assertEquals(DepositStatus.FAILED, deposit.getStatus());
            assertEquals("Ledger error", deposit.getFailureReason());
        }
    }

    @Nested
    @DisplayName("Reconstitution")
    class Reconstitution {

        @Test
        @DisplayName("should reconstitute deposit from persisted data")
        void shouldReconstitute() {
            Deposit deposit = Deposit.reconstitute(
                    "dep-123", CUSTOMER_ID, ACCOUNT_ID,
                    VALID_MONEY, "reconstituted",
                    DepositStatus.COMPLETED, null,
                    java.time.LocalDateTime.now().minusHours(1),
                    java.time.LocalDateTime.now()
            );

            assertEquals("dep-123", deposit.getId());
            assertEquals(DepositStatus.COMPLETED, deposit.getStatus());
            assertEquals("reconstituted", deposit.getDescription());
        }
    }
}

