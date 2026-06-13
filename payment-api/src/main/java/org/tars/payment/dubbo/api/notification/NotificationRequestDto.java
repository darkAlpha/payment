package org.tars.payment.dubbo.api.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDto implements Serializable {
    private String customerId;
    private String channel;    // SMS, EMAIL, PUSH
    private String templateCode;
    private Map<String, String> parameters;
}
