package org.tars.deposit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findByDepositAccountIdOrderByCreatedAtDesc(String depositAccountId);

    List<TransactionEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
