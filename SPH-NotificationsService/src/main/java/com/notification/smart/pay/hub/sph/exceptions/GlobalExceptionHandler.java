package com.notification.smart.pay.hub.sph.exceptions;

import com.common.smart.pay.hub.sph.dto.response.ApiResponse;
import com.common.smart.pay.hub.sph.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(ApiResponse.error("RESOURCE_NOT_FOUND", ex.getMessage()));
    }
    // 15+ more handlers needed
}