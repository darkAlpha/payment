package org.tars.deposit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for DepositEntity.
 */
@Repository
public interface DepositJpaRepository extends JpaRepository<DepositEntity, String> {

    List<DepositEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
