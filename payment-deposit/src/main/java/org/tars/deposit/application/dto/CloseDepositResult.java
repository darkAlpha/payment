package org.tars.deposit.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CloseDepositResult(
        String depositId,
        String status,
        BigDecimal finalBalance,
        BigDecimal accruedInterest,
        String message,
        LocalDateTime closedAt
) {}
