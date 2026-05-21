package com.notification.smart.pay.hub.sph.dto;

import java.time.LocalDateTime;

public class NotificationRecords {

    public record NotificationRequest(
        String recipientId,
        String recipientEmail,
        String notificationType,
        String subject,
        String message,
        String htmlContent
    ) {}

    public record NotificationResponse(
        String id,
        String recipientId,
        String status,
        LocalDateTime sentAt
    ) {}

    public record NotificationEvent(
        String eventId,
        String notificationId,
        String eventType,
        LocalDateTime timestamp,
        long durationMs
    ) {}

    public record NotificationMetrics(
        long totalSent,
        long totalFailed,
        long totalPending,
        double successRate,
        double averageDeliveryTime
    ) {}
}