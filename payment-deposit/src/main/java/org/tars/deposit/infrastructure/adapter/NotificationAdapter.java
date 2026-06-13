package org.tars.deposit.infrastructure.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.tars.core.resilience.Resilient;
import org.tars.deposit.application.port.output.NotificationPort;

import java.util.Map;

/**
 * Adapter for notification service. Sends async, non-blocking.
 */
@Component
public class NotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(NotificationAdapter.class);

    @Override
    @Async
    @Resilient(name = "notification-service", maxRetries = 2, retryDelayMs = 1000)
    public void send(String customerId, String channel, String templateCode, Map<String, String> parameters) {
        log.info("NOTIFICATION: customer={}, channel={}, template={}, params={}",
                customerId, channel, templateCode, parameters);

        // TODO: Replace with actual Dubbo call to NotificationService
    }
}
