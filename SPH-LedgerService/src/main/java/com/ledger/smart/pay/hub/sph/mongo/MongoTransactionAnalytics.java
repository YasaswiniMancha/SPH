package com.ledger.smart.pay.hub.sph.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "transaction_analytics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "userId_txnDate_idx", def = "{'userId': 1, 'transactionDate': -1}"),
    @CompoundIndex(name = "userId_txnType_idx", def = "{'userId': 1, 'transactionType': 1}")
})
public class MongoTransactionAnalytics {

    @Id
    private String id;  // MongoDB ObjectId

    @Indexed
    private String userId;

    @Indexed
    private LocalDate transactionDate;

    private String walletId;
    private String merchantId;
    private String transactionType;  // CREDIT, DEBIT, REFUND

    // Daily aggregates
    private BigDecimal totalCredit;
    private BigDecimal totalDebit;
    private BigDecimal totalRefund;
    private Integer creditCount;
    private Integer debitCount;
    private Integer refundCount;

    // Extended metrics
    private BigDecimal avgTransactionAmount;
    private BigDecimal maxTransactionAmount;
    private BigDecimal minTransactionAmount;

    // Hourly breakdown (optional: array of hourly buckets)
    @Builder.Default
    private java.util.List<HourlyMetrics> hourlyMetrics = new java.util.ArrayList<>();

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer ttlSeconds;  // For MongoDB TTL index auto-cleanup

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlyMetrics {
        private Integer hour;  // 0-23
        private BigDecimal credit;
        private BigDecimal debit;
        private Integer count;
    }
}