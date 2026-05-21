package com.notification.smart.pay.hub.sph.service;

import com.common.smart.pay.hub.sph.exception.BusinessException;
import com.common.smart.pay.hub.sph.exception.ResourceNotFoundException;
import com.notification.smart.pay.hub.sph.dto.request.NotificationDTO;
import com.notification.smart.pay.hub.sph.dto.response.NotificationResponseDTO;
import com.notification.smart.pay.hub.sph.entity.NotificationEntity;
import com.notification.smart.pay.hub.sph.repository.NotificationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationMetricsService metricsService;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository,
                               EmailService emailService,
                               KafkaTemplate<String, String> kafkaTemplate,
                               NotificationMetricsService metricsService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.kafkaTemplate = kafkaTemplate;
        this.metricsService = metricsService;
    }

    @Timed(value = "notification.send", description = "Send notification time")
    @CircuitBreaker(name = "notificationServiceCB", fallbackMethod = "sendNotificationFallback")
    public NotificationResponseDTO sendNotification(NotificationDTO request) {
        log.info("Sending notification to: {}", request.getRecipientEmail());

        NotificationEntity notification = NotificationEntity.builder()
                .recipientId(request.getRecipientId())
                .recipientEmail(request.getRecipientEmail())
                .notificationType(request.getNotificationType())
                .subject(request.getSubject())
                .message(request.getMessage())
                .htmlContent(request.getHtmlContent())
                .maxRetries(request.getMaxRetries())
                .status(NotificationEntity.NotificationStatus.PENDING)
                .isActive(true)
                .build();

        NotificationEntity saved = notificationRepository.save(notification);

        // Attempt to send immediately
        boolean sent = sendEmailNotification(saved);
        if (sent) {
            saved.setStatus(NotificationEntity.NotificationStatus.SENT);
            saved.setSentAt(LocalDateTime.now());
            notificationRepository.save(saved);
            metricsService.recordNotificationSent();
        }

        publishEvent("NOTIFICATION_SENT", saved);
        return toResponseDTO(saved);
    }

    @Cacheable(value = "notifications", key = "#id")
    @Timed(value = "notification.get", description = "Get notification time")
    public NotificationResponseDTO getNotificationById(String id) {
        log.info("Fetching notification: {}", id);
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        return toResponseDTO(notification);
    }

    @Timed(value = "notification.list", description = "List notifications time")
    public Page<NotificationResponseDTO> getAllNotifications(Pageable pageable) {
        log.info("Fetching all active notifications");
        return notificationRepository.findAllActiveNotifications(pageable)
                .map(this::toResponseDTO);
    }

    @Timed(value = "notification.list.recipient", description = "List recipient notifications time")
    public Page<NotificationResponseDTO> getNotificationsByRecipient(String recipientId, Pageable pageable) {
        log.info("Fetching notifications for recipient: {}", recipientId);
        return notificationRepository.findByRecipientId(recipientId, pageable)
                .map(this::toResponseDTO);
    }

    @Timed(value = "notification.list.status", description = "List notifications by status time")
    public List<NotificationResponseDTO> getNotificationsByStatus(String status) {
        log.info("Fetching notifications with status: {}", status);
        NotificationEntity.NotificationStatus notifStatus = NotificationEntity.NotificationStatus.valueOf(status);
        return notificationRepository.findByStatus(notifStatus)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @CacheEvict(value = "notifications", allEntries = true)
    @Timed(value = "notification.retry", description = "Retry failed notification time")
    public NotificationResponseDTO retryNotification(String id) {
        log.info("Retrying notification: {}", id);
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            throw new BusinessException("Max retry attempts exceeded");
        }

        notification.setRetryCount(notification.getRetryCount() + 1);
        boolean sent = sendEmailNotification(notification);

        if (sent) {
            notification.setStatus(NotificationEntity.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            metricsService.recordNotificationSent();
        } else {
            notification.setStatus(NotificationEntity.NotificationStatus.FAILED);
        }

        NotificationEntity updated = notificationRepository.save(notification);
        publishEvent("NOTIFICATION_RETRY", updated);
        return toResponseDTO(updated);
    }

    @CacheEvict(value = "notifications", allEntries = true)
    public void deleteNotification(String id) {
        log.info("Deleting notification: {}", id);
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setIsActive(false);
        notificationRepository.save(notification);
        publishEvent("NOTIFICATION_DELETED", notification);
    }

    private boolean sendEmailNotification(NotificationEntity notification) {
        try {
            boolean emailSent;
            if (notification.getHtmlContent() != null && !notification.getHtmlContent().isEmpty()) {
                emailSent = emailService.sendHtmlEmail(notification.getRecipientEmail(),
                        notification.getSubject(),
                        notification.getHtmlContent());
            } else {
                emailSent = emailService.sendSimpleEmail(notification.getRecipientEmail(),
                        notification.getSubject(),
                        notification.getMessage());
            }
            return emailSent;
        } catch (Exception e) {
            log.error("Failed to send email notification", e);
            notification.setFailureReason(e.getMessage());
            return false;
        }
    }

    public NotificationResponseDTO sendNotificationFallback(NotificationDTO request, Exception ex) {
        log.error("Notification send fallback triggered: {}", ex.getMessage());
        throw new BusinessException("Notification service temporarily unavailable");
    }

    private void publishEvent(String eventType, NotificationEntity notification) {
        try {
            String message = String.format(
                    "notificationId=%s,recipientId=%s,eventType=%s,timestamp=%d",
                    notification.getId(),
                    notification.getRecipientId(),
                    eventType,
                    System.currentTimeMillis()
            );
            kafkaTemplate.send("notification-events", message);
            log.debug("Notification event published: {}", eventType);
        } catch (Exception e) {
            log.warn("Failed to publish notification event", e);
        }
    }

    private NotificationResponseDTO toResponseDTO(NotificationEntity entity) {
        return NotificationResponseDTO.builder()
                .id(entity.getId())
                .recipientId(entity.getRecipientId())
                .recipientEmail(entity.getRecipientEmail())
                .notificationType(entity.getNotificationType())
                .subject(entity.getSubject())
                .status(entity.getStatus().toString())
                .retryCount(entity.getRetryCount())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .failureReason(entity.getFailureReason())
                .build();
    }
}