package org.tars.deposit.domain.model;

public enum TransactionType {
    DEPOSIT_OPEN,
    DEPOSIT_CLOSE,
    DEPOSIT_TO_CARD,
    CARD_TO_DEPOSIT,
    DEPOSIT_TO_DEPOSIT,
    INTEREST_ACCRUAL
}
