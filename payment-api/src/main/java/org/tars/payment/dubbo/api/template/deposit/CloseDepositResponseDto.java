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
public class CloseDepositResponseDto implements Serializable {
    private String depositId;
    private String status;
    private BigDecimal finalBalance;
    private BigDecimal accruedInterest;
    private String message;
    private String closedAt;
}
