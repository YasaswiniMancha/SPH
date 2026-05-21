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
public class MerchantServiceWebClient {

    private final WebClient merchantServiceWebClient;

    @Timed(value = "merchant.api.validate.call", description = "Merchant validation API call")
    public Mono<MerchantResponse> validateMerchant(String merchantId) {
        return merchantServiceWebClient
            .get()
            .uri("/api/v1/merchants/{merchantId}", merchantId)
            .retrieve()
            .bodyToMono(MerchantResponse.class)
            .timeout(Duration.ofSeconds(3))
            .retryWhen(Retry.backoff(2, Duration.ofMillis(100)))
            .doOnError(ex -> log.error("Failed to validate merchant: {}", merchantId, ex))
            .onErrorResume(ex -> Mono.just(fallbackMerchant(merchantId)));
    }

    private MerchantResponse fallbackMerchant(String merchantId) {
        log.warn("Using fallback merchant for: {}", merchantId);
        return new MerchantResponse(merchantId, "UNKNOWN", false);
    }

    public record MerchantResponse(
        String merchantId,
        String name,
        boolean active
    ) {}
}