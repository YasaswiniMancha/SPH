package com.ledger.smart.pay.hub.sph.exceptions;

public class MerchantValidationException extends RuntimeException {
    public MerchantValidationException(String message) {
        super(message);
    }

    public MerchantValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}