package org.tars.deposit.application.port.output;

import java.math.BigDecimal;

/**
 * Output port for external Account Service (ledger) interaction.
 * Follows Dependency Inversion Principle.
 */
public interface AccountServicePort {

    /**
     * Posts a credit entry to the account ledger.
     *
     * @param accountId  the target account
     * @param amount     the deposit amount
     * @param currency   the currency code
     * @param reference  the deposit reference ID
     * @return ledger posting result
     */
    LedgerPostingResult postToLedger(String accountId, BigDecimal amount, String currency, String reference);

    record LedgerPostingResult(String transactionId, boolean success, String message) {}
}

