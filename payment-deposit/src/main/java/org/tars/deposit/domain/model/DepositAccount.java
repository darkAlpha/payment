package org.tars.deposit.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregate Root representing a deposit account.
 */
public class DepositAccount {

    private final String id;
    private final String customerId;
    private final String accountNumber;
    private BigDecimal balance;
    private BigDecimal interestRate;
    private final String currency;
    private final int termMonths;
    private final LocalDate openDate;
    private LocalDate maturityDate;
    private LocalDate closeDate;
    private DepositAccountStatus status;
    private BigDecimal accruedInterest;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private DepositAccount(String id, String customerId, String accountNumber,
                           BigDecimal balance, BigDecimal interestRate, String currency,
                           int termMonths, LocalDate openDate, DepositAccountStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.interestRate = interestRate;
        this.currency = currency;
        this.termMonths = termMonths;
        this.openDate = openDate;
        this.maturityDate = openDate.plusMonths(termMonths);
        this.status = status;
        this.accruedInterest = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static DepositAccount open(String customerId, BigDecimal amount, BigDecimal interestRate,
                                       String currency, int termMonths) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Initial amount must be positive");
        if (termMonths <= 0)
            throw new IllegalArgumentException("Term must be positive");

        String id = UUID.randomUUID().toString();
        String accountNumber = "DEP-" + System.currentTimeMillis();

        return new DepositAccount(id, customerId, accountNumber, amount, interestRate,
                currency, termMonths, LocalDate.now(), DepositAccountStatus.ACTIVE);
    }

    public static DepositAccount reconstitute(String id, String customerId, String accountNumber,
                                               BigDecimal balance, BigDecimal interestRate, String currency,
                                               int termMonths, LocalDate openDate, LocalDate maturityDate,
                                               LocalDate closeDate, DepositAccountStatus status,
                                               BigDecimal accruedInterest,
                                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        DepositAccount acc = new DepositAccount(id, customerId, accountNumber, balance,
                interestRate, currency, termMonths, openDate, status);
        acc.maturityDate = maturityDate;
        acc.closeDate = closeDate;
        acc.accruedInterest = accruedInterest;
        acc.updatedAt = updatedAt;
        return acc;
    }

    public void credit(BigDecimal amount) {
        assertActive();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Credit amount must be positive");
        this.balance = this.balance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void debit(BigDecimal amount) {
        assertActive();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Debit amount must be positive");
        if (this.balance.compareTo(amount) < 0) throw new IllegalStateException("Insufficient balance");
        this.balance = this.balance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void accrueInterest(BigDecimal dailyInterest) {
        assertActive();
        this.accruedInterest = this.accruedInterest.add(dailyInterest);
        this.balance = this.balance.add(dailyInterest);
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal close() {
        assertActive();
        this.status = DepositAccountStatus.CLOSED;
        this.closeDate = LocalDate.now();
        this.updatedAt = LocalDateTime.now();
        BigDecimal finalBalance = this.balance;
        this.balance = BigDecimal.ZERO;
        return finalBalance;
    }

    private void assertActive() {
        if (this.status != DepositAccountStatus.ACTIVE) {
            throw new IllegalStateException("Deposit account is not active: " + this.status);
        }
    }

    // Getters
    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getInterestRate() { return interestRate; }
    public String getCurrency() { return currency; }
    public int getTermMonths() { return termMonths; }
    public LocalDate getOpenDate() { return openDate; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public LocalDate getCloseDate() { return closeDate; }
    public DepositAccountStatus getStatus() { return status; }
    public BigDecimal getAccruedInterest() { return accruedInterest; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
