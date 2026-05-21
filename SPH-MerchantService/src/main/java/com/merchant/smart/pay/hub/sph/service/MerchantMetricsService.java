package com.merchant.smart.pay.hub.sph.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom metrics using Micrometer
 */
@Slf4j
@Service
public class MerchantMetricsService {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeMerchants;
    private final Counter merchantCreatedCounter;
    private final Counter merchantApprovedCounter;
    private final Timer merchantProcessingTimer;

    public MerchantMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.activeMerchants = new AtomicInteger(0);
        
        this.merchantCreatedCounter = Counter.builder("merchant.created.total")
            .description("Total merchants created")
            .register(meterRegistry);

        this.merchantApprovedCounter = Counter.builder("merchant.approved.total")
            .description("Total merchants approved")
            .register(meterRegistry);

        this.merchantProcessingTimer = Timer.builder("merchant.processing.time")
            .description("Merchant processing time")
            .register(meterRegistry);

        // Gauge for active merchants
        meterRegistry.gauge("merchant.active.count", activeMerchants);
    }

    public void recordMerchantCreated() {
        merchantCreatedCounter.increment();
        activeMerchants.incrementAndGet();
    }

    public void recordMerchantApproved() {
        merchantApprovedCounter.increment();
    }

    public void recordProcessingTime(long durationMillis) {
        merchantProcessingTimer.record(java.time.Duration.ofMillis(durationMillis));
    }

    public void decrementActiveMerchants() {
        activeMerchants.decrementAndGet();
    }
}