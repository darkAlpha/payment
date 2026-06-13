package org.tars.deposit.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tars.core.exception.BusinessException;
import org.tars.core.exception.ErrorCode;
import org.tars.core.idempotency.Idempotent;
import org.tars.deposit.application.dto.CloseDepositCommand;
import org.tars.deposit.application.dto.CloseDepositResult;
import org.tars.deposit.application.port.input.CloseDepositUseCase;
import org.tars.deposit.application.port.output.LedgerServicePort;
import org.tars.deposit.application.port.output.NotificationPort;
import org.tars.deposit.domain.model.DepositAccount;
import org.tars.deposit.domain.model.Transaction;
import org.tars.deposit.domain.model.TransactionType;
import org.tars.deposit.domain.repository.DepositAccountRepository;
import org.tars.deposit.domain.repository.TransactionRepository;
import org.tars.deposit.infrastructure.audit.AuditLogService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional
public class CloseDepositService implements CloseDepositUseCase {

    private static final Logger log = LoggerFactory.getLogger(CloseDepositService.class);

    private final DepositAccountRepository depositAccountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerServicePort ledgerServicePort;
    private final NotificationPort notificationPort;
    private final AuditLogService auditLogService;

    public CloseDepositService(DepositAccountRepository depositAccountRepository,
                               TransactionRepository transactionRepository,
                               LedgerServicePort ledgerServicePort,
                               NotificationPort notificationPort,
                               AuditLogService auditLogService) {
        this.depositAccountRepository = depositAccountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerServicePort = ledgerServicePort;
        this.notificationPort = notificationPort;
        this.auditLogService = auditLogService;
    }

    @Override
    @Idempotent(key = "#command.idempotencyKey()")
    public CloseDepositResult execute(CloseDepositCommand command) {
        log.info("Closing deposit: depositId={}, customerId={}", command.depositId(), command.customerId());

        DepositAccount account = depositAccountRepository.findById(command.depositId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND, command.depositId()));

        BigDecimal accruedInterest = account.getAccruedInterest();
        BigDecimal finalBalance = account.close();
        depositAccountRepository.save(account);

        // Post to ledger
        ledgerServicePort.postEntry(account.getAccountNumber(), command.depositId(),
                finalBalance, account.getCurrency(), "DEBIT", "DEPOSIT_CLOSE",
                "Deposit closure", command.depositId());

        // Record transaction
        Transaction txn = Transaction.create(account.getId(), command.customerId(),
                TransactionType.DEPOSIT_CLOSE, finalBalance, account.getCurrency(),
                account.getAccountNumber(), null, "Deposit closed: " + command.reason());
        transactionRepository.save(txn);

        // Audit
        auditLogService.logAction(command.depositId(), command.customerId(),
                "DEPOSIT_CLOSED", "ACTIVE", "CLOSED",
                "Final balance: " + finalBalance + ", reason: " + command.reason());

        // Notification
        notificationPort.send(command.customerId(), "SMS", "DEPOSIT_CLOSED",
                Map.of("depositId", command.depositId(), "balance", finalBalance.toPlainString()));

        log.info("Deposit {} closed successfully, final balance={}", command.depositId(), finalBalance);

        return new CloseDepositResult(command.depositId(), "CLOSED", finalBalance,
                accruedInterest, "Deposit closed successfully", LocalDateTime.now());
    }
}
