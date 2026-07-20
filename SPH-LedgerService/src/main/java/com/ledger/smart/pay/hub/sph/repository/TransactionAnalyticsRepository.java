package com.ledger.smart.pay.hub.sph.repository;

import com.ledger.smart.pay.hub.sph.entity.TransactionAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionAnalyticsRepository extends JpaRepository<TransactionAnalytics, Long> {

    /**
     * Find analytics for a specific user on a specific date
     */
    Optional<TransactionAnalytics> findByUserIdAndTransactionDate(String userId, LocalDate date);

    /**
     * Find all analytics for a user (ordered by date descending)
     */
    List<TransactionAnalytics> findByUserId(String userId);

    /**
     * Find all analytics for a user ordered by transaction date descending
     */
    List<TransactionAnalytics> findByUserIdOrderByTransactionDateDesc(String userId);

    /**
     * Find analytics between date range
     */
    @Query("SELECT ta FROM TransactionAnalytics ta WHERE ta.userId = :userId AND ta.transactionDate BETWEEN :startDate AND :endDate ORDER BY ta.transactionDate DESC")
    List<TransactionAnalytics> findByUserIdAndDateRange(
        @Param("userId") String userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get total debit for user in date range
     */
    @Query("SELECT COALESCE(SUM(ta.totalDebit), 0) FROM TransactionAnalytics ta WHERE ta.userId = :userId AND ta.transactionDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal getTotalDebitByDateRange(
        @Param("userId") String userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get total credit for user in date range
     */
    @Query("SELECT COALESCE(SUM(ta.totalCredit), 0) FROM TransactionAnalytics ta WHERE ta.userId = :userId AND ta.transactionDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal getTotalCreditByDateRange(
        @Param("userId") String userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get total transaction count for user in date range
     */
    @Query("SELECT COALESCE(SUM(ta.transactionCount), 0) FROM TransactionAnalytics ta WHERE ta.userId = :userId AND ta.transactionDate BETWEEN :startDate AND :endDate")
    Long getTotalTransactionCountByDateRange(
        @Param("userId") String userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get latest analytics for a user
     */
    @Query(value = "SELECT * FROM transaction_analytics WHERE user_id = :userId ORDER BY transaction_date DESC LIMIT 1", nativeQuery = true)
    Optional<TransactionAnalytics> findLatestByUserId(@Param("userId") String userId);

    /**
     * Check if analytics exist for a user on a specific date
     */
    boolean existsByUserIdAndTransactionDate(String userId, LocalDate date);

    /**
     * Delete analytics for a specific date
     */
    void deleteByTransactionDate(LocalDate date);

    /**
     * Delete analytics older than a specific date
     */
    @Query("DELETE FROM TransactionAnalytics ta WHERE ta.transactionDate < :date")
    void deleteOlderThan(@Param("date") LocalDate date);

    /**
     * Count analytics records for a user
     */
    long countByUserId(String userId);

    /**
     * Get average amount for all analytics
     */
    @Query("SELECT AVG(ta.averageAmount) FROM TransactionAnalytics ta WHERE ta.userId = :userId")
    java.math.BigDecimal getAverageAmountByUserId(@Param("userId") String userId);
}