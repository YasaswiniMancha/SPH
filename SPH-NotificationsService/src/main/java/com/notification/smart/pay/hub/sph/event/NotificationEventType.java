package com.notification.smart.pay.hub.sph.event;

import java.time.LocalDateTime;

public sealed abstract class NotificationEventType {

    public abstract String getEventType();
    public abstract LocalDateTime getTimestamp();
    public abstract String getNotificationId();

    public static final class NotificationSent extends NotificationEventType {
        private final String notificationId;
        private final LocalDateTime timestamp;
        private final String recipientEmail;

        public NotificationSent(String notificationId, String recipientEmail) {
            this.notificationId = notificationId;
            this.timestamp = LocalDateTime.now();
            this.recipientEmail = recipientEmail;
        }

        @Override
        public String getEventType() { return "NOTIFICATION_SENT"; }
        @Override
        public LocalDateTime getTimestamp() { return timestamp; }
        @Override
        public String getNotificationId() { return notificationId; }
        public String getRecipientEmail() { return recipientEmail; }
    }

    public static final class NotificationFailed extends NotificationEventType {
        private final String notificationId;
        private final LocalDateTime timestamp;
        private final String failureReason;

        public NotificationFailed(String notificationId, String failureReason) {
            this.notificationId = notificationId;
            this.timestamp = LocalDateTime.now();
            this.failureReason = failureReason;
        }

        @Override
        public String getEventType() { return "NOTIFICATION_FAILED"; }
        @Override
        public LocalDateTime getTimestamp() { return timestamp; }
        @Override
        public String getNotificationId() { return notificationId; }
        public String getFailureReason() { return failureReason; }
    }

    public static final class NotificationRetry extends NotificationEventType {
        private final String notificationId;
        private final LocalDateTime timestamp;
        private final int retryCount;

        public NotificationRetry(String notificationId, int retryCount) {
            this.notificationId = notificationId;
            this.timestamp = LocalDateTime.now();
            this.retryCount = retryCount;
        }

        @Override
        public String getEventType() { return "NOTIFICATION_RETRY"; }
        @Override
        public LocalDateTime getTimestamp() { return timestamp; }
        @Override
        public String getNotificationId() { return notificationId; }
        public int getRetryCount() { return retryCount; }
    }

    public static final class NotificationDelivered extends NotificationEventType {
        private final String notificationId;
        private final LocalDateTime timestamp;
        private final long deliveryTimeMs;

        public NotificationDelivered(String notificationId, long deliveryTimeMs) {
            this.notificationId = notificationId;
            this.timestamp = LocalDateTime.now();
            this.deliveryTimeMs = deliveryTimeMs;
        }

        @Override
        public String getEventType() { return "NOTIFICATION_DELIVERED"; }
        @Override
        public LocalDateTime getTimestamp() { return timestamp; }
        @Override
        public String getNotificationId() { return notificationId; }
        public long getDeliveryTimeMs() { return deliveryTimeMs; }
    }
}