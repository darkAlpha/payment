package org.tars.deposit.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.tars.deposit.application.port.output.CustomerServicePort;
import org.tars.deposit.domain.exception.CustomerNotFoundException;

/**
 * Adapter for external Customer Service.
 * Implements the output port using WebClient for HTTP communication.
 */
@Component
public class CustomerServiceAdapter implements CustomerServicePort {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceAdapter.class);

    private final WebClient webClient;

    public CustomerServiceAdapter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("${customer.service.url:http://localhost:8081}")
                .build();
    }

    @Override
    public CustomerInfo getCustomer(String customerId) {
        log.info("Fetching customer info for customerId={}", customerId);
        try {
            CustomerResponse response = webClient.get()
                    .uri("/api/v1/customers/{id}", customerId)
                    .retrieve()
                    .bodyToMono(CustomerResponse.class)
                    .block();

            if (response == null) {
                throw new CustomerNotFoundException(customerId);
            }

            return new CustomerInfo(response.id(), response.fullName(), response.active());

        } catch (WebClientResponseException.NotFound e) {
            throw new CustomerNotFoundException(customerId);
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling customer service for customerId={}", customerId, e);
            throw new CustomerNotFoundException(customerId);
        }
    }

    private record CustomerResponse(String id, String fullName, boolean active) {}
}

