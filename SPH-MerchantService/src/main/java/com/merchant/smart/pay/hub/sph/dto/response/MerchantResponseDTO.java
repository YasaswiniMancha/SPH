package com.merchant.smart.pay.hub.sph.dto.response;

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
public class MerchantResponseDTO {
    private String id;
    private String merchantCode;
    private String businessName;
    private String businessEmail;
    private String businessPhone;
    private String status;
    private BigDecimal dailyTransactionLimit;
    private BigDecimal monthlyTransactionLimit;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
}