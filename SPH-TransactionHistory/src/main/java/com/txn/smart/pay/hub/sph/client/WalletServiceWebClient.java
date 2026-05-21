package com.txn.smart.pay.hub.sph.client;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceWebClient {

    private final WebClient walletServiceWebClient;

    @Timed(value = "wallet.api.balance.call", description = "Wallet balance API call")
    public Mono<WalletResponse> getWalletBalance(String walletId) {
        return walletServiceWebClient
            .get()
            .uri("/api/v1/wallets/{walletId}/balance", walletId)
            .retrieve()
            .bodyToMono(WalletResponse.class)
            .timeout(Duration.ofSeconds(3))
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
            .doOnError(ex -> log.error("Failed to fetch wallet balance: {}", walletId, ex))
            .onErrorResume(ex -> Mono.just(fallbackWallet(walletId)));
    }

    @Timed(value = "wallet.api.deduct.call", description = "Wallet deduction API call")
    public Mono<DeductResponse> deductFromWallet(String walletId, java.math.BigDecimal amount) {
        DeductRequest request = new DeductRequest(walletId, amount);
        
        return walletServiceWebClient
            .post()
            .uri("/api/v1/wallets/{walletId}/deduct", walletId)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(DeductResponse.class)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
            .doOnError(ex -> log.error("Failed to deduct from wallet: {}", walletId, ex))
            .onErrorResume(ex -> Mono.just(fallbackDeduct(walletId)));
    }

    private WalletResponse fallbackWallet(String walletId) {
        log.warn("Using fallback wallet for: {}", walletId);
        return new WalletResponse(walletId, java.math.BigDecimal.ZERO, "UNAVAILABLE");
    }

    private DeductResponse fallbackDeduct(String walletId) {
        log.warn("Using fallback deduct for: {}", walletId);
        return new DeductResponse(walletId, false, "SERVICE_UNAVAILABLE");
    }

    public record WalletResponse(
        String walletId,
        java.math.BigDecimal balance,
        String status
    ) {}

    public record DeductRequest(
        String walletId,
        java.math.BigDecimal amount
    ) {}

    public record DeductResponse(
        String walletId,
        boolean success,
        String message
    ) {}
}