package org.tars.deposit.domain.event;

import org.tars.deposit.domain.model.Money;

import java.time.LocalDateTime;

/**
 * Domain event raised when a deposit is successfully created.
 */
public record DepositCreatedEvent(
        String depositId,
        String customerId,
        String accountId,
        Money money,
        LocalDateTime occurredAt
) {
    public static DepositCreatedEvent from(String depositId, String customerId,
                                            String accountId, Money money) {
        return new DepositCreatedEvent(depositId, customerId, accountId, money, LocalDateTime.now());
    }
}

