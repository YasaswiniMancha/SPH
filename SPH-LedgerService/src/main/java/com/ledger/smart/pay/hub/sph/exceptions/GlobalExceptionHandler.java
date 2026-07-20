package com.ledger.smart.pay.hub.sph.exceptions;

import com.ledger.smart.pay.hub.sph.exceptions.AnalyticsException;
import com.ledger.smart.pay.hub.sph.exceptions.ReverseTransactionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


// Global exception handler for the Transaction History service- to provide consistent error responses and logging across the application.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ReverseTransactionException.class)  //Handles errors that occur during transaction reversal, such as invalid transaction state or insufficient permissions.
    public ResponseEntity<ErrorResponse> handleReverseTransaction(ReverseTransactionException ex) {
        log.warn("Failed to reverse transaction: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                "REVERSE_TRANSACTION_FAILED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                Instant.now()
            ));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(TransactionNotFoundException ex) {
        log.warn("Transaction not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(
                "TRANSACTION_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                Instant.now()
            ));
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransaction(InvalidTransactionException ex) {
        log.warn("Invalid transaction: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                "INVALID_TRANSACTION",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                Instant.now()
            ));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        log.warn("Insufficient balance: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(new ErrorResponse(
                "INSUFFICIENT_BALANCE",
                ex.getMessage(),
                HttpStatus.PAYMENT_REQUIRED.value(),
                Instant.now()
            ));
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTransaction(DuplicateTransactionException ex) {
        log.warn("Duplicate transaction: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(
                "DUPLICATE_TRANSACTION",
                ex.getMessage(),
                HttpStatus.CONFLICT.value(),
                Instant.now()
            ));
    }

    @ExceptionHandler(WalletServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleWalletServiceUnavailable(WalletServiceUnavailableException ex) {
        log.error("Wallet service unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(
                "WALLET_SERVICE_UNAVAILABLE",
                ex.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                Instant.now()
            ));
    }

    @ExceptionHandler(MerchantValidationException.class)
    public ResponseEntity<ErrorResponse> handleMerchantValidation(MerchantValidationException ex) {
        log.warn("Merchant validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                "MERCHANT_VALIDATION_FAILED",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                Instant.now()
            ));
    }

    @ExceptionHandler(AnalyticsException.class)
    public ResponseEntity<ErrorResponse> handleAnalyticsException(AnalyticsException ex) {
        log.error("Analytics error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                "ANALYTICS_ERROR",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                Instant.now()
            ));
    }

    @ExceptionHandler(TransactionServiceException.class)
    public ResponseEntity<ErrorResponse> handleTransactionServiceException(TransactionServiceException ex) {
        log.error("Transaction service error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                "SERVICE_ERROR",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                Instant.now()
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(fieldName, message);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("error", "VALIDATION_ERROR");
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Validation failed");
        response.put("details", errors);

        log.warn("Validation error: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGenericException(RuntimeException ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please contact support.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                Instant.now()
            ));
    }

    public record ErrorResponse(
        String error,
        String message,
        int status,
        Instant timestamp
    ) {}
}