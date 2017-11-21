/*Copyright (C) 2017 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.distributor.impl.lock;

import ch.sourcepond.io.distributor.api.exception.LockException;
import ch.sourcepond.io.distributor.api.exception.UnlockException;
import ch.sourcepond.io.distributor.impl.response.StatusResponseException;
import ch.sourcepond.io.distributor.impl.response.StatusResponseListenerFactory;
import ch.sourcepond.io.distributor.spi.TimeoutConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.ITopic;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.slf4j.LoggerFactory.getLogger;

public class LockManager {
    private static final Logger LOG = getLogger(LockManager.class);
    static final TimeUnit DEFAULT_LEASE_UNIT = MINUTES;
    static final long DEFAULT_LEASE_TIMEOUT = 15;
    private final ConcurrentMap<String, ILock> globalLocks = new ConcurrentHashMap<>();
    private final HazelcastInstance hci;
    private final TimeoutConfig timeoutConfig;
    private final ITopic<String> lockRequestTopic;
    private final ITopic<String> unlockRequestTopic;
    private final StatusResponseListenerFactory factory;

    public LockManager(final HazelcastInstance pHci,
                       final TimeoutConfig pTimeoutConfig,
                       final StatusResponseListenerFactory pFactory,
                       final ITopic<String> pLockRequestTopic,
                       final ITopic<String> pUnlockRequestTopic) {
        hci = pHci;
        timeoutConfig = pTimeoutConfig;
        factory = pFactory;
        lockRequestTopic = pLockRequestTopic;
        unlockRequestTopic = pUnlockRequestTopic;
    }

    /**
     * Acquires on all known cluster-nodes a {@link java.nio.channels.FileLock} for the path specified. This method
     * blocks until all nodes have responded to the request. If the path does not exist on a node, it will be created
     * and locked.
     *
     * @param pPath Path to be locked on all nodes, must not be {@code null}.
     * @throws StatusResponseException Thrown, if the lock acquisition failed on some node.
     * @throws LockException           Thrown, if the lock acquisition timed out for a node.
     */
    private void acquireGlobalFileLock(final String pPath) throws StatusResponseException, TimeoutException {
        // In this case, the path is also the request-message
        factory.create(pPath, lockRequestTopic).awaitResponse(pPath);
    }

    /**
     * Releases on all known cluster-nodes the file-locks for the path specified (see {@link java.nio.channels.FileLock#release()}). If
     * no locks exist, nothing happens.
     *
     * @param pPath Path to be released on all nodes, must not be {@code null}
     */
    private void releaseGlobalFileLock(final String pPath) throws StatusResponseException, TimeoutException {
        // In this case, the path is also the request-message
        factory.create(pPath, unlockRequestTopic).awaitResponse(pPath);
    }

    private void lockAcquisitionFailed(final String pPath, final String pMessage, final Exception pCause)
            throws LockException {
        try {
            throw new LockException(pMessage, pCause);
        } finally {
            try {
                releaseGlobalFileLock(pPath);
            } catch (final StatusResponseException | TimeoutException e) {
                LOG.warn(e.getMessage(), e);
            } finally {
                final ILock globalLock = globalLocks.remove(pPath);
                assert globalLock != null : "globalLock is null";
                globalLock.unlock();
            }
        }
    }

    public boolean isLocked(final String pPath) {
        return globalLocks.containsKey(pPath);
    }

    public void lock(final String pPath) throws LockException {
        final ILock globalLock = globalLocks.computeIfAbsent(pPath, p -> hci.getLock(p));

        try {
            // Because timeout-config is mutable we store the current values into
            // variables to make sure that in error case the actual values are included
            // into the exception message.
            final long lockTimeout = timeoutConfig.getLockTimeout();
            final TimeUnit lockTimeUnit = timeoutConfig.getLockTimeoutUnit();

            if (globalLock.tryLock(timeoutConfig.getLockTimeout(), timeoutConfig.getLockTimeoutUnit(), DEFAULT_LEASE_TIMEOUT, DEFAULT_LEASE_UNIT)) {
                acquireGlobalFileLock(pPath);
            } else {
                lockAcquisitionFailed(pPath, format("Lock acquisition timed out after %d %s", lockTimeout, lockTimeUnit), null);
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
            lockAcquisitionFailed(pPath, format("Lock acquisition interrupted for %s!", pPath), e);
        } catch (final StatusResponseException | TimeoutException e) {
            lockAcquisitionFailed(pPath, format("Lock acquisition failed for %s!", pPath), e);
        }
    }

    public void unlock(final String pPath) throws UnlockException {
        final ILock lock = globalLocks.remove(pPath);
        if (lock != null) {
            try {
                releaseGlobalFileLock(pPath);
            } catch (final StatusResponseException | TimeoutException e) {
                throw new UnlockException(format("Exception occurred while releasing file-lock for %s", pPath), e);
            } finally {
                lock.unlock();
            }
        } else {
            LOG.warn("No global lock registered for {}, nothing unlocked!", pPath);
        }
    }
}
