package com.notification.smart.pay.hub.sph.service;

import com.notification.smart.pay.hub.sph.dto.NotificationRecords;
import com.notification.smart.pay.hub.sph.repository.NotificationRepository;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationAsyncService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final Executor virtualThreadExecutor;

    @Autowired
    public NotificationAsyncService(NotificationRepository notificationRepository,
                                    EmailService emailService,
                                    @Qualifier("virtualThreadExecutor") Executor virtualThreadExecutor) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Timed(value = "notification.bulk.send", description = "Bulk send notifications time")
    public CompletableFuture<Integer> sendBulkNotificationsAsync(java.util.List<String> recipientEmails, String subject, String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int successCount = (int) recipientEmails.stream()
                        .parallel()
                        .filter(email -> emailService.sendSimpleEmail(email, subject, message))
                        .count();
                log.info("Bulk notifications sent: {} of {}", successCount, recipientEmails.size());
                return successCount;
            } catch (Exception e) {
                log.error("Bulk notification send failed", e);
                return 0;
            }
        }, virtualThreadExecutor);
    }

    @Timed(value = "notification.process.pending", description = "Process pending notifications time")
    public CompletableFuture<Void> processPendingNotificationsAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                var pendingNotifications = notificationRepository.findByStatus(
                        com.notification.smart.pay.hub.sph.entity.NotificationEntity.NotificationStatus.PENDING);

                pendingNotifications.parallelStream()
                        .forEach(notification -> {
                            try {
                                boolean sent = emailService.sendSimpleEmail(
                                        notification.getRecipientEmail(),
                                        notification.getSubject(),
                                        notification.getMessage()
                                );
                                if (sent) {
                                    notification.setStatus(
                                            com.notification.smart.pay.hub.sph.entity.NotificationEntity.NotificationStatus.SENT);
                                }
                                notificationRepository.save(notification);
                            } catch (Exception e) {
                                log.error("Failed to process pending notification", e);
                            }
                        });
                log.info("Processed {} pending notifications", pendingNotifications.size());
            } catch (Exception e) {
                log.error("Pending notification processing failed", e);
            }
        }, virtualThreadExecutor);
    }

    @Timed(value = "notification.export", description = "Export notifications time")
    public CompletableFuture<String> exportNotificationsAsync(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var notifications = notificationRepository.findByDateRange(startDate, endDate);
                String csv = "ID,RECIPIENT,TYPE,SUBJECT,STATUS,CREATED_AT\n" +
                        notifications.stream()
                                .map(n -> String.format("%s,%s,%s,%s,%s,%s",
                                        n.getId(),
                                        n.getRecipientEmail(),
                                        n.getNotificationType(),
                                        n.getSubject(),
                                        n.getStatus(),
                                        n.getCreatedAt()))
                                .collect(Collectors.joining("\n"));
                log.info("Exported {} notifications", notifications.size());
                return csv;
            } catch (Exception e) {
                log.error("Notification export failed", e);
                return "";
            }
        }, virtualThreadExecutor);
    }
}