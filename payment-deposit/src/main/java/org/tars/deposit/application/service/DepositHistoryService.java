package org.tars.deposit.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tars.deposit.application.dto.DepositResult;
import org.tars.deposit.application.port.input.GetDepositHistoryUseCase;
import org.tars.deposit.domain.model.Deposit;
import org.tars.deposit.domain.repository.DepositRepository;

import java.util.List;

/**
 * Application service for querying deposit transaction history.
 */
@Service
@Transactional(readOnly = true)
public class DepositHistoryService implements GetDepositHistoryUseCase {

    private static final Logger log = LoggerFactory.getLogger(DepositHistoryService.class);

    private final DepositRepository depositRepository;

    public DepositHistoryService(DepositRepository depositRepository) {
        this.depositRepository = depositRepository;
    }

    @Override
    public List<DepositResult> execute(String customerId) {
        log.info("Fetching deposit history for customerId={}", customerId);

        List<Deposit> deposits = depositRepository.findByCustomerId(customerId);

        log.info("Found {} deposit records for customerId={}", deposits.size(), customerId);

        return deposits.stream()
                .map(deposit -> DepositResult.fromDomain(deposit, deposit.getStatus().name()))
                .toList();
    }
}

