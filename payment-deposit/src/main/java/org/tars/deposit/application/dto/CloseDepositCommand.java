package org.tars.deposit.application.dto;

public record CloseDepositCommand(
        String depositId,
        String customerId,
        String reason,
        String idempotencyKey
) {}
