package org.tars.deposit.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.tars.core.resilience.Resilient;
import org.tars.deposit.application.port.output.LedgerServicePort;

import java.math.BigDecimal;

/**
 * Adapter for ledger service with retry/resilience.
 */
@Component
public class LedgerServiceAdapter implements LedgerServicePort {

    private static final Logger log = LoggerFactory.getLogger(LedgerServiceAdapter.class);

    @Override
    @Resilient(name = "ledger-service", maxRetries = 3, retryDelayMs = 500)
    public LedgerResult postEntry(String accountId, String transactionId, BigDecimal amount,
                                  String currency, String entryType, String operationType,
                                  String description, String referenceId) {
        log.info("LEDGER: {} {} {} {} for account={}, ref={}",
                entryType, amount, currency, operationType, accountId, referenceId);

        // TODO: Replace with actual Dubbo call to LedgerService
        // For now, simulate successful posting
        return new LedgerResult("entry-" + System.currentTimeMillis(), accountId,
                BigDecimal.ZERO, true, "Posted successfully");
    }
}
