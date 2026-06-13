package org.tars.payment.dubbo.api.template.deposit;

import java.util.List;

/**
 * Dubbo service API for all deposit operations.
 */
public interface DepositService {

    CreateDepositResponseDto createDeposit(CreateDepositRequestDto request);

    CloseDepositResponseDto closeDeposit(CloseDepositRequestDto request);

    TransferResponseDto depositToCard(TransferRequestDto request);

    TransferResponseDto cardToDeposit(TransferRequestDto request);

    TransferResponseDto depositToDeposit(TransferRequestDto request);

    List<DepositHistoryResponseDto> getDepositHistory(String customerId);
}
