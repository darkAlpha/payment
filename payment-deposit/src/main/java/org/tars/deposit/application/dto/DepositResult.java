package org.tars.deposit.application.dto;

import org.tars.deposit.domain.model.Deposit;
import org.tars.deposit.domain.model.DepositStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Result object returned after deposit creation.
 */
public record DepositResult(
        String depositId,
        String customerId,
        String accountId,
        BigDecimal amount,
        String currency,
        DepositStatus status,
        String message,
        LocalDateTime createdAt
) {
    public static DepositResult fromDomain(Deposit deposit, String message) {
        return new DepositResult(
                deposit.getId(),
                deposit.getCustomerId(),
                deposit.getAccountId(),
                deposit.getMoney().amount(),
                deposit.getMoney().currency().getCurrencyCode(),
                deposit.getStatus(),
                message,
                deposit.getCreatedAt()
        );
    }
}

