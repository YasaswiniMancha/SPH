package com.wallet.smart.pay.hub.sph.exception;

public class WalletServiceException extends RuntimeException {
    public WalletServiceException(String message) {
        super(message);
    }

    public WalletServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public WalletServiceException(Throwable cause) {
        super(cause);
    }
}

