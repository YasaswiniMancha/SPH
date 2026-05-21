package com.payments.smart.pay.hub.sph.repository;

import java.util.List;
import java.util.UUID;

import com.payments.smart.pay.hub.sph.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
	List<PaymentTransaction> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
}