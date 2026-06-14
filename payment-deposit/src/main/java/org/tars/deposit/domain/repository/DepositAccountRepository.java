package org.tars.deposit.domain.repository;

import org.tars.deposit.domain.model.DepositAccount;
import org.tars.deposit.domain.model.DepositAccountStatus;

import java.util.List;
import java.util.Optional;

public interface DepositAccountRepository {

    DepositAccount save(DepositAccount account);

    Optional<DepositAccount> findById(String id);

    Optional<DepositAccount> findByAccountNumber(String accountNumber);

    List<DepositAccount> findByCustomerId(String customerId);

    List<DepositAccount> findByStatus(DepositAccountStatus status);
}
