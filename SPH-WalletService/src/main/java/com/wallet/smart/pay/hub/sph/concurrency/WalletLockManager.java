package com.wallet.smart.pay.hub.sph.concurrency;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;
//this class manages locks for wallet operations, ensuring that concurrent access to the same wallet is properly synchronized. It uses a ConcurrentHashMap to store locks for each wallet ID, allowing for thread-safe access and modification. The getLock method retrieves the lock for a given wallet ID, creating a new lock if one does not already exist.
@Component
public class WalletLockManager {

    private final ConcurrentHashMap<UUID, ReentrantLock> lockMap = new ConcurrentHashMap<>(); //here we use reentrant lock to allow the same thread to acquire the lock multiple times without causing a deadlock. This is useful in scenarios where a method that holds the lock calls another method that also requires the same lock.

    public ReentrantLock getLock(UUID walletId) { //here we use computeIfAbsent to ensure that only one lock is created per wallet ID, even in a concurrent environment. If a lock already exists for the given wallet ID, it will be returned; otherwise, a new lock will be created and stored in the map.
        return lockMap.computeIfAbsent(walletId, newLock -> new ReentrantLock());
    }
}
