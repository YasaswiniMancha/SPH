package com.ledger.smart.pay.hub.sph.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaction_analytics", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "transaction_date"}))
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalDebit;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalCredit;

    @Column(nullable = false)
    private Long transactionCount;

    @Column(precision = 19, scale = 2)
    private BigDecimal averageAmount;
}