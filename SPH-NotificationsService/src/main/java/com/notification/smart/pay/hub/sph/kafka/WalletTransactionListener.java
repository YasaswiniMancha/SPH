package com.notification.smart.pay.hub.sph.kafka;

import com.notification.smart.pay.hub.sph.dto.request.NotificationDTO;
import com.notification.smart.pay.hub.sph.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WalletTransactionListener {

    private final NotificationService notificationService;

    public WalletTransactionListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "wallet.transactions", groupId = "notification-service-group")
    public void onMessage(String payload) {
        log.info("Received wallet transaction event: {}", payload);
        try {
            // Simple parsing; in prod use a proper DTO and ObjectMapper
            String subject = "Wallet Transaction";
            String message = "Transaction event: " + payload;

            NotificationDTO dto = new NotificationDTO();
            dto.setRecipientId("system");
            dto.setRecipientEmail("ops@example.com");
            dto.setNotificationType("TRANSACTION_EVENT");
            dto.setSubject(subject);
            dto.setMessage(message);
            dto.setHtmlContent(null);
            dto.setMaxRetries(3);

            notificationService.sendNotification(dto);
        } catch (Exception ex) {
            log.error("Failed to process wallet transaction event", ex);
        }
    }
}

