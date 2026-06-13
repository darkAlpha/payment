package org.tars.core.exception;

/**
 * Centralized error codes for i18n message resolution.
 */
public final class ErrorCode {

    private ErrorCode() {}

    // Deposit errors
    public static final String DEPOSIT_NOT_FOUND = "error.deposit.not_found";
    public static final String DEPOSIT_ALREADY_CLOSED = "error.deposit.already_closed";
    public static final String DEPOSIT_INSUFFICIENT_BALANCE = "error.deposit.insufficient_balance";
    public static final String DEPOSIT_INVALID_AMOUNT = "error.deposit.invalid_amount";
    public static final String DEPOSIT_INVALID_TERM = "error.deposit.invalid_term";

    // Customer errors
    public static final String CUSTOMER_NOT_FOUND = "error.customer.not_found";
    public static final String CUSTOMER_INACTIVE = "error.customer.inactive";

    // Account/Ledger errors
    public static final String LEDGER_POSTING_FAILED = "error.ledger.posting_failed";
    public static final String ACCOUNT_NOT_FOUND = "error.account.not_found";

    // Card errors
    public static final String CARD_NOT_FOUND = "error.card.not_found";
    public static final String CARD_EXPIRED = "error.card.expired";

    // Idempotency errors
    public static final String DUPLICATE_REQUEST = "error.idempotency.duplicate";

    // Security errors
    public static final String ACCESS_DENIED = "error.security.access_denied";
    public static final String TOKEN_EXPIRED = "error.security.token_expired";
    public static final String TOKEN_INVALID = "error.security.token_invalid";

    // Generic
    public static final String INTERNAL_ERROR = "error.internal";
    public static final String EXTERNAL_SERVICE_UNAVAILABLE = "error.external.unavailable";
}
