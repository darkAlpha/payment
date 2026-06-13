package org.tars.deposit.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.tars.deposit.domain.model.Transaction;
import org.tars.deposit.domain.model.TransactionType;
import org.tars.deposit.domain.repository.TransactionRepository;

import java.util.List;

@Component
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpa;

    public TransactionRepositoryAdapter(TransactionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Transaction save(Transaction txn) {
        jpa.save(toEntity(txn));
        return txn;
    }

    @Override
    public List<Transaction> findByDepositAccountId(String depositAccountId) {
        return jpa.findByDepositAccountIdOrderByCreatedAtDesc(depositAccountId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Transaction> findByCustomerId(String customerId) {
        return jpa.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toDomain).toList();
    }

    private TransactionEntity toEntity(Transaction t) {
        return TransactionEntity.builder()
                .id(t.getId()).depositAccountId(t.getDepositAccountId()).customerId(t.getCustomerId())
                .type(t.getType().name()).amount(t.getAmount()).currency(t.getCurrency())
                .sourceId(t.getSourceId()).targetId(t.getTargetId())
                .description(t.getDescription()).referenceId(t.getReferenceId())
                .createdAt(t.getCreatedAt()).build();
    }

    private Transaction toDomain(TransactionEntity e) {
        return Transaction.reconstitute(e.getId(), e.getDepositAccountId(), e.getCustomerId(),
                TransactionType.valueOf(e.getType()), e.getAmount(), e.getCurrency(),
                e.getSourceId(), e.getTargetId(), e.getDescription(),
                e.getReferenceId(), e.getCreatedAt());
    }
}
