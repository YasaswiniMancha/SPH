package com.ledger.smart.pay.hub.sph.exceptions;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public InsufficientBalanceException(String walletId, BigDecimal amount, BigDecimal balance) {
        super("Insufficient balance for wallet: " + walletId + ". Required: " + amount + ", Available: " + balance);
    }
}