package com.payments.smart.pay.hub.sph.controller;

import com.payments.smart.pay.hub.sph.dto.request.CreatePaymentRequest;
import com.payments.smart.pay.hub.sph.dto.request.ProcessPaymentRequest;
import com.payments.smart.pay.hub.sph.dto.request.RefundPaymentRequest;
import com.payments.smart.pay.hub.sph.dto.response.PaymentResponse;
import com.payments.smart.pay.hub.sph.dto.response.PaymentTransactionResponse;
import com.payments.smart.pay.hub.sph.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerUnitTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private com.payments.smart.pay.hub.sph.controller.PaymentController paymentController;

    @Test
    void createPayment() {
        CreatePaymentRequest req = new CreatePaymentRequest("m1","c1", new BigDecimal("10.00"), "USD", "pm1", "desc");
        PaymentResponse resp = new PaymentResponse(UUID.randomUUID(), "m1", "c1", new BigDecimal("10.00"), "USD", "CREATED", "r1", "desc");
        when(paymentService.createPayment(req)).thenReturn(resp);

        var response = paymentController.createPayment(req);
        assertEquals(201, response.getStatusCodeValue());
        assertEquals(resp, response.getBody());
    }

    @Test
    void processPayment() {
        ProcessPaymentRequest req = new ProcessPaymentRequest(UUID.randomUUID(), new BigDecimal("10.00"), "desc");
        PaymentTransactionResponse txn = new PaymentTransactionResponse(UUID.randomUUID(), UUID.randomUUID(), "PROCESS", new BigDecimal("10.00"), "USD", "ref", "desc");
        when(paymentService.processPayment(eq(req), eq("key1"))).thenReturn(txn);

        var res = paymentController.processPayment(UUID.randomUUID(), req, "key1");
        assertEquals(txn, res);
    }

    @Test
    void refundPayment() {
        RefundPaymentRequest req = new RefundPaymentRequest(UUID.randomUUID(), new BigDecimal("5.00"), "reason");
        PaymentTransactionResponse txn = new PaymentTransactionResponse(UUID.randomUUID(), UUID.randomUUID(), "REFUND", new BigDecimal("5.00"), "USD", "ref2", "reason");
        when(paymentService.refundPayment(eq(req), any())).thenReturn(txn);

        var res = paymentController.refundPayment(UUID.randomUUID(), req, null);
        assertEquals(txn, res);
    }

    @Test
    void processPaymentIdempotencyHeader() {
        ProcessPaymentRequest req = new ProcessPaymentRequest(UUID.randomUUID(), new BigDecimal("10.00"), "desc");
        PaymentTransactionResponse txn = new PaymentTransactionResponse(UUID.randomUUID(), req.paymentId(), "PROCESS", req.amount(), "USD", "r3", "desc");
        when(paymentService.processPayment(eq(req), eq("idem"))).thenReturn(txn);

        var res = paymentController.processPayment(UUID.randomUUID(), req, "idem");
        assertEquals(txn, res);
    }

    @Test
    void getPayment() {
        UUID id = UUID.randomUUID();
        PaymentResponse resp = new PaymentResponse(id, "m1", "c1", new BigDecimal("5.00"), "USD", "CREATED", "r2", "desc");
        when(paymentService.getPayment(id)).thenReturn(resp);

        var result = paymentController.getPayment(id);
        assertEquals(resp, result);
    }

    @Test
    void createPaymentValidation() {
        CreatePaymentRequest req = new CreatePaymentRequest("m2","c2", new BigDecimal("1.00"), "USD", "pm2", "desc");
        when(paymentService.createPayment(req)).thenReturn(new PaymentResponse(UUID.randomUUID(), "m2","c2", new BigDecimal("1.00"), "USD","CREATED","r4","desc"));

        var resp = paymentController.createPayment(req);
        assertEquals(201, resp.getStatusCodeValue());
    }
}

