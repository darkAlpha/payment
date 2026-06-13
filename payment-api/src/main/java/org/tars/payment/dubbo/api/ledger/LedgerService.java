package org.tars.payment.dubbo.api.ledger;

/**
 * Dubbo service API for account ledger operations.
 */
public interface LedgerService {

    LedgerEntryResponseDto postEntry(LedgerEntryRequestDto request);
}
