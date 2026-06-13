package org.tars.deposit.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregate Root for Deposit bounded context.
 * Encapsulates all deposit business rules.
 */
public class Deposit {

    private final String id;
    private final String customerId;
    private final String accountId;
    private final Money money;
    private final String description;
    private DepositStatus status;
    private String failureReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Deposit(String id, String customerId, String accountId, Money money,
                    String description, DepositStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.accountId = accountId;
        this.money = money;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    /**
     * Factory method - creates a new Deposit in PENDING status.
     */
    public static Deposit create(String customerId, String accountId, Money money, String description) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID must not be blank");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID must not be blank");
        }
        if (!money.isPositive()) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        return new Deposit(
                UUID.randomUUID().toString(),
                customerId,
                accountId,
                money,
                description,
                DepositStatus.PENDING,
                LocalDateTime.now()
        );
    }

    /**
     * Reconstitute from persistence.
     */
    public static Deposit reconstitute(String id, String customerId, String accountId,
                                        Money money, String description,
                                        DepositStatus status, String failureReason,
                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        Deposit deposit = new Deposit(id, customerId, accountId, money, description, status, createdAt);
        deposit.failureReason = failureReason;
        deposit.updatedAt = updatedAt;
        return deposit;
    }

    public void markValidated() {
        assertStatus(DepositStatus.PENDING);
        this.status = DepositStatus.VALIDATED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markLedgerPosted() {
        assertStatus(DepositStatus.VALIDATED);
        this.status = DepositStatus.LEDGER_POSTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        assertStatus(DepositStatus.LEDGER_POSTED);
        this.status = DepositStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = DepositStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    private void assertStatus(DepositStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    String.format("Expected status %s but was %s", expected, this.status));
        }
    }

    // Getters
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getAccountId() { return accountId; }
    public Money getMoney() { return money; }
    public String getDescription() { return description; }
    public DepositStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

