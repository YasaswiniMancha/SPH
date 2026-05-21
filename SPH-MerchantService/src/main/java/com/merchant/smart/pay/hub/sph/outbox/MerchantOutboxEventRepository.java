package com.merchant.smart.pay.hub.sph.outbox;

import com.merchant.smart.pay.hub.sph.outbox.MerchantOutboxEvent;
import com.merchant.smart.pay.hub.sph.outbox.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MerchantOutboxEventRepository extends JpaRepository<MerchantOutboxEvent, String> {

    @Query("SELECT e FROM MerchantOutboxEvent e WHERE e.status = ?1 ORDER BY e.createdAt ASC")
    List<MerchantOutboxEvent> findByStatus(OutboxEventStatus status);

    @Query("SELECT e FROM MerchantOutboxEvent e WHERE e.status = ?1 AND e.retryCount < e.maxRetries ORDER BY e.createdAt ASC LIMIT ?2")
    List<MerchantOutboxEvent> findRetryableEvents(OutboxEventStatus status, int limit);

    @Query("SELECT e FROM MerchantOutboxEvent e WHERE e.createdAt < ?1 AND e.status = ?2")
    List<MerchantOutboxEvent> findStaleEvents(Instant before, OutboxEventStatus status);

    @Query("SELECT COUNT(e) FROM MerchantOutboxEvent e WHERE e.status = ?1")
    long countByStatus(OutboxEventStatus status);
}