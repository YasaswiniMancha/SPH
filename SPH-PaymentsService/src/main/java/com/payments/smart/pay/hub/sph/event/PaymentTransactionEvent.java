package com.payments.smart.pay.hub.sph.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Domain event for payment transactions.
 * Published to Kafka for consumption by other services (notifications, settlement, etc.)
 */
public record PaymentTransactionEvent(
		@JsonProperty("transaction_id")
		UUID transactionId,

		@JsonProperty("payment_id")
		UUID paymentId,

		@JsonProperty("type")
		String type,

		@JsonProperty("amount")
		BigDecimal amount,

		@JsonProperty("currency")
		String currency,

		@JsonProperty("reference_id")
		String referenceId,

		@JsonProperty("occurred_at")
		Instant occurredAt
) implements Serializable {

	private static final long serialVersionUID = 1L;
}