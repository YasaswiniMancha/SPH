package com.notification.smart.pay.hub.sph.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {

    private String id;
    private String recipientId;
    private String recipientEmail;
    private String notificationType;
    private String subject;
    private String status;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String failureReason;
}