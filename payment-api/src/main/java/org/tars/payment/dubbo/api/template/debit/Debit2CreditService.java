package org.tars.payment.dubbo.api.template.debit;

/**
 * Debit to credit service
 */
public interface Debit2CreditService {

    DebitResponseDto sendMoneyDebit2Credit(DebitRequestDto debitRequestDto);
}
