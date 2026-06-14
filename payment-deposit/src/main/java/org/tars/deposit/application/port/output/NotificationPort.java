package org.tars.deposit.application.port.output;

import java.util.Map;

/**
 * Port for notification service integration.
 */
public interface NotificationPort {

    void send(String customerId, String channel, String templateCode, Map<String, String> parameters);
}
