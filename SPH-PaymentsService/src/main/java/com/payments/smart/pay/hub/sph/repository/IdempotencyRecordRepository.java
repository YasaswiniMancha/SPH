package com.payments.smart.pay.hub.sph.repository;

import java.util.Optional;

import com.payments.smart.pay.hub.sph.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
	Optional<IdempotencyRecord> findById(String idempotencyKey);
}