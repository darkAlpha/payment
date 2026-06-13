package org.tars.deposit.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tars.deposit.application.dto.CreateDepositCommand;
import org.tars.deposit.application.dto.DepositResult;
import org.tars.deposit.application.port.input.CreateDepositUseCase;
import org.tars.deposit.application.port.output.AccountServicePort;
import org.tars.deposit.application.port.output.CustomerServicePort;
import org.tars.deposit.domain.exception.CustomerNotFoundException;
import org.tars.deposit.domain.exception.LedgerPostingException;
import org.tars.deposit.domain.model.Deposit;
import org.tars.deposit.domain.model.DepositStatus;
import org.tars.deposit.domain.model.Money;
import org.tars.deposit.domain.repository.DepositRepository;
import org.tars.deposit.infrastructure.audit.AuditLogService;

/**
 * Application service orchestrating the deposit creation use case.
 * Coordinates between domain model and external services (ports).
 * Single Responsibility: orchestrates the deposit workflow.
 */
@Service
@Transactional
public class DepositApplicationService implements CreateDepositUseCase {

    private static final Logger log = LoggerFactory.getLogger(DepositApplicationService.class);

    private final DepositRepository depositRepository;
    private final CustomerServicePort customerServicePort;
    private final AccountServicePort accountServicePort;
    private final AuditLogService auditLogService;

    public DepositApplicationService(DepositRepository depositRepository,
                                     CustomerServicePort customerServicePort,
                                     AccountServicePort accountServicePort,
                                     AuditLogService auditLogService) {
        this.depositRepository = depositRepository;
        this.customerServicePort = customerServicePort;
        this.accountServicePort = accountServicePort;
        this.auditLogService = auditLogService;
    }

    @Override
    public DepositResult execute(CreateDepositCommand command) {
        log.info("Creating deposit for customer={}, account={}, amount={} {}",
                command.customerId(), command.accountId(), command.amount(), command.currency());

        // 1. Create domain aggregate
        Money money = Money.of(command.amount(), command.currency());
        Deposit deposit = Deposit.create(command.customerId(), command.accountId(), money, command.description());
        deposit = depositRepository.save(deposit);

        auditLogService.logAction(deposit.getId(), command.customerId(),
                "DEPOSIT_CREATED", null, DepositStatus.PENDING.name(),
                String.format("amount=%s %s, account=%s", command.amount(), command.currency(), command.accountId()));

        try {
            // 2. Validate customer via external service
            log.debug("Validating customer={} via customer service", command.customerId());
            CustomerServicePort.CustomerInfo customerInfo = customerServicePort.getCustomer(command.customerId());
            if (!customerInfo.active()) {
                deposit.markFailed("Customer is not active");
                depositRepository.save(deposit);
                auditLogService.logAction(deposit.getId(), command.customerId(),
                        "CUSTOMER_VALIDATION_FAILED", DepositStatus.PENDING.name(), DepositStatus.FAILED.name(),
                        "Customer is not active");
                return DepositResult.fromDomain(deposit, "Customer is not active");
            }
            deposit.markValidated();
            deposit = depositRepository.save(deposit);

            auditLogService.logAction(deposit.getId(), command.customerId(),
                    "CUSTOMER_VALIDATED", DepositStatus.PENDING.name(), DepositStatus.VALIDATED.name(),
                    "Customer validated: " + customerInfo.fullName());

            // 3. Post to ledger via account service
            log.debug("Posting to ledger: account={}, amount={} {}", command.accountId(), command.amount(), command.currency());
            AccountServicePort.LedgerPostingResult ledgerResult = accountServicePort.postToLedger(
                    command.accountId(),
                    command.amount(),
                    command.currency(),
                    deposit.getId()
            );

            if (!ledgerResult.success()) {
                deposit.markFailed("Ledger posting failed: " + ledgerResult.message());
                depositRepository.save(deposit);
                auditLogService.logAction(deposit.getId(), command.customerId(),
                        "LEDGER_POSTING_FAILED", DepositStatus.VALIDATED.name(), DepositStatus.FAILED.name(),
                        ledgerResult.message());
                return DepositResult.fromDomain(deposit, ledgerResult.message());
            }
            deposit.markLedgerPosted();

            auditLogService.logAction(deposit.getId(), command.customerId(),
                    "LEDGER_POSTED", DepositStatus.VALIDATED.name(), DepositStatus.LEDGER_POSTED.name(),
                    "Transaction: " + ledgerResult.transactionId());

            // 4. Mark completed
            deposit.markCompleted();
            deposit = depositRepository.save(deposit);

            auditLogService.logAction(deposit.getId(), command.customerId(),
                    "DEPOSIT_COMPLETED", DepositStatus.LEDGER_POSTED.name(), DepositStatus.COMPLETED.name(),
                    "Deposit completed successfully");

            log.info("Deposit {} completed successfully", deposit.getId());
            return DepositResult.fromDomain(deposit, "Deposit completed successfully");

        } catch (CustomerNotFoundException e) {
            log.error("Customer not found: {}", command.customerId(), e);
            deposit.markFailed(e.getMessage());
            depositRepository.save(deposit);
            auditLogService.logAction(deposit.getId(), command.customerId(),
                    "CUSTOMER_NOT_FOUND", DepositStatus.PENDING.name(), DepositStatus.FAILED.name(),
                    e.getMessage());
            return DepositResult.fromDomain(deposit, e.getMessage());

        } catch (LedgerPostingException e) {
            log.error("Ledger posting failed for deposit: {}", deposit.getId(), e);
            deposit.markFailed(e.getMessage());
            depositRepository.save(deposit);
            auditLogService.logAction(deposit.getId(), command.customerId(),
                    "LEDGER_EXCEPTION", deposit.getStatus().name(), DepositStatus.FAILED.name(),
                    e.getMessage());
            return DepositResult.fromDomain(deposit, e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during deposit creation: {}", deposit.getId(), e);
            deposit.markFailed("Internal error: " + e.getMessage());
            depositRepository.save(deposit);
            auditLogService.logAction(deposit.getId(), command.customerId(),
                    "INTERNAL_ERROR", deposit.getStatus().name(), DepositStatus.FAILED.name(),
                    e.getMessage());
            return DepositResult.fromDomain(deposit, "Internal error occurred");
        }
    }
}
