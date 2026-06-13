package org.tars.deposit.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByDepositIdOrderByTimestampDesc(String depositId);

    List<AuditLogEntity> findByCustomerIdOrderByTimestampDesc(String customerId);
}

