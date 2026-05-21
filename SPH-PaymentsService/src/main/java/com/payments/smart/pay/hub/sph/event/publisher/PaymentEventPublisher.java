package com.payments.smart.pay.hub.sph.event.publisher;

import com.payments.smart.pay.hub.sph.event.PaymentTransactionEvent;

public interface PaymentEventPublisher {
	void publishTransaction(PaymentTransactionEvent event);
}