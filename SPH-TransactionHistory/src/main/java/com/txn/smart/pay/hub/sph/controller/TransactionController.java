package com.txn.smart.pay.hub.sph.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.txn.smart.pay.hub.sph.dto.request.TransactionFilterRequest;
import com.txn.smart.pay.hub.sph.dto.request.TransactionRequest;
import com.txn.smart.pay.hub.sph.dto.response.TransactionResponse;
import com.txn.smart.pay.hub.sph.service.TransactionService;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Timed(value = "http.transaction.create", description = "HTTP create transaction")
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        log.info("Create transaction request for user: {}", request.userId());
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Timed(value = "http.transaction.get.user", description = "HTTP get user transactions")
    public ResponseEntity<Page<TransactionResponse>> getUserTransactions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("Fetching transactions for user: {}", userId);
        Page<TransactionResponse> transactions = transactionService.getUserTransactions(userId, page, pageSize);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/filter")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "http.transaction.filter", description = "HTTP filter transactions")
    public ResponseEntity<Page<TransactionResponse>> filterTransactions(@Valid @RequestBody TransactionFilterRequest filter) {
        log.info("Filtering transactions with criteria");
        Page<TransactionResponse> transactions = transactionService.filterTransactions(filter);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{transactionId}")
    @Timed(value = "http.transaction.get.id", description = "HTTP get transaction by ID")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        return transactionService.getTransactionById(transactionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{transactionId}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "http.transaction.reverse", description = "HTTP reverse transaction")
    public ResponseEntity<TransactionResponse> reverseTransaction(@PathVariable String transactionId) {
        log.info("Reversing transaction: {}", transactionId);
        TransactionResponse response = transactionService.reverseTransaction(transactionId);
        return ResponseEntity.ok(response);
    }
}