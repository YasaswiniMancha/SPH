package com.merchant.smart.pay.hub.sph.event;

import java.time.LocalDateTime;

/**
 * Java 15+ Sealed Classes - Type-safe event hierarchy
 * Prevents unauthorized subclasses
 */
public sealed abstract class MerchantEventTypes permits
        MerchantCreatedEvent,
        MerchantUpdatedEvent,
        MerchantApprovedEvent,
        MerchantRejectedEvent,
        MerchantSuspendedEvent {

    public abstract String getEventType();
    public abstract LocalDateTime getTimestamp();
    public abstract String getMerchantId();
}

final class MerchantCreatedEvent extends MerchantEventTypes {
    private final String merchantId;
    private final String merchantCode;
    private final LocalDateTime timestamp;

    public MerchantCreatedEvent(String merchantId, String merchantCode) {
        this.merchantId = merchantId;
        this.merchantCode = merchantCode;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return "MERCHANT_CREATED";
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getMerchantId() {
        return merchantId;
    }
}

final class MerchantUpdatedEvent extends MerchantEventTypes {
    private final String merchantId;
    private final LocalDateTime timestamp;

    public MerchantUpdatedEvent(String merchantId) {
        this.merchantId = merchantId;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return "MERCHANT_UPDATED";
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getMerchantId() {
        return merchantId;
    }
}

final class MerchantApprovedEvent extends MerchantEventTypes {
    private final String merchantId;
    private final String approvedBy;
    private final LocalDateTime timestamp;

    public MerchantApprovedEvent(String merchantId, String approvedBy) {
        this.merchantId = merchantId;
        this.approvedBy = approvedBy;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return "MERCHANT_APPROVED";
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getMerchantId() {
        return merchantId;
    }
}

final class MerchantRejectedEvent extends MerchantEventTypes {
    private final String merchantId;
    private final String reason;
    private final LocalDateTime timestamp;

    public MerchantRejectedEvent(String merchantId, String reason) {
        this.merchantId = merchantId;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return "MERCHANT_REJECTED";
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getMerchantId() {
        return merchantId;
    }
}

final class MerchantSuspendedEvent extends MerchantEventTypes {
    private final String merchantId;
    private final String suspensionReason;
    private final LocalDateTime timestamp;

    public MerchantSuspendedEvent(String merchantId, String suspensionReason) {
        this.merchantId = merchantId;
        this.suspensionReason = suspensionReason;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String getEventType() {
        return "MERCHANT_SUSPENDED";
    }

    @Override
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getMerchantId() {
        return merchantId;
    }
}