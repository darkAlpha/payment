package org.tars.deposit.application.dto;

public record AccrualResult(
        int processedAccounts,
        int successCount,
        int failureCount,
        String executionTime
) {}
