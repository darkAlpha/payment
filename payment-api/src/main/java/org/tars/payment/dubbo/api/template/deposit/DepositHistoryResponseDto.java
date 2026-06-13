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
public class DepositHistoryResponseDto implements Serializable {

    private String depositId;
    private String customerId;
    private String accountId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private String failureReason;
    private String createdAt;
    private String updatedAt;
}

