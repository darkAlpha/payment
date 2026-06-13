package org.tars.deposit.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.tars.deposit.application.port.output.AccountServicePort;
import org.tars.deposit.domain.exception.LedgerPostingException;

import java.math.BigDecimal;

/**
 * Adapter for external Account Service (ledger).
 * Implements the output port using WebClient for HTTP communication.
 */
@Component
public class AccountServiceAdapter implements AccountServicePort {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceAdapter.class);

    private final WebClient webClient;

    public AccountServiceAdapter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("${account.service.url:http://localhost:8082}")
                .build();
    }

    @Override
    public LedgerPostingResult postToLedger(String accountId, BigDecimal amount, String currency, String reference) {
        log.info("Posting to ledger: accountId={}, amount={} {}, ref={}",
                accountId, amount, currency, reference);
        try {
            LedgerRequest request = new LedgerRequest(accountId, amount, currency, reference, "CREDIT");

            LedgerResponse response = webClient.post()
                    .uri("/api/v1/accounts/{accountId}/ledger", accountId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LedgerResponse.class)
                    .block();

            if (response == null) {
                throw new LedgerPostingException(accountId, "No response from account service");
            }

            return new LedgerPostingResult(response.transactionId(), response.success(), response.message());

        } catch (LedgerPostingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error posting to ledger for accountId={}", accountId, e);
            throw new LedgerPostingException(accountId, e.getMessage());
        }
    }

    private record LedgerRequest(String accountId, BigDecimal amount, String currency,
                                  String reference, String entryType) {}

    private record LedgerResponse(String transactionId, boolean success, String message) {}
}

