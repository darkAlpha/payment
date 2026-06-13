package org.tars.deposit.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    @Id
    private String id;

    @Column(name = "deposit_account_id", nullable = false)
    private String depositAccountId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private String type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "target_id")
    private String targetId;

    private String description;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
