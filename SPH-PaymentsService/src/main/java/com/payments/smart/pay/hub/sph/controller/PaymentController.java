package com.payments.smart.pay.hub.sph.controller;

import java.util.UUID;

import com.payments.smart.pay.hub.sph.dto.request.CreatePaymentRequest;
import com.payments.smart.pay.hub.sph.dto.response.PaymentResponse;
import com.payments.smart.pay.hub.sph.dto.response.PaymentTransactionResponse;
import com.payments.smart.pay.hub.sph.dto.request.ProcessPaymentRequest;
import com.payments.smart.pay.hub.sph.dto.request.RefundPaymentRequest;
import com.payments.smart.pay.hub.sph.service.PaymentService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

	private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping
	public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
	}

	@GetMapping("/{paymentId}")
	public PaymentResponse getPayment(@PathVariable UUID paymentId) {
		return paymentService.getPayment(paymentId);
	}

	@PostMapping("/{paymentId}/process")
	public PaymentTransactionResponse processPayment(
		@PathVariable UUID paymentId,
		@Valid @RequestBody ProcessPaymentRequest request,
		@RequestHeader(name = IDEMPOTENCY_HEADER, required = false) String idempotencyKey
	) {
		return paymentService.processPayment(request, idempotencyKey);
	}

	@PostMapping("/{paymentId}/refund")
	public PaymentTransactionResponse refundPayment(
		@PathVariable UUID paymentId,
		@Valid @RequestBody RefundPaymentRequest request,
		@RequestHeader(name = IDEMPOTENCY_HEADER, required = false) String idempotencyKey
	) {
		return paymentService.refundPayment(request, idempotencyKey);
	}
}