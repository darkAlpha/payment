package org.tars.deposit.domain.model;

/**
 * Value Object representing the lifecycle status of a Deposit.
 */
public enum DepositStatus {
    PENDING,
    VALIDATED,
    LEDGER_POSTED,
    COMPLETED,
    FAILED
}

