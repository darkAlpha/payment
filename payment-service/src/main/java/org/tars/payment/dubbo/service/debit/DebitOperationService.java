package org.tars.payment.dubbo.service.debit;

import org.apache.dubbo.config.annotation.DubboService;
import org.tars.payment.dubbo.api.template.debit.Debit2CreditService;
import org.tars.payment.dubbo.api.template.debit.DebitRequestDto;
import org.tars.payment.dubbo.api.template.debit.DebitResponseDto;

@DubboService
public class DebitOperationService implements Debit2CreditService {

    @Override
    public DebitResponseDto sendMoneyDebit2Credit(DebitRequestDto debitRequestDto) {
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return DebitResponseDto.builder()
                .status("OK")
                .message("SUCCESS")
                .build();
    }
}
