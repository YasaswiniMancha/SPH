package com.payments.smart.pay.hub.sph.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
	UUID paymentId,
	String merchantId,
	String customerId,
	BigDecimal amount,
	String currency,
	String status,
	String referenceId,
	String description
) {
}