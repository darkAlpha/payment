package org.tars.deposit.application.port.output;

import java.math.BigDecimal;

/**
 * Port for ledger/account service integration.
 */
public interface LedgerServicePort {

    LedgerResult postEntry(String accountId, String transactionId, BigDecimal amount,
                           String currency, String entryType, String operationType,
                           String description, String referenceId);

    record LedgerResult(String entryId, String accountId, BigDecimal newBalance,
                        boolean success, String message) {}
}
