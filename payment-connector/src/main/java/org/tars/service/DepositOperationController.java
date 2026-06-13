package org.tars.service;

import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.tars.payment.dubbo.api.template.deposit.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/deposit")
public class DepositOperationController {

    private static final Logger log = LoggerFactory.getLogger(DepositOperationController.class);

    @DubboReference
    private DepositService depositService;

    @PostMapping
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'SYSTEM')")
    public CreateDepositResponseDto createDeposit(@RequestBody @Validated CreateDepositRequestDto request) {
        log.info("POST /api/v1/deposit - customer={}, amount={} {}",
                request.getCustomerId(), request.getAmount(), request.getCurrency());
        CreateDepositResponseDto response = depositService.createDeposit(request);
        log.info("POST /api/v1/deposit - depositId={}, status={}", response.getDepositId(), response.getStatus());
        return response;
    }

    @PostMapping("/close")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN')")
    public CloseDepositResponseDto closeDeposit(@RequestBody @Validated CloseDepositRequestDto request) {
        log.info("POST /api/v1/deposit/close - depositId={}", request.getDepositId());
        CloseDepositResponseDto response = depositService.closeDeposit(request);
        log.info("POST /api/v1/deposit/close - status={}", response.getStatus());
        return response;
    }

    @PostMapping("/transfer/deposit-to-card")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'SYSTEM')")
    public TransferResponseDto depositToCard(@RequestBody @Validated TransferRequestDto request) {
        log.info("POST /api/v1/deposit/transfer/deposit-to-card - source={}, target={}, amount={}",
                request.getSourceId(), request.getTargetId(), request.getAmount());
        return depositService.depositToCard(request);
    }

    @PostMapping("/transfer/card-to-deposit")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'SYSTEM')")
    public TransferResponseDto cardToDeposit(@RequestBody @Validated TransferRequestDto request) {
        log.info("POST /api/v1/deposit/transfer/card-to-deposit - source={}, target={}, amount={}",
                request.getSourceId(), request.getTargetId(), request.getAmount());
        return depositService.cardToDeposit(request);
    }

    @PostMapping("/transfer/deposit-to-deposit")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'SYSTEM')")
    public TransferResponseDto depositToDeposit(@RequestBody @Validated TransferRequestDto request) {
        log.info("POST /api/v1/deposit/transfer/deposit-to-deposit - source={}, target={}, amount={}",
                request.getSourceId(), request.getTargetId(), request.getAmount());
        return depositService.depositToDeposit(request);
    }

    @GetMapping("/history/{customerId}")
    @PreAuthorize("hasAnyRole('TELLER', 'ADMIN', 'AUDITOR')")
    public List<DepositHistoryResponseDto> getHistory(@PathVariable String customerId) {
        log.info("GET /api/v1/deposit/history/{}", customerId);
        List<DepositHistoryResponseDto> history = depositService.getDepositHistory(customerId);
        log.info("GET /api/v1/deposit/history/{} - returned {} records", customerId, history.size());
        return history;
    }
}
