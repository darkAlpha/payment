package org.tars.deposit.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResult(
        String transactionId,
        String sourceId,
        String targetId,
        BigDecimal amount,
        String currency,
        String status,
        String message,
        LocalDateTime createdAt
) {}
