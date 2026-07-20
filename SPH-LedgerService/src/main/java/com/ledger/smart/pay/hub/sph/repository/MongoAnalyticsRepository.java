package com.ledger.smart.pay.hub.sph.repository;

import com.ledger.smart.pay.hub.sph.mongo.MongoTransactionAnalytics;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MongoAnalyticsRepository extends MongoRepository<MongoTransactionAnalytics, String> {

    // Find analytics by userId and date
    @Query("{'userId': ?0, 'transactionDate': ?1}")
    Optional<MongoTransactionAnalytics> findByUserIdAndDate(String userId, LocalDate date);

    // Find all analytics for a user in a date range
    @Query("{'userId': ?0, 'transactionDate': {$gte: ?1, $lte: ?2}}")
    List<MongoTransactionAnalytics> findByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate);

    // Aggregation: Total debit for user in date range
    @Aggregation(pipeline = {
        "{$match: {'userId': ?0, 'transactionDate': {$gte: ?1, $lte: ?2}}}",
        "{$group: {_id: null, totalDebit: {$sum: '$totalDebit'}}}"
    })
    Optional<AggregateResult> sumDebitByDateRange(String userId, LocalDate startDate, LocalDate endDate);

    // Aggregation: User statistics
    @Aggregation(pipeline = {
        "{$match: {'userId': ?0}}",
        "{$group: {_id: '$userId', " +
            "totalTransactions: {$sum: {$add: ['$creditCount', '$debitCount']}}, " +
            "totalAmount: {$sum: {$add: ['$totalCredit', '$totalDebit']}}, " +
            "avgAmount: {$avg: '$avgTransactionAmount'}}}"
    })
    Optional<UserStatsResult> getUserStatistics(String userId);

    interface AggregateResult {
        java.math.BigDecimal getTotalDebit();
    }

    interface UserStatsResult {
        String getId();
        Integer getTotalTransactions();
        java.math.BigDecimal getTotalAmount();
        java.math.BigDecimal getAvgAmount();
    }
}