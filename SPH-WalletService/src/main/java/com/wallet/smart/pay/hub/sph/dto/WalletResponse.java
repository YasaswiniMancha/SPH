package com.wallet.smart.pay.hub.sph.dto;

import java.math.BigDecimal;
import java.util.UUID;

//record class is used to create immutable data transfer objects (DTOs) in Java. It provides a concise syntax for defining classes that are primarily used to hold data. In this case, the WalletResponse record class is designed to represent the response of a wallet-related API call.
public record WalletResponse(
    UUID walletId,
    String customerId,
    String currency,
    BigDecimal balance,
    String status
) {
}
