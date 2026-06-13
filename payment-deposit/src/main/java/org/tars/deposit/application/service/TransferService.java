package org.tars.deposit.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tars.core.exception.BusinessException;
import org.tars.core.exception.ErrorCode;
import org.tars.core.idempotency.Idempotent;
import org.tars.deposit.application.dto.TransferCommand;
import org.tars.deposit.application.dto.TransferResult;
import org.tars.deposit.application.port.input.TransferUseCase;
import org.tars.deposit.application.port.output.LedgerServicePort;
import org.tars.deposit.application.port.output.NotificationPort;
import org.tars.deposit.domain.model.DepositAccount;
import org.tars.deposit.domain.model.Transaction;
import org.tars.deposit.domain.model.TransactionType;
import org.tars.deposit.domain.repository.DepositAccountRepository;
import org.tars.deposit.domain.repository.TransactionRepository;
import org.tars.deposit.infrastructure.audit.AuditLogService;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional
public class TransferService implements TransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final DepositAccountRepository depositAccountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerServicePort ledgerServicePort;
    private final NotificationPort notificationPort;
    private final AuditLogService auditLogService;

    public TransferService(DepositAccountRepository depositAccountRepository,
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
    public TransferResult execute(TransferCommand command) {
        log.info("Transfer: type={}, source={}, target={}, amount={} {}",
                command.type(), command.sourceId(), command.targetId(), command.amount(), command.currency());

        return switch (command.type()) {
            case DEPOSIT_TO_CARD -> executeDepositToCard(command);
            case CARD_TO_DEPOSIT -> executeCardToDeposit(command);
            case DEPOSIT_TO_DEPOSIT -> executeDepositToDeposit(command);
            default -> throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        };
    }

    private TransferResult executeDepositToCard(TransferCommand command) {
        DepositAccount source = getAccount(command.sourceId());
        source.debit(command.amount());
        depositAccountRepository.save(source);

        // Post debit to ledger
        LedgerServicePort.LedgerResult debitResult = ledgerServicePort.postEntry(
                source.getAccountNumber(), null, command.amount(), command.currency(),
                "DEBIT", "TRANSFER", "Transfer to card: " + command.targetId(), command.idempotencyKey());

        if (!debitResult.success()) {
            throw new BusinessException(ErrorCode.LEDGER_POSTING_FAILED, source.getAccountNumber(), debitResult.message());
        }

        // Record transaction
        Transaction txn = Transaction.create(source.getId(), command.customerId(),
                TransactionType.DEPOSIT_TO_CARD, command.amount(), command.currency(),
                command.sourceId(), command.targetId(), command.description());
        transactionRepository.save(txn);

        auditLogService.logAction(source.getId(), command.customerId(),
                "DEPOSIT_TO_CARD", "ACTIVE", "ACTIVE",
                String.format("amount=%s, target_card=%s", command.amount(), command.targetId()));

        notificationPort.send(command.customerId(), "PUSH", "TRANSFER_COMPLETED",
                Map.of("amount", command.amount().toPlainString(), "target", command.targetId()));

        log.info("Deposit-to-card completed: txn={}", txn.getId());
        return new TransferResult(txn.getId(), command.sourceId(), command.targetId(),
                command.amount(), command.currency(), "COMPLETED", "Transfer successful", LocalDateTime.now());
    }

    private TransferResult executeCardToDeposit(TransferCommand command) {
        DepositAccount target = getAccount(command.targetId());
        target.credit(command.amount());
        depositAccountRepository.save(target);

        // Post credit to ledger
        ledgerServicePort.postEntry(target.getAccountNumber(), null, command.amount(), command.currency(),
                "CREDIT", "TRANSFER", "Transfer from card: " + command.sourceId(), command.idempotencyKey());

        Transaction txn = Transaction.create(target.getId(), command.customerId(),
                TransactionType.CARD_TO_DEPOSIT, command.amount(), command.currency(),
                command.sourceId(), command.targetId(), command.description());
        transactionRepository.save(txn);

        auditLogService.logAction(target.getId(), command.customerId(),
                "CARD_TO_DEPOSIT", "ACTIVE", "ACTIVE",
                String.format("amount=%s, source_card=%s", command.amount(), command.sourceId()));

        notificationPort.send(command.customerId(), "PUSH", "TRANSFER_COMPLETED",
                Map.of("amount", command.amount().toPlainString(), "source", command.sourceId()));

        log.info("Card-to-deposit completed: txn={}", txn.getId());
        return new TransferResult(txn.getId(), command.sourceId(), command.targetId(),
                command.amount(), command.currency(), "COMPLETED", "Transfer successful", LocalDateTime.now());
    }

    private TransferResult executeDepositToDeposit(TransferCommand command) {
        DepositAccount source = getAccount(command.sourceId());
        DepositAccount target = getAccount(command.targetId());

        source.debit(command.amount());
        target.credit(command.amount());
        depositAccountRepository.save(source);
        depositAccountRepository.save(target);

        // Double-entry ledger
        ledgerServicePort.postEntry(source.getAccountNumber(), null, command.amount(), command.currency(),
                "DEBIT", "TRANSFER", "Transfer to " + target.getAccountNumber(), command.idempotencyKey());
        ledgerServicePort.postEntry(target.getAccountNumber(), null, command.amount(), command.currency(),
                "CREDIT", "TRANSFER", "Transfer from " + source.getAccountNumber(), command.idempotencyKey());

        Transaction txn = Transaction.create(source.getId(), command.customerId(),
                TransactionType.DEPOSIT_TO_DEPOSIT, command.amount(), command.currency(),
                command.sourceId(), command.targetId(), command.description());
        transactionRepository.save(txn);

        auditLogService.logAction(source.getId(), command.customerId(),
                "DEPOSIT_TO_DEPOSIT", "ACTIVE", "ACTIVE",
                String.format("amount=%s, target=%s", command.amount(), command.targetId()));

        notificationPort.send(command.customerId(), "PUSH", "TRANSFER_COMPLETED",
                Map.of("amount", command.amount().toPlainString(), "type", "deposit-to-deposit"));

        log.info("Deposit-to-deposit completed: txn={}", txn.getId());
        return new TransferResult(txn.getId(), command.sourceId(), command.targetId(),
                command.amount(), command.currency(), "COMPLETED", "Transfer successful", LocalDateTime.now());
    }

    private DepositAccount getAccount(String id) {
        return depositAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND, id));
    }
}
