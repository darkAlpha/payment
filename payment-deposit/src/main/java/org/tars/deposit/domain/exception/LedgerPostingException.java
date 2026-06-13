package org.tars.deposit.domain.exception;

/**
 * Exception thrown when ledger posting fails in the account service.
 */
public class LedgerPostingException extends RuntimeException {

    public LedgerPostingException(String accountId, String reason) {
        super("Failed to post to ledger for account " + accountId + ": " + reason);
    }
}

