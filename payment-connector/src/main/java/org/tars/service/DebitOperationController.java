package org.tars.service;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tars.payment.dubbo.api.template.debit.Debit2CreditService;
import org.tars.payment.dubbo.api.template.debit.DebitRequestDto;
import org.tars.payment.dubbo.api.template.debit.DebitResponseDto;

@RestController
@RequestMapping("/api/v1/debit")
public class DebitOperationController {
    @DubboReference
    private Debit2CreditService debit2CreditService;

    @PostMapping(value = "/transfer")
    public DebitResponseDto transferMoneyToCredit(@RequestBody
                                                                  @Validated
                                                                  DebitRequestDto debitRequestDto) {

        return debit2CreditService.sendMoneyDebit2Credit(debitRequestDto);
    }
}
