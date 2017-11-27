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
package ch.sourcepond.io.fssync.distributor.impl.lock;

import ch.sourcepond.io.fssync.distributor.api.LockException;
import ch.sourcepond.io.fssync.distributor.api.UnlockException;
import ch.sourcepond.io.fssync.distributor.impl.annotations.Lock;
import ch.sourcepond.io.fssync.distributor.impl.annotations.Unlock;
import ch.sourcepond.io.fssync.distributor.impl.binding.TimeoutConfig;
import ch.sourcepond.io.fssync.distributor.impl.response.ClusterResponseBarrierFactory;
import ch.sourcepond.io.fssync.distributor.impl.response.ResponseException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.ITopic;
import org.slf4j.Logger;

import javax.inject.Inject;
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
    private final ClusterResponseBarrierFactory factory;
    private final HazelcastInstance hci;
    private final ITopic<String> lockRequestTopic;
    private final ITopic<String> unlockRequestTopic;
    private final TimeoutConfig lockTimeoutConfig;

    @Inject
    public LockManager(final ClusterResponseBarrierFactory pFactory,
                       final HazelcastInstance pHci,
                       @Lock final ITopic<String> pLockRequestTopic,
                       @Unlock final ITopic<String> pUnlockRequestTopic,
                       @Lock final TimeoutConfig pLockTimeoutConfig) {
        factory = pFactory;
        hci = pHci;
        lockRequestTopic = pLockRequestTopic;
        unlockRequestTopic = pUnlockRequestTopic;
        lockTimeoutConfig = pLockTimeoutConfig;
    }

    /**
     * Acquires on all known cluster-nodes a {@link java.nio.channels.FileLock} for the path specified. This method
     * blocks until all nodes have responded to the request. If the path does not exist on a node, it will be created
     * and locked.
     *
     * @param pPath Path to be locked on all nodes, must not be {@code null}.
     * @throws ResponseException Thrown, if the lock acquisition failed on some node.
     * @throws LockException     Thrown, if the lock acquisition timed out for a node.
     */
    private void acquireGlobalFileLock(final String pPath) throws ResponseException, TimeoutException {
        // In this case, the path is also the request-message
        factory.create(pPath, lockRequestTopic).awaitResponse(pPath);
    }

    /**
     * Releases on all known cluster-nodes the file-locks for the path specified (see {@link java.nio.channels.FileLock#release()}). If
     * no locks exist, nothing happens.
     *
     * @param pPath Path to be released on all nodes, must not be {@code null}
     */
    private void releaseGlobalFileLock(final String pPath) throws ResponseException, TimeoutException {
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
            } catch (final ResponseException | TimeoutException e) {
                LOG.warn(e.getMessage(), e);
            } finally {
                hci.getLock(pPath).unlock();
            }
        }
    }

    public boolean isLocked(final String pPath) {
        return hci.getLock(pPath).isLocked();
    }

    public void lock(final String pPath) throws LockException {
        final ILock globalLock = hci.getLock(pPath);
        try {
            if (globalLock.tryLock(lockTimeoutConfig.getTimeout(), lockTimeoutConfig.getUnit(), DEFAULT_LEASE_TIMEOUT, DEFAULT_LEASE_UNIT)) {
                acquireGlobalFileLock(pPath);
            } else {
                lockAcquisitionFailed(pPath, format("Lock acquisition timed out after %d %s",
                        lockTimeoutConfig.getTimeout(), lockTimeoutConfig.getUnit()), null);
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
            lockAcquisitionFailed(pPath, format("Lock acquisition interrupted for %s!", pPath), e);
        } catch (final ResponseException | TimeoutException e) {
            lockAcquisitionFailed(pPath, format("Lock acquisition failed for %s!", pPath), e);
        }
    }

    public void unlock(final String pPath) throws UnlockException {
        final ILock lock = hci.getLock(pPath);
        try {
            releaseGlobalFileLock(pPath);
        } catch (final ResponseException | TimeoutException e) {
            throw new UnlockException(format("Exception occurred while releasing file-lock for %s", pPath), e);
        } finally {
            lock.unlock();
        }
    }
}
