package com.wallet.smart.pay.hub.sph.repository;

import java.util.Optional;
import java.util.UUID;

import com.wallet.smart.pay.hub.sph.entity.Wallet;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByCustomerId(String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findWithLockById(UUID id);
}
