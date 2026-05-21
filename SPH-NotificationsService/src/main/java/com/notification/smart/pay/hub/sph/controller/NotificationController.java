package com.notification.smart.pay.hub.sph.controller;

import com.notification.smart.pay.hub.sph.dto.request.NotificationDTO;
import com.notification.smart.pay.hub.sph.dto.response.NotificationResponseDTO;
import com.notification.smart.pay.hub.sph.service.NotificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponseDTO> sendNotification(@Valid @RequestBody NotificationDTO request) {
        log.info("Send notification endpoint called");
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendNotification(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponseDTO> getNotification(@PathVariable String id) {
        log.info("Get notification endpoint called: {}", id);
        return ResponseEntity.ok(notificationService.getNotificationById(id));
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> getAllNotifications(Pageable pageable) {
        log.info("List notifications endpoint called");
        return ResponseEntity.ok(notificationService.getAllNotifications(pageable));
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<Page<NotificationResponseDTO>> getRecipientNotifications(
            @PathVariable String recipientId, Pageable pageable) {
        log.info("Get recipient notifications: {}", recipientId);
        return ResponseEntity.ok(notificationService.getNotificationsByRecipient(recipientId, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getNotificationsByStatus(@PathVariable String status) {
        log.info("Get notifications by status: {}", status);
        return ResponseEntity.ok(notificationService.getNotificationsByStatus(status));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<NotificationResponseDTO> retryNotification(@PathVariable String id) {
        log.info("Retry notification: {}", id);
        return ResponseEntity.ok(notificationService.retryNotification(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable String id) {
        log.info("Delete notification: {}", id);
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is healthy");
    }
}