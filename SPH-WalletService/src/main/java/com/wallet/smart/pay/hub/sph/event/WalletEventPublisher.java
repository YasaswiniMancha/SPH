package com.wallet.smart.pay.hub.sph.event;

public interface WalletEventPublisher {
    void publishTransaction(WalletTransactionEvent event);
}
