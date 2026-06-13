package org.tars.deposit.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.tars.deposit.application.dto.CreateDepositCommand;
import org.tars.deposit.application.dto.DepositResult;
import org.tars.deposit.application.port.output.AccountServicePort;
import org.tars.deposit.application.port.output.AccountServicePort.LedgerPostingResult;
import org.tars.deposit.application.port.output.CustomerServicePort;
import org.tars.deposit.application.port.output.CustomerServicePort.CustomerInfo;
import org.tars.deposit.domain.exception.CustomerNotFoundException;
import org.tars.deposit.domain.exception.LedgerPostingException;
import org.tars.deposit.domain.model.Deposit;
import org.tars.deposit.domain.model.DepositStatus;
import org.tars.deposit.domain.repository.DepositRepository;
import org.tars.deposit.infrastructure.audit.AuditLogService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DepositApplicationService")
class DepositApplicationServiceTest {

    @Mock
    private DepositRepository depositRepository;

    @Mock
    private CustomerServicePort customerServicePort;

    @Mock
    private AccountServicePort accountServicePort;

    @Mock
    private AuditLogService auditLogService;

    private DepositApplicationService service;

    private static final String CUSTOMER_ID = "cust-001";
    private static final String ACCOUNT_ID = "acc-001";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(1000);
    private static final String CURRENCY = "USD";

    @BeforeEach
    void setUp() {
        service = new DepositApplicationService(depositRepository, customerServicePort, accountServicePort, auditLogService);

        // Default: repository returns the deposit as-is
        when(depositRepository.save(any(Deposit.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CreateDepositCommand validCommand() {
        return new CreateDepositCommand(CUSTOMER_ID, ACCOUNT_ID, AMOUNT, CURRENCY, "Test deposit");
    }

    @Nested
    @DisplayName("Successful deposit creation")
    class SuccessScenarios {

        @Test
        @DisplayName("should create deposit successfully when customer is valid and ledger posts")
        void shouldCreateDepositSuccessfully() {
            // Given
            when(customerServicePort.getCustomer(CUSTOMER_ID))
                    .thenReturn(new CustomerInfo(CUSTOMER_ID, "John Doe", true));
            when(accountServicePort.postToLedger(eq(ACCOUNT_ID), eq(AMOUNT), eq(CURRENCY), any()))
                    .thenReturn(new LedgerPostingResult("txn-001", true, "Posted"));

            // When
            DepositResult result = service.execute(validCommand());

            // Then
            assertNotNull(result);
            assertEquals(CUSTOMER_ID, result.customerId());
            assertEquals(ACCOUNT_ID, result.accountId());
            assertEquals(AMOUNT, result.amount());
            assertEquals(CURRENCY, result.currency());
            assertEquals(DepositStatus.COMPLETED, result.status());
            assertNotNull(result.depositId());
            assertNotNull(result.createdAt());
        }

        @Test
        @DisplayName("should persist deposit multiple times during workflow")
        void shouldPersistAtEachStep() {
            // Given
            when(customerServicePort.getCustomer(CUSTOMER_ID))
                    .thenReturn(new CustomerInfo(CUSTOMER_ID, "John Doe", true));
            when(accountServicePort.postToLedger(eq(ACCOUNT_ID), eq(AMOUNT), eq(CURRENCY), any()))
                    .thenReturn(new LedgerPostingResult("txn-001", true, "Posted"));

            // When
            service.execute(validCommand());

            // Then - saved multiple times: initial, after validation, final
            verify(depositRepository, atLeast(3)).save(any(Deposit.class));
        }

        @Test
        @DisplayName("should call customer service with correct customer ID")
        void shouldCallCustomerService() {
            // Given
            when(customerServicePort.getCustomer(CUSTOMER_ID))
                    .thenReturn(new CustomerInfo(CUSTOMER_ID, "John Doe", true));
            when(accountServicePort.postToLedger(eq(ACCOUNT_ID), eq(AMOUNT), eq(CURRENCY), any()))
                    .thenReturn(new LedgerPostingResult("txn-001", true, "Posted"));

            // When
            service.execute(validCommand());

            // Then
            verify(customerServicePort).getCustomer(CUSTOMER_ID);
        }

        @Test
        @DisplayName("should call account service with correct parameters")
        void shouldCallAccountService() {
            // Given
            when(customerServicePort.getCustomer(CUSTOMER_ID))
                    .thenReturn(new CustomerInfo(CUSTOMER_ID, "John Doe", true));
            when(accountServicePort.postToLedger(eq(ACCOUNT_ID), eq(AMOUNT), eq(CURRENCY), any()))
                    .thenReturn(new LedgerPostingResult("txn-001", true, "Posted"));

            // When
            service.execute(validCommand());

            // Then
            verify(accountServicePort).postToLedger(eq(ACCOUNT_ID), eq(AMOUNT), eq(CURRENCY), any());
        }
    }

    @Nested
    @DisplayName("Failure scenarios")
    class FailureScenarios {

        @Test
        @DisplayName("should fail when customer is not found")
        void shouldFailWhenCustomerNotFound() {
            // Given
            when(customerServicePort.getCustomer(CUSTOMER_ID))
                    .thenThrow(new CustomerNotFoundException(CUSTOMER_ID));

            // When
            DepositResult result = service.execute(validCommand());

            // Then
            assertEquals(DepositStatus.FAILED, result.status());
            assertTrue(result.message().contains("Customer not found"));
            verify(accountServicePort, never()).postToLedger(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should fail when customer is inactive")
        void shouldFailWhenCustomerInactive() {
            // Given
            when(customerServicePort.getCustomer(CUSTOMER_ID))
                    .thenReturn(new CustomerInfo(CUSTOMER_ID, "John Doe", false));

            // When
            DepositResult result = service.execute(validCommand());

            // Then
            assertEquals(DepositStatus.FAILED, result.status());
            assertEquals("Customer is not active", result.message());
            verify(accountServicePort, never()).postToLedger(any(), any(), any(), any());
        }

        @Test
        @DisplayName("should fail when ledger posting fails")
        void shouldFailWhenLedgerPostingFails() {
            // Given
            when(customerServicePort.getCustomer(CUSTOMER_ID))
                    .thenReturn(new CustomerInfo(CUSTOMER_ID, "John Doe", true));
            when(accountServicePort.postToLedger(eq(ACCOUNT_ID), eq(AMOUNT), eq(CURRENCY), any()))
                    .thenReturn(new LedgerPostingResult(null, false, "Insufficient limit"));

            // When
            DepositResult result = service.execute(validCommand());

            // Then
            assertEquals(DepositStatus.FAILED, result.status());
            assertEquals("Insufficient limit", result.message());
        }

        @Test
        @DisplayName("should fail when ledger posting throws exception")
        void shouldFailWhenLedgerPostingThrows() {
            // Given
            when(customerServicePort.getCustomer(CUSTOMER_ID))
                    .thenReturn(new CustomerInfo(CUSTOMER_ID, "John Doe", true));
            when(accountServicePort.postToLedger(eq(ACCOUNT_ID), eq(AMOUNT), eq(CURRENCY), any()))
                    .thenThrow(new LedgerPostingException(ACCOUNT_ID, "Connection timeout"));

            // When
            DepositResult result = service.execute(validCommand());

            // Then
            assertEquals(DepositStatus.FAILED, result.status());
            assertTrue(result.message().contains("Failed to post to ledger"));
        }

        @Test
        @DisplayName("should handle unexpected exceptions gracefully")
        void shouldHandleUnexpectedExceptions() {
            // Given
            when(customerServicePort.getCustomer(CUSTOMER_ID))
                    .thenThrow(new RuntimeException("Network failure"));

            // When
            DepositResult result = service.execute(validCommand());

            // Then
            assertEquals(DepositStatus.FAILED, result.status());
            assertEquals("Internal error occurred", result.message());
        }
    }

    @Nested
    @DisplayName("Command validation")
    class CommandValidation {

        @Test
        @DisplayName("should reject null customer ID in command")
        void shouldRejectNullCustomerId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CreateDepositCommand(null, ACCOUNT_ID, AMOUNT, CURRENCY, "desc"));
        }

        @Test
        @DisplayName("should reject blank account ID in command")
        void shouldRejectBlankAccountId() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CreateDepositCommand(CUSTOMER_ID, "", AMOUNT, CURRENCY, "desc"));
        }

        @Test
        @DisplayName("should reject zero amount in command")
        void shouldRejectZeroAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CreateDepositCommand(CUSTOMER_ID, ACCOUNT_ID, BigDecimal.ZERO, CURRENCY, "desc"));
        }

        @Test
        @DisplayName("should reject null currency in command")
        void shouldRejectNullCurrency() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CreateDepositCommand(CUSTOMER_ID, ACCOUNT_ID, AMOUNT, null, "desc"));
        }
    }
}

