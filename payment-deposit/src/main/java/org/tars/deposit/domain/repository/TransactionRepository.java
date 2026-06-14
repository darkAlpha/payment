package org.tars.deposit.domain.repository;

import org.tars.deposit.domain.model.Transaction;

import java.util.List;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    List<Transaction> findByDepositAccountId(String depositAccountId);

    List<Transaction> findByCustomerId(String customerId);
}
