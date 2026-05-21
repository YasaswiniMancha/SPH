package com.notification.smart.pay.hub.sph.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_recipient_id", columnList = "recipient_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_is_active", columnList = "is_active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String recipientId;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String notificationType; // EMAIL, SMS, PUSH, IN_APP

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String htmlContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING; // PENDING, SENT, FAILED, DELIVERED

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private Integer retryCount = 0;

    private Integer maxRetries = 3;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime sentAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Version
    private Long version;

    public enum NotificationStatus {
        PENDING, SENT, FAILED, DELIVERED, BOUNCED
    }

    public enum NotificationType {
        EMAIL, SMS, PUSH, IN_APP
    }
}