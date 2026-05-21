package com.merchant.smart.pay.hub.sph.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Java 21 Records - Immutable data carriers
 * More efficient than traditional POJOs
 */

public class MerchantRecords {

    /**
     * Immutable merchant request record
     */
    public record CreateMerchantRequest(
            @NotBlank(message = "Merchant code cannot be blank") String merchantCode,
            @NotBlank(message = "Business name cannot be blank") String businessName,
            @NotBlank(message = "Business email required") @Email String businessEmail,
            @NotBlank(message = "Business phone required") @Pattern(regexp = "^[+]?[0-9]{10,15}$") String businessPhone,
            @NotBlank(message = "Business address required") String businessAddress,
            @NotBlank(message = "City required") String businessCity,
            @NotBlank(message = "State required") String businessState,
            @NotBlank(message = "Zip code required") String businessZipCode,
            @NotBlank(message = "Contact name required") String contactPersonName,
            @NotBlank(message = "Contact email required") @Email String contactPersonEmail,
            String taxId,
            String bankAccountNumber
    ) {}

    /**
     * Immutable merchant response record
     */
    public record MerchantResponse(
            String id,
            String merchantCode,
            String businessName,
            String businessEmail,
            String status,
            BigDecimal dailyTransactionLimit,
            Boolean isActive,
            LocalDateTime createdAt
    ) {}

    /**
     * Immutable merchant event record
     */
    public record MerchantEvent(
            String merchantId,
            String merchantCode,
            String eventType,
            String details,
            LocalDateTime timestamp
    ) {}

    /**
     * Immutable merchant metrics record
     */
    public record MerchantMetrics(
            long totalMerchants,
            long activeMerchants,
            long pendingMerchants,
            long rejectedMerchants,
            double totalTransactionVolume,
            LocalDateTime lastUpdated
    ) {}
}