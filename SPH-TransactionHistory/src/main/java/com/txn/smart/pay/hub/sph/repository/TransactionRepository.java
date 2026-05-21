package com.txn.smart.pay.hub.sph.repository;


import com.txn.smart.pay.hub.sph.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findByUserId(String userId, Pageable pageable);

    Page<Transaction> findByWalletId(String walletId, Pageable pageable);

    Page<Transaction> findByMerchantId(String merchantId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findByUserIdAndStatusBetweenDates(
        @Param("userId") String userId,
        @Param("status") String status,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate,
        Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findTransactionsByDateRange(
        @Param("userId") String userId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );

    Optional<Transaction> findByReferenceId(String referenceId);

    long countByUserId(String userId);

    long countByUserIdAndStatus(String userId, String status);
}