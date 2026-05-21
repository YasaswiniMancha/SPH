package com.merchant.smart.pay.hub.sph.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantDTO {
    private String id;

    @NotBlank(message = "Merchant code cannot be blank")
    @Size(min = 3, max = 50, message = "Merchant code must be 3-50 characters")
    private String merchantCode;

    @NotBlank(message = "Business name cannot be blank")
    @Size(min = 5, max = 100, message = "Business name must be 5-100 characters")
    private String businessName;

    @NotBlank(message = "Business description cannot be blank")
    private String businessDescription;

    @NotBlank(message = "Business email is required")
    @Email(message = "Business email must be valid")
    private String businessEmail;

    @NotBlank(message = "Business phone is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String businessPhone;

    @NotBlank(message = "Business address is required")
    private String businessAddress;

    @NotBlank(message = "City is required")
    private String businessCity;

    @NotBlank(message = "State is required")
    private String businessState;

    @NotBlank(message = "Zip code is required")
    private String businessZipCode;

    @NotBlank(message = "Contact person name is required")
    private String contactPersonName;

    @NotBlank(message = "Contact person email is required")
    @Email(message = "Contact person email must be valid")
    private String contactPersonEmail;

    @NotBlank(message = "Contact person phone is required")
    private String contactPersonPhone;

    private String status;
    private BigDecimal dailyTransactionLimit;
    private BigDecimal monthlyTransactionLimit;
    private Boolean isActive;
    private String taxId;
    private String registrationNumber;
    private String bankAccountNumber;
    private String bankName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime verifiedAt;
}