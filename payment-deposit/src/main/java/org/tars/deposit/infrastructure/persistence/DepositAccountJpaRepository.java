package org.tars.deposit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepositAccountJpaRepository extends JpaRepository<DepositAccountEntity, String> {

    Optional<DepositAccountEntity> findByAccountNumber(String accountNumber);

    List<DepositAccountEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<DepositAccountEntity> findByStatus(String status);
}
