package com.txn.smart.pay.hub.sph.exceptions;

public class ReverseTransactionException extends RuntimeException {
    public ReverseTransactionException(String message) {
        super(message);
    }

    public ReverseTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}