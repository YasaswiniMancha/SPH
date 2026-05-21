package com.payments.smart.pay.hub.sph.concurrency;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

/**
 * Manages locks for payment operations, ensuring that concurrent access 
 * to the same payment is properly synchronized.
 */
@Component
public class PaymentLockManager {

	private final ConcurrentHashMap<UUID, ReentrantLock> lockMap = new ConcurrentHashMap<>();

	public ReentrantLock getLock(UUID paymentId) {
		return lockMap.computeIfAbsent(paymentId, newLock -> new ReentrantLock());
	}
}