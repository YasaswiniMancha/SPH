package com.wallet.smart.pay.hub.sph.repository;

import java.util.List;

import com.wallet.smart.pay.hub.sph.outbox.OutboxStatus;
import com.wallet.smart.pay.hub.sph.outbox.WalletOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletOutboxEventRepository extends JpaRepository<WalletOutboxEvent, java.util.UUID> {
    List<WalletOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
