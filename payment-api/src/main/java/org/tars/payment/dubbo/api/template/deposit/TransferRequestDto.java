package org.tars.payment.dubbo.api.template.deposit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDto implements Serializable {
    private String customerId;
    private String sourceId;
    private String targetId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String idempotencyKey;
}
