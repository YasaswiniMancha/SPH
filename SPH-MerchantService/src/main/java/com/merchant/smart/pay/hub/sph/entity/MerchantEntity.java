package com.merchant.smart.pay.hub.sph.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "merchants", indexes = {
    @Index(name = "idx_merchant_code", columnList = "merchant_code", unique = true),
    @Index(name = "idx_business_email", columnList = "business_email", unique = true),
    @Index(name = "idx_is_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String merchantCode;

    @Column(nullable = false, length = 100)
    private String businessName;

    @Column(nullable = false, length = 255)
    private String businessDescription;

    @Column(nullable = false, unique = true, length = 100)
    private String businessEmail;

    @Column(nullable = false, length = 20)
    private String businessPhone;

    @Column(nullable = false, length = 255)
    private String businessAddress;

    @Column(nullable = false, length = 50)
    private String businessCity;

    @Column(nullable = false, length = 50)
    private String businessState;

    @Column(nullable = false, length = 10)
    private String businessZipCode;

    @Column(nullable = false, length = 100)
    private String contactPersonName;

    @Column(nullable = false, length = 100)
    private String contactPersonEmail;

    @Column(nullable = false, length = 20)
    private String contactPersonPhone;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private MerchantStatus status = MerchantStatus.PENDING;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyTransactionLimit = BigDecimal.valueOf(100000);

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyTransactionLimit = BigDecimal.valueOf(5000000);

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "registration_number", length = 50)
    private String registrationNumber;

    @Column(name = "bank_account_number", length = 100)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;

    public enum MerchantStatus {
        PENDING, APPROVED, REJECTED, SUSPENDED, ACTIVE, INACTIVE
    }
}