package org.tars.deposit.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositAccountEntity {

    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "open_date", nullable = false)
    private LocalDate openDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "close_date")
    private LocalDate closeDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private String status;

    @Column(name = "accrued_interest", precision = 19, scale = 6)
    private BigDecimal accruedInterest;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
