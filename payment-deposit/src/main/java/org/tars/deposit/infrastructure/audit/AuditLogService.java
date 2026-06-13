package org.tars.deposit.infrastructure.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Audit logging service that persists audit trail for all deposit operations.
 * Uses REQUIRES_NEW propagation to ensure audit is saved even if main transaction rolls back.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogJpaRepository auditLogRepository;

    public AuditLogService(AuditLogJpaRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String depositId, String customerId, String action,
                          String statusBefore, String statusAfter, String details) {
        log.debug("AUDIT: depositId={}, action={}, {} -> {}, details={}",
                depositId, action, statusBefore, statusAfter, details);

        AuditLogEntity entry = AuditLogEntity.builder()
                .depositId(depositId)
                .customerId(customerId)
                .action(action)
                .statusBefore(statusBefore)
                .statusAfter(statusAfter)
                .details(details)
                .performedBy("SYSTEM")
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(entry);

        log.info("AUDIT LOG: [{}] deposit={} customer={} {} -> {}",
                action, depositId, customerId, statusBefore, statusAfter);
    }
}

