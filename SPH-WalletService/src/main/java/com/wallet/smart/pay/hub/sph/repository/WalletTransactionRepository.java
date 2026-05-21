package com.wallet.smart.pay.hub.sph.repository;

import java.util.List;
import java.util.UUID;

import com.wallet.smart.pay.hub.sph.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
}
