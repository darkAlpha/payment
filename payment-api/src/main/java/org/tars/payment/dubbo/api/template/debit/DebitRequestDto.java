package org.tars.payment.dubbo.api.template.debit;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DebitRequestDto {
    private String id;
    private String name;
    private BigDecimal amount;
    private BigDecimal rate;
}
