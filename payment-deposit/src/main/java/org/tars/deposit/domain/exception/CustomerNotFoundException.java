package org.tars.deposit.domain.exception;

/**
 * Exception thrown when a customer is not found by external customer service.
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String customerId) {
        super("Customer not found: " + customerId);
    }
}

