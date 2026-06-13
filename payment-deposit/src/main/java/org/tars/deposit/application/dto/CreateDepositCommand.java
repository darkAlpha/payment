package org.tars.deposit.application.dto;

import java.math.BigDecimal;

/**
 * Command object for creating a deposit.
 * Immutable record follows CQRS command pattern.
 */
public record CreateDepositCommand(
        String customerId,
        String accountId,
        BigDecimal amount,
        String currency,
        String description
) {
    public CreateDepositCommand {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        if (accountId == null || accountId.isBlank()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
    }
}

