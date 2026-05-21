package com.payments.smart.pay.hub.sph.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentTransactionResponse(
	UUID transactionId,
	UUID paymentId,
	String type,
	BigDecimal amount,
	String currency,
	String referenceId,
	String description
) {
}