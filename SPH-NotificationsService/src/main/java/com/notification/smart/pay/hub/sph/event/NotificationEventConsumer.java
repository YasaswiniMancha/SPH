package com.notification.smart.pay.hub.sph.event;

import com.notification.smart.pay.hub.sph.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationEventConsumer {

    @Autowired
    private NotificationRepository notificationRepository;

    @KafkaListener(topics = "notification-events", groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory", concurrency = "10")
    public void consumeNotificationEvents(String message, Acknowledgment acknowledgment) {
        try {
            log.info("Received notification event: {}", message);
            String[] parts = message.split(",");
            String eventType = null;

            for (String part : parts) {
                if (part.startsWith("eventType=")) {
                    eventType = part.substring(10);
                    break;
                }
            }

            log.debug("Processing event type: {}", eventType);
            // Process event based on type
            if ("NOTIFICATION_SENT".equals(eventType)) {
                log.info("Event processed: {}", eventType);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error consuming notification event", e);
        }
    }

    @KafkaListener(topics = "merchant-events", groupId = "notification-merchant-group")
    public void consumeMerchantEvents(String message) {
        log.info("Received merchant event for notification: {}", message);
        // Trigger notification on merchant events
    }
}