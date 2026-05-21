package com.wallet.smart.pay.hub.sph.repository;

import com.wallet.smart.pay.hub.sph.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
}
