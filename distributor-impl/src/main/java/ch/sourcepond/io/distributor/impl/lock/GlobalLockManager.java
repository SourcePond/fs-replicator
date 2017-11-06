package ch.sourcepond.io.distributor.impl.lock;

import ch.sourcepond.io.distributor.api.GlobalLockException;

import java.util.concurrent.TimeUnit;

public interface GlobalLockManager {

    void lockGlobally(String pPath, TimeUnit pTimeoutUnit, long pTimeout) throws GlobalLockException;

    void unlockGlobally(String pPath);
}
