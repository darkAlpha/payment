package org.tars.deposit.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tars.deposit.application.dto.AccrualResult;
import org.tars.deposit.application.port.input.AccrualUseCase;
import org.tars.deposit.application.port.output.LedgerServicePort;
import org.tars.deposit.domain.model.DepositAccount;
import org.tars.deposit.domain.model.DepositAccountStatus;
import org.tars.deposit.domain.model.Transaction;
import org.tars.deposit.domain.model.TransactionType;
import org.tars.deposit.domain.repository.DepositAccountRepository;
import org.tars.deposit.domain.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * End-of-Day accrual service. Calculates daily interest for all active deposit accounts.
 */
@Service
public class AccrualService implements AccrualUseCase {

    private static final Logger log = LoggerFactory.getLogger(AccrualService.class);
    private static final int DAYS_IN_YEAR = 365;

    private final DepositAccountRepository depositAccountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerServicePort ledgerServicePort;

    public AccrualService(DepositAccountRepository depositAccountRepository,
                          TransactionRepository transactionRepository,
                          LedgerServicePort ledgerServicePort) {
        this.depositAccountRepository = depositAccountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerServicePort = ledgerServicePort;
    }

    @Override
    @Transactional
    public AccrualResult executeEod() {
        Instant start = Instant.now();
        log.info("EOD Accrual started");

        List<DepositAccount> activeAccounts = depositAccountRepository.findByStatus(DepositAccountStatus.ACTIVE);
        int processed = 0;
        int success = 0;
        int failures = 0;

        for (DepositAccount account : activeAccounts) {
            try {
                BigDecimal dailyInterest = calculateDailyInterest(account);
                if (dailyInterest.compareTo(BigDecimal.ZERO) > 0) {
                    account.accrueInterest(dailyInterest);
                    depositAccountRepository.save(account);

                    // Post accrual to ledger
                    ledgerServicePort.postEntry(account.getAccountNumber(), null,
                            dailyInterest, account.getCurrency(), "CREDIT", "ACCRUAL",
                            "Daily interest accrual", account.getId());

                    // Record transaction
                    Transaction txn = Transaction.create(account.getId(), account.getCustomerId(),
                            TransactionType.INTEREST_ACCRUAL, dailyInterest, account.getCurrency(),
                            "SYSTEM", account.getAccountNumber(), "Daily interest accrual");
                    transactionRepository.save(txn);

                    success++;
                }
                processed++;
            } catch (Exception e) {
                log.error("Accrual failed for account {}: {}", account.getId(), e.getMessage());
                failures++;
                processed++;
            }
        }

        String executionTime = Duration.between(start, Instant.now()).toString();
        log.info("EOD Accrual completed: processed={}, success={}, failures={}, duration={}",
                processed, success, failures, executionTime);

        return new AccrualResult(processed, success, failures, executionTime);
    }

    private BigDecimal calculateDailyInterest(DepositAccount account) {
        // Daily interest = balance * (annual_rate / 365)
        return account.getBalance()
                .multiply(account.getInterestRate())
                .divide(BigDecimal.valueOf(100 * DAYS_IN_YEAR), 6, RoundingMode.HALF_UP);
    }
}
