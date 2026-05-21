package com.notification.smart.pay.hub.sph.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    @NotBlank(message = "Recipient ID is required")
    private String recipientId;

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;

    @NotBlank(message = "Notification type is required")
    @Pattern(regexp = "EMAIL|SMS|PUSH|IN_APP", message = "Invalid notification type")
    private String notificationType;

    @NotBlank(message = "Subject is required")
    @Size(min = 3, max = 255, message = "Subject must be between 3-255 characters")
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(min = 5, max = 5000, message = "Message must be between 5-5000 characters")
    private String message;

    private String htmlContent;

    @Min(value = 1, message = "Max retries must be at least 1")
    @Max(value = 10, message = "Max retries cannot exceed 10")
    private Integer maxRetries = 3;
}