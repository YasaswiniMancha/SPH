package com.notification.smart.pay.hub.sph.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class NotificationMetricsService {

    private final Counter notificationSentCounter;
    private final Counter notificationFailedCounter;
    private final AtomicInteger pendingNotifications;
    private final Timer sendDurationTimer;

    public NotificationMetricsService(MeterRegistry meterRegistry) {
        this.notificationSentCounter = Counter.builder("notification.sent.total")
            .description("Total notifications sent")
            .register(meterRegistry);

        this.notificationFailedCounter = Counter.builder("notification.failed.total")
            .description("Total notifications failed")
            .register(meterRegistry);

        this.sendDurationTimer = Timer.builder("notification.send.duration")
            .description("Notification send duration")
            .register(meterRegistry);

        this.pendingNotifications = new AtomicInteger(0);
        meterRegistry.gauge("notification.pending", pendingNotifications);
    }

    public void recordNotificationSent() {
        notificationSentCounter.increment();
        log.debug("Notification sent metric recorded");
    }

    public void recordNotificationFailed() {
        notificationFailedCounter.increment();
        log.debug("Notification failed metric recorded");
    }

    public void recordSendDuration(long durationMs) {
        sendDurationTimer.record(java.time.Duration.ofMillis(durationMs));
    }

    public void incrementPendingCount() {
        pendingNotifications.incrementAndGet();
    }

    public void decrementPendingCount() {
        pendingNotifications.decrementAndGet();
    }
}