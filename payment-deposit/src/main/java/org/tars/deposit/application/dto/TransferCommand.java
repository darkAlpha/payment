package org.tars.deposit.application.dto;

import org.tars.deposit.domain.model.TransactionType;

import java.math.BigDecimal;

public record TransferCommand(
        String customerId,
        String sourceId,
        String targetId,
        BigDecimal amount,
        String currency,
        String description,
        TransactionType type,
        String idempotencyKey
) {}
