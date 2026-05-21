package com.wallet.smart.pay.hub.sph.controller;

import java.util.UUID;

import com.wallet.smart.pay.hub.sph.dto.*;
import com.wallet.smart.pay.hub.sph.service.WalletService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletService.createWallet(request));
    }

    @GetMapping("/{walletId}/balance")
    public WalletResponse getBalance(@PathVariable UUID walletId) {
        return walletService.getBalance(walletId);
    }

    @PostMapping("/{walletId}/credit")
    public TransactionResponse credit(
        @PathVariable UUID walletId,
        @Valid @RequestBody AmountRequest request,
        @RequestHeader(name = IDEMPOTENCY_HEADER, required = false) String idempotencyKey
    ) {
        return walletService.credit(walletId, request, idempotencyKey);
    }

    @PostMapping("/{walletId}/debit")
    public TransactionResponse debit(
        @PathVariable UUID walletId,
        @Valid @RequestBody AmountRequest request,
        @RequestHeader(name = IDEMPOTENCY_HEADER, required = false) String idempotencyKey
    ) {
        return walletService.debit(walletId, request, idempotencyKey);
    }

    @PostMapping("/transfers")
    public TransactionResponse transfer(
        @Valid @RequestBody TransferRequest request,
        @RequestHeader(name = IDEMPOTENCY_HEADER, required = false) String idempotencyKey
    ) {
        return walletService.transfer(request, idempotencyKey);
    }
}
