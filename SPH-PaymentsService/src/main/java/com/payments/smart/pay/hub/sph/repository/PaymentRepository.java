package com.payments.smart.pay.hub.sph.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.payments.smart.pay.hub.sph.entity.Payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
	Optional<Payment> findByMerchantIdAndReferenceId(String merchantId, String referenceId);

	List<Payment> findByMerchantIdOrderByCreatedAtDesc(String merchantId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<Payment> findWithLockById(UUID id);
}