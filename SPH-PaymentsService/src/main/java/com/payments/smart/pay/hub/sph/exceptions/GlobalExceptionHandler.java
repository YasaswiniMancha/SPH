package com.payments.smart.pay.hub.sph.exceptions;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(Map.of("timestamp", Instant.now(), "error", "NOT_FOUND", "message", ex.getMessage()));
	}

	@ExceptionHandler(BadRequestException.class)
	ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(Map.of("timestamp", Instant.now(), "error", "BAD_REQUEST", "message", ex.getMessage()));
	}

	@ExceptionHandler(PaymentServiceException.class)
	ResponseEntity<Map<String, Object>> handlePaymentServiceException(PaymentServiceException ex) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(Map.of("timestamp", Instant.now(), "error", "SERVICE_UNAVAILABLE", 
				"message", ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getAllErrors().stream()
			.findFirst()
			.map(error -> error instanceof FieldError fieldError
				? fieldError.getField() + ": " + fieldError.getDefaultMessage()
				: error.getDefaultMessage())
			.orElse("Validation failed");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(Map.of("timestamp", Instant.now(), "error", "VALIDATION_ERROR", "message", message));
	}

	@ExceptionHandler(RuntimeException.class)
	ResponseEntity<Map<String, Object>> handleGenericException(RuntimeException ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(Map.of("timestamp", Instant.now(), "error", "INTERNAL_ERROR", 
				"message", "An unexpected error occurred. Please contact support."));
	}
}