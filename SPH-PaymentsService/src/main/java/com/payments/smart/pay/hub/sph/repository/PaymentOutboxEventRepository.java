package com.payments.smart.pay.hub.sph.repository;

import java.util.List;
import java.util.UUID;

import com.payments.smart.pay.hub.sph.outbox.OutboxStatus;
import com.payments.smart.pay.hub.sph.outbox.PaymentOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEvent, UUID> {
	List<PaymentOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}