package org.tars.deposit.interfaces.dubbo;

import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tars.deposit.application.dto.*;
import org.tars.deposit.application.port.input.*;
import org.tars.deposit.domain.model.TransactionType;
import org.tars.payment.dubbo.api.template.deposit.*;

import java.util.List;

/**
 * Dubbo service implementation for all deposit operations.
 */
@DubboService
public class DepositServiceImpl implements DepositService {

    private static final Logger log = LoggerFactory.getLogger(DepositServiceImpl.class);

    private final CreateDepositUseCase createDepositUseCase;
    private final GetDepositHistoryUseCase getDepositHistoryUseCase;
    private final CloseDepositUseCase closeDepositUseCase;
    private final TransferUseCase transferUseCase;

    public DepositServiceImpl(CreateDepositUseCase createDepositUseCase,
                              GetDepositHistoryUseCase getDepositHistoryUseCase,
                              CloseDepositUseCase closeDepositUseCase,
                              TransferUseCase transferUseCase) {
        this.createDepositUseCase = createDepositUseCase;
        this.getDepositHistoryUseCase = getDepositHistoryUseCase;
        this.closeDepositUseCase = closeDepositUseCase;
        this.transferUseCase = transferUseCase;
    }

    @Override
    public CreateDepositResponseDto createDeposit(CreateDepositRequestDto request) {
        log.info("Dubbo createDeposit: customerId={}, amount={} {}",
                request.getCustomerId(), request.getAmount(), request.getCurrency());

        CreateDepositCommand command = new CreateDepositCommand(
                request.getCustomerId(), request.getAccountId(),
                request.getAmount(), request.getCurrency(), request.getDescription());

        DepositResult result = createDepositUseCase.execute(command);

        log.info("Dubbo createDeposit result: depositId={}, status={}", result.depositId(), result.status());

        return CreateDepositResponseDto.builder()
                .depositId(result.depositId()).customerId(result.customerId())
                .accountId(result.accountId()).amount(result.amount()).currency(result.currency())
                .status(result.status().name()).message(result.message())
                .createdAt(result.createdAt() != null ? result.createdAt().toString() : null)
                .build();
    }

    @Override
    public CloseDepositResponseDto closeDeposit(CloseDepositRequestDto request) {
        log.info("Dubbo closeDeposit: depositId={}", request.getDepositId());

        CloseDepositCommand command = new CloseDepositCommand(
                request.getDepositId(), request.getCustomerId(),
                request.getReason(), request.getIdempotencyKey());

        CloseDepositResult result = closeDepositUseCase.execute(command);

        log.info("Dubbo closeDeposit result: status={}", result.status());

        return CloseDepositResponseDto.builder()
                .depositId(result.depositId()).status(result.status())
                .finalBalance(result.finalBalance()).accruedInterest(result.accruedInterest())
                .message(result.message())
                .closedAt(result.closedAt() != null ? result.closedAt().toString() : null)
                .build();
    }

    @Override
    public TransferResponseDto depositToCard(TransferRequestDto request) {
        log.info("Dubbo depositToCard: source={}, target={}, amount={}", request.getSourceId(), request.getTargetId(), request.getAmount());
        return executeTransfer(request, TransactionType.DEPOSIT_TO_CARD);
    }

    @Override
    public TransferResponseDto cardToDeposit(TransferRequestDto request) {
        log.info("Dubbo cardToDeposit: source={}, target={}, amount={}", request.getSourceId(), request.getTargetId(), request.getAmount());
        return executeTransfer(request, TransactionType.CARD_TO_DEPOSIT);
    }

    @Override
    public TransferResponseDto depositToDeposit(TransferRequestDto request) {
        log.info("Dubbo depositToDeposit: source={}, target={}, amount={}", request.getSourceId(), request.getTargetId(), request.getAmount());
        return executeTransfer(request, TransactionType.DEPOSIT_TO_DEPOSIT);
    }

    @Override
    public List<DepositHistoryResponseDto> getDepositHistory(String customerId) {
        log.info("Dubbo getDepositHistory: customerId={}", customerId);
        List<DepositResult> results = getDepositHistoryUseCase.execute(customerId);
        log.info("Dubbo getDepositHistory: {} records", results.size());

        return results.stream()
                .map(r -> DepositHistoryResponseDto.builder()
                        .depositId(r.depositId()).customerId(r.customerId())
                        .accountId(r.accountId()).amount(r.amount()).currency(r.currency())
                        .status(r.status().name()).description(r.message())
                        .createdAt(r.createdAt() != null ? r.createdAt().toString() : null)
                        .build())
                .toList();
    }

    private TransferResponseDto executeTransfer(TransferRequestDto request, TransactionType type) {
        TransferCommand command = new TransferCommand(
                request.getCustomerId(), request.getSourceId(), request.getTargetId(),
                request.getAmount(), request.getCurrency(), request.getDescription(),
                type, request.getIdempotencyKey());

        TransferResult result = transferUseCase.execute(command);

        return TransferResponseDto.builder()
                .transactionId(result.transactionId())
                .sourceId(result.sourceId()).targetId(result.targetId())
                .amount(result.amount()).currency(result.currency())
                .status(result.status()).message(result.message())
                .createdAt(result.createdAt() != null ? result.createdAt().toString() : null)
                .build();
    }
}
