package org.tars.deposit.application.port.output;

/**
 * Output port for external Customer Service interaction.
 * Follows Dependency Inversion Principle - domain depends on abstraction.
 */
public interface CustomerServicePort {

    /**
     * Validates that a customer exists and is active.
     *
     * @param customerId the customer identifier
     * @return customer details if found and active
     * @throws org.tars.deposit.domain.exception.CustomerNotFoundException if customer not found
     */
    CustomerInfo getCustomer(String customerId);

    record CustomerInfo(String customerId, String fullName, boolean active) {}
}

