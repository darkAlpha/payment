package org.tars.payment.dubbo.api.template.debit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DebitResponseDto {
    private String status;
    private String message;
}
