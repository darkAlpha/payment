package org.tars.deposit.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.tars.deposit.domain.model.DepositAccount;
import org.tars.deposit.domain.model.DepositAccountStatus;
import org.tars.deposit.domain.repository.DepositAccountRepository;

import java.util.List;
import java.util.Optional;

@Component
public class DepositAccountRepositoryAdapter implements DepositAccountRepository {

    private final DepositAccountJpaRepository jpa;

    public DepositAccountRepositoryAdapter(DepositAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DepositAccount save(DepositAccount account) {
        jpa.save(toEntity(account));
        return account;
    }

    @Override
    public Optional<DepositAccount> findById(String id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<DepositAccount> findByAccountNumber(String accountNumber) {
        return jpa.findByAccountNumber(accountNumber).map(this::toDomain);
    }

    @Override
    public List<DepositAccount> findByCustomerId(String customerId) {
        return jpa.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<DepositAccount> findByStatus(DepositAccountStatus status) {
        return jpa.findByStatus(status.name()).stream().map(this::toDomain).toList();
    }

    private DepositAccountEntity toEntity(DepositAccount a) {
        return DepositAccountEntity.builder()
                .id(a.getId()).customerId(a.getCustomerId()).accountNumber(a.getAccountNumber())
                .balance(a.getBalance()).interestRate(a.getInterestRate()).currency(a.getCurrency())
                .termMonths(a.getTermMonths()).openDate(a.getOpenDate()).maturityDate(a.getMaturityDate())
                .closeDate(a.getCloseDate()).status(a.getStatus().name())
                .accruedInterest(a.getAccruedInterest())
                .createdAt(a.getCreatedAt()).updatedAt(a.getUpdatedAt())
                .build();
    }

    private DepositAccount toDomain(DepositAccountEntity e) {
        return DepositAccount.reconstitute(e.getId(), e.getCustomerId(), e.getAccountNumber(),
                e.getBalance(), e.getInterestRate(), e.getCurrency(), e.getTermMonths(),
                e.getOpenDate(), e.getMaturityDate(), e.getCloseDate(),
                DepositAccountStatus.valueOf(e.getStatus()), e.getAccruedInterest(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
