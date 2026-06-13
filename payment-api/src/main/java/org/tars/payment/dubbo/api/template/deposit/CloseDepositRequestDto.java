package org.tars.payment.dubbo.api.template.deposit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseDepositRequestDto implements Serializable {
    private String depositId;
    private String customerId;
    private String reason;
    private String idempotencyKey;
}
