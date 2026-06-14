package org.tars.deposit.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity representing a transaction on a deposit account.
 */
public class Transaction {

    private final String id;
    private final String depositAccountId;
    private final String customerId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final String currency;
    private final String sourceId;
    private final String targetId;
    private final String description;
    private final String referenceId;
    private final LocalDateTime createdAt;

    private Transaction(String id, String depositAccountId, String customerId,
                        TransactionType type, BigDecimal amount, String currency,
                        String sourceId, String targetId, String description,
                        String referenceId, LocalDateTime createdAt) {
        this.id = id;
        this.depositAccountId = depositAccountId;
        this.customerId = customerId;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.description = description;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
    }

    public static Transaction create(String depositAccountId, String customerId,
                                      TransactionType type, BigDecimal amount, String currency,
                                      String sourceId, String targetId, String description) {
        return new Transaction(UUID.randomUUID().toString(), depositAccountId, customerId,
                type, amount, currency, sourceId, targetId, description, null, LocalDateTime.now());
    }

    public static Transaction reconstitute(String id, String depositAccountId, String customerId,
                                            TransactionType type, BigDecimal amount, String currency,
                                            String sourceId, String targetId, String description,
                                            String referenceId, LocalDateTime createdAt) {
        return new Transaction(id, depositAccountId, customerId, type, amount, currency,
                sourceId, targetId, description, referenceId, createdAt);
    }

    // Getters
    public String getId() { return id; }
    public String getDepositAccountId() { return depositAccountId; }
    public String getCustomerId() { return customerId; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getSourceId() { return sourceId; }
    public String getTargetId() { return targetId; }
    public String getDescription() { return description; }
    public String getReferenceId() { return referenceId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
