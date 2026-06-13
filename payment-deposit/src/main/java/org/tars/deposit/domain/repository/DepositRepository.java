package org.tars.deposit.domain.repository;

import org.tars.deposit.domain.model.Deposit;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Deposit aggregate persistence.
 * Defined in domain, implemented in infrastructure.
 */
public interface DepositRepository {

    Deposit save(Deposit deposit);

    Optional<Deposit> findById(String id);

    List<Deposit> findByCustomerId(String customerId);
}

