package com.notification.smart.pay.hub.sph.controller;

import com.notification.smart.pay.hub.sph.controller.NotificationController;
import com.notification.smart.pay.hub.sph.dto.request.NotificationDTO;
import com.notification.smart.pay.hub.sph.dto.response.NotificationResponseDTO;
import com.notification.smart.pay.hub.sph.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void sendNotification() {
        NotificationDTO req = new NotificationDTO();
        NotificationResponseDTO resp = NotificationResponseDTO.builder().id("1").recipientId("r1").subject("s").build();
        when(notificationService.sendNotification(req)).thenReturn(resp);

        var r = notificationController.sendNotification(req);
        assertEquals(201, r.getStatusCodeValue());
        assertEquals(resp, r.getBody());
    }

    @Test
    void getNotification() {
        NotificationResponseDTO resp = NotificationResponseDTO.builder().id("1").recipientId("r1").subject("s").build();
        when(notificationService.getNotificationById("1")).thenReturn(resp);

        var r = notificationController.getNotification("1");
        assertEquals(200, r.getStatusCodeValue());
    }

    @Test
    void getAllNotifications() {
        NotificationResponseDTO dto = NotificationResponseDTO.builder().id("1").recipientId("r1").subject("s").build();
        Page<NotificationResponseDTO> page = new PageImpl<>(List.of(dto));
        when(notificationService.getAllNotifications(any())).thenReturn(page);

        var r = notificationController.getAllNotifications(null);
        assertEquals(200, r.getStatusCodeValue());
    }

    @Test
    void getRecipientNotifications() {
        NotificationResponseDTO dto = NotificationResponseDTO.builder().id("1").recipientId("r1").subject("s").build();
        Page<NotificationResponseDTO> page = new PageImpl<>(List.of(dto));
        when(notificationService.getNotificationsByRecipient(eq("r1"), any())).thenReturn(page);

        var r = notificationController.getRecipientNotifications("r1", null);
        assertEquals(200, r.getStatusCodeValue());
    }

    @Test
    void retryAndDeleteNotification() {
        NotificationResponseDTO dto = NotificationResponseDTO.builder().id("1").recipientId("r1").subject("s").build();
        when(notificationService.retryNotification("1")).thenReturn(dto);
        doNothing().when(notificationService).deleteNotification("1");

        var r1 = notificationController.retryNotification("1");
        assertEquals(200, r1.getStatusCodeValue());

        var r2 = notificationController.deleteNotification("1");
        assertEquals(204, r2.getStatusCodeValue());
    }
}

