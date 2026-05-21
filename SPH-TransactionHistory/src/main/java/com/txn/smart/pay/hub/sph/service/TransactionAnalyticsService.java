package com.txn.smart.pay.hub.sph.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.txn.smart.pay.hub.sph.dto.response.TransactionAnalyticsResponse;
import com.txn.smart.pay.hub.sph.entity.Transaction;
import com.txn.smart.pay.hub.sph.mongo.MongoTransactionAnalytics;
import com.txn.smart.pay.hub.sph.repository.MongoAnalyticsRepository;
import com.txn.smart.pay.hub.sph.repository.TransactionRepository;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionAnalyticsService {

    private final MongoAnalyticsRepository analyticsRepository;
    private final MongoTemplate mongoTemplate;
    private final TransactionRepository transactionRepository;

    @Async("analyticsExecutor")
    @Timed(value = "analytics.compute.daily", description = "Compute daily analytics")
    @Scheduled(fixedDelay = 3600000) // Run every hour
    public void computeDailyAnalytics() {
        log.info("Starting daily analytics computation");

        LocalDate today = LocalDate.now();

        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> t.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().isEqual(today))
                .toList();

        if (transactions.isEmpty()) {
            log.info("No transactions found for today");
            return;
        }

        // Group by userId concurrently
        ConcurrentHashMap<String, List<Transaction>> groupedByUser = new ConcurrentHashMap<>();
        transactions.parallelStream()
                .forEach(t -> groupedByUser.computeIfAbsent(t.getUserId(), k -> new java.util.ArrayList<>()).add(t));

        // Process each user's analytics
        groupedByUser.forEach((userId, userTransactions) -> {
            computeUserAnalytics(userId, userTransactions, today);
        });

        log.info("Daily analytics computation completed. Processed {} users", groupedByUser.size());
    }

    @Async("analyticsExecutor")
    @Timed(value = "analytics.compute.bulk", description = "Bulk compute analytics")
    public CompletableFuture<Void> computeBulkAnalytics(List<String> userIds) {
        log.info("Computing analytics for {} users", userIds.size());

        userIds.parallelStream()
                .forEach(userId -> {
                    List<Transaction> userTransactions = transactionRepository.findAll().stream()
                            .filter(t -> t.getUserId().equals(userId))
                            .toList();

                    if (!userTransactions.isEmpty()) {
                        computeUserAnalytics(userId, userTransactions, LocalDate.now());
                    }
                });

        log.info("Bulk analytics computation completed for {} users", userIds.size());
        return CompletableFuture.completedFuture(null);
    }

    // Real-time update after transaction completion
    @Async("analyticsExecutor")
    @Timed(value = "analytics.update.realtime", description = "Real-time analytics update")
    public void updateTransactionAnalyticsRealTime(Transaction transaction) {
        try {
            LocalDate txnDate = transaction.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            String userId = transaction.getUserId();

            // Find or create daily analytics document
            MongoTransactionAnalytics analytics = analyticsRepository
                    .findByUserIdAndDate(userId, txnDate)
                    .orElse(MongoTransactionAnalytics.builder()
                            .userId(userId)
                            .transactionDate(txnDate)
                            .walletId(transaction.getWalletId())
                            .totalCredit(BigDecimal.ZERO)
                            .totalDebit(BigDecimal.ZERO)
                            .totalRefund(BigDecimal.ZERO)
                            .creditCount(0)
                            .debitCount(0)
                            .refundCount(0)
                            .createdAt(LocalDateTime.now())
                            .ttlSeconds(90 * 24 * 3600)  // Auto-delete after 90 days
                            .build());

            // Update based on transaction type
            updateAnalyticsWithTransaction(analytics, transaction);

            analytics.setUpdatedAt(LocalDateTime.now());
            analyticsRepository.save(analytics);
            log.info("Analytics updated for userId: {}, date: {}", userId, txnDate);

        } catch (Exception e) {
            log.error("Failed to update analytics for transaction: {}", transaction.getId(), e);
        }
    }

    // Bulk upsert for high-throughput scenarios
    @Async("analyticsExecutor")
    @Timed(value = "analytics.bulk.update", description = "Bulk analytics update")
    public void bulkUpdateAnalytics(List<Transaction> transactions) {
        try {
            if (transactions.isEmpty()) {
                log.debug("No transactions provided for bulk analytics update");
                return;
            }

            Map<String, MongoTransactionAnalytics> analyticsMap = new ConcurrentHashMap<>();

            // Group by userId + date
            transactions.parallelStream().forEach(txn -> {
                String key = txn.getUserId() + "_" + txn.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                analyticsMap.computeIfAbsent(key, k -> {
                    LocalDate txnDate = txn.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
                    return analyticsRepository
                            .findByUserIdAndDate(txn.getUserId(), txnDate)
                            .orElse(MongoTransactionAnalytics.builder()
                                    .userId(txn.getUserId())
                                    .transactionDate(txnDate)
                                    .totalCredit(BigDecimal.ZERO)
                                    .totalDebit(BigDecimal.ZERO)
                                    .totalRefund(BigDecimal.ZERO)
                                    .creditCount(0)
                                    .debitCount(0)
                                    .refundCount(0)
                                    .createdAt(LocalDateTime.now())
                                    .ttlSeconds(90 * 24 * 3600)
                                    .build());
                });

                // Update with transaction
                updateAnalyticsWithTransaction(analyticsMap.get(key), txn);
            });

            // Bulk save to MongoDB using upsert
            BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, //here unordered is used to improve performance as order is not important for analytics updates, Runs faster, parallel-ish, continues even if one fails.
                    MongoTransactionAnalytics.class);

            analyticsMap.values().forEach(analytics -> {
                Query query = Query.query(
                        Criteria.where("userId").is(analytics.getUserId())
                                .and("transactionDate").is(analytics.getTransactionDate())
                );
                Update update = new Update()
                        .set("totalCredit", analytics.getTotalCredit())
                        .set("totalDebit", analytics.getTotalDebit())
                        .set("totalRefund", analytics.getTotalRefund())
                        .set("creditCount", analytics.getCreditCount())
                        .set("debitCount", analytics.getDebitCount())
                        .set("refundCount", analytics.getRefundCount())
                        .set("updatedAt", LocalDateTime.now());

                ops.upsert(query, update);
            });

            ops.execute();
            log.info("Bulk analytics update completed for {} records", transactions.size());

        } catch (Exception e) {
            log.error("Bulk analytics update failed", e);
        }
    }

    // Query analytics for a user in date range
    @Cacheable(value = "user-analytics", key = "'range_' + #userId + '_' + #startDate + '_' + #endDate")
    @Timed(value = "analytics.get.daterange", description = "Get user analytics by date range")
    public List<TransactionAnalyticsResponse> getUserAnalyticsByDateRange(
            String userId, LocalDate startDate, LocalDate endDate) {

        List<MongoTransactionAnalytics> analytics = analyticsRepository
                .findByUserIdAndDateRange(userId, startDate, endDate);

        return analytics.stream()
                .map(this::mapMongoToResponse)
                .collect(Collectors.toList());
    }

    // Get aggregated statistics
    @Timed(value = "analytics.get.statistics", description = "Get user statistics")
    public Map<String, Object> getUserStatistics(String userId) {
        Optional<MongoAnalyticsRepository.UserStatsResult> stats = analyticsRepository
                .getUserStatistics(userId);

        if (stats.isPresent()) {
            MongoAnalyticsRepository.UserStatsResult result = stats.get();
            return Map.of(
                    "userId", userId,
                    "totalTransactions", result.getTotalTransactions(),
                    "totalAmount", result.getTotalAmount(),
                    "avgAmount", result.getAvgAmount()
            );
        }
        return Map.of("userId", userId, "totalTransactions", 0, "totalAmount", BigDecimal.ZERO, "avgAmount", BigDecimal.ZERO);
    }

    // Private helper methods

    private void computeUserAnalytics(String userId, List<Transaction> transactions, LocalDate date) {
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        int creditCount = 0;
        int debitCount = 0;
        int refundCount = 0;

        for (Transaction t : transactions) {
            switch (t.getTransactionType()) {
                case "DEBIT" -> {
                    totalDebit = totalDebit.add(t.getAmount());
                    debitCount++;
                }
                case "CREDIT" -> {
                    totalCredit = totalCredit.add(t.getAmount());
                    creditCount++;
                }
                case "REFUND" -> {
                    totalRefund = totalRefund.add(t.getAmount());
                    refundCount++;
                }
            }
        }

        BigDecimal avgAmount = transactions.isEmpty()
                ? BigDecimal.ZERO
                : totalDebit.add(totalCredit).add(totalRefund)
                .divide(BigDecimal.valueOf(transactions.size()), 2, java.math.RoundingMode.HALF_UP);

        MongoTransactionAnalytics analytics = analyticsRepository
                .findByUserIdAndDate(userId, date)
                .orElse(MongoTransactionAnalytics.builder()
                        .userId(userId)
                        .transactionDate(date)
                        .createdAt(LocalDateTime.now())
                        .ttlSeconds(90 * 24 * 3600)
                        .build());

        analytics.setTotalDebit(totalDebit);
        analytics.setTotalCredit(totalCredit);
        analytics.setTotalRefund(totalRefund);
        analytics.setDebitCount(debitCount);
        analytics.setCreditCount(creditCount);
        analytics.setRefundCount(refundCount);
        analytics.setUpdatedAt(LocalDateTime.now());

        analyticsRepository.save(analytics);
        log.debug("Analytics saved for user: {} on date: {}", userId, date);
    }

    private void updateAnalyticsWithTransaction(MongoTransactionAnalytics analytics, Transaction txn) {
        switch (txn.getTransactionType()) {
            case "CREDIT" -> {
                analytics.setTotalCredit(analytics.getTotalCredit().add(txn.getAmount()));
                analytics.setCreditCount(analytics.getCreditCount() + 1);
            }
            case "DEBIT" -> {
                analytics.setTotalDebit(analytics.getTotalDebit().add(txn.getAmount()));
                analytics.setDebitCount(analytics.getDebitCount() + 1);
            }
            case "REFUND" -> {
                analytics.setTotalRefund(analytics.getTotalRefund().add(txn.getAmount()));
                analytics.setRefundCount(analytics.getRefundCount() + 1);
            }
        }
    }

    private TransactionAnalyticsResponse mapMongoToResponse(MongoTransactionAnalytics analytics) {
        BigDecimal net = analytics.getTotalCredit().subtract(analytics.getTotalDebit());
        return new TransactionAnalyticsResponse(
                analytics.getUserId(),
                analytics.getTransactionDate(),
                analytics.getTotalDebit(),
                analytics.getTotalCredit(),
                (long) (analytics.getDebitCount() + analytics.getCreditCount() + analytics.getRefundCount()),
                analytics.getTotalCredit().add(analytics.getTotalDebit()).add(analytics.getTotalRefund())
                        .divide(BigDecimal.valueOf(Math.max(1, analytics.getDebitCount() + analytics.getCreditCount() + analytics.getRefundCount())), 2, java.math.RoundingMode.HALF_UP),
                net
        );
    }
}