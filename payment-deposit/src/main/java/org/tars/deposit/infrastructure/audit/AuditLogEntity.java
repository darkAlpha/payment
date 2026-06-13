package org.tars.deposit.infrastructure.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity for audit log entries.
 * Records all significant operations for compliance and traceability.
 */
@Entity
@Table(name = "deposit_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deposit_id", nullable = false)
    private String depositId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "status_before")
    private String statusBefore;

    @Column(name = "status_after")
    private String statusAfter;

    @Column(name = "details", length = 1024)
    private String details;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}

