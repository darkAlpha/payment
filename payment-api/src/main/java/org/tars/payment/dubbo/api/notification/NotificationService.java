package org.tars.payment.dubbo.api.notification;

/**
 * Dubbo service API for sending notifications.
 */
public interface NotificationService {

    void sendNotification(NotificationRequestDto request);
}
