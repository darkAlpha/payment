package org.tars.deposit.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.tars.deposit.domain.model.Deposit;
import org.tars.deposit.domain.model.DepositStatus;
import org.tars.deposit.domain.model.Money;
import org.tars.deposit.domain.repository.DepositRepository;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing the domain DepositRepository port using JPA.
 * Maps between domain model and persistence entity.
 */
@Component
public class DepositRepositoryAdapter implements DepositRepository {

    private final DepositJpaRepository jpaRepository;

    public DepositRepositoryAdapter(DepositJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Deposit save(Deposit deposit) {
        DepositEntity entity = toEntity(deposit);
        jpaRepository.save(entity);
        return deposit;
    }

    @Override
    public Optional<Deposit> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Deposit> findByCustomerId(String customerId) {
        return jpaRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private DepositEntity toEntity(Deposit deposit) {
        return DepositEntity.builder()
                .id(deposit.getId())
                .customerId(deposit.getCustomerId())
                .accountId(deposit.getAccountId())
                .amount(deposit.getMoney().amount())
                .currency(deposit.getMoney().currency().getCurrencyCode())
                .description(deposit.getDescription())
                .status(deposit.getStatus().name())
                .failureReason(deposit.getFailureReason())
                .createdAt(deposit.getCreatedAt())
                .updatedAt(deposit.getUpdatedAt())
                .build();
    }

    private Deposit toDomain(DepositEntity entity) {
        Money money = Money.of(entity.getAmount(), entity.getCurrency());
        return Deposit.reconstitute(
                entity.getId(),
                entity.getCustomerId(),
                entity.getAccountId(),
                money,
                entity.getDescription(),
                DepositStatus.valueOf(entity.getStatus()),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

