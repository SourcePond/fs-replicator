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
package ch.sourcepond.io.fssync.distributor.hazelcast.lock;

import ch.sourcepond.io.fssync.distributor.hazelcast.exception.LockException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.UnlockException;
import ch.sourcepond.io.fssync.distributor.hazelcast.Config;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Lock;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Unlock;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ClusterResponseBarrierFactory;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ResponseException;
import com.hazelcast.core.ITopic;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

public class LockManager implements AutoCloseable {
    private static final Logger LOG = getLogger(LockManager.class);
    private final ClusterResponseBarrierFactory factory;
    private final Locks locks;
    private final ITopic<DistributionMessage> lockRequestTopic;
    private final ITopic<DistributionMessage> unlockRequestTopic;
    private final Config config;

    @Inject
    public LockManager(final ClusterResponseBarrierFactory pFactory,
                       final Locks pLocks,
                       final Config pConfig,
                       @Lock final ITopic<DistributionMessage> pLockRequestTopic,
                       @Unlock final ITopic<DistributionMessage> pUnlockRequestTopic) {
        factory = pFactory;
        locks = pLocks;
        lockRequestTopic = pLockRequestTopic;
        unlockRequestTopic = pUnlockRequestTopic;
        config = pConfig;
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
    private void acquireGlobalFileLock(final String pSyncDir, final String pPath) throws ResponseException, TimeoutException {
        // In this case, the path is also the request-message
        factory.create(pPath, lockRequestTopic).awaitResponse(new DistributionMessage(pSyncDir, pPath));
    }

    /**
     * Releases on all known cluster-nodes the file-locks for the path specified (see {@link java.nio.channels.FileLock#release()}). If
     * no locks exist, nothing happens.
     *
     * @param pPath Path to be released on all nodes, must not be {@code null}
     */
    private void releaseGlobalFileLock(final String pSyncDir, final String pPath) throws ResponseException, TimeoutException {
        // In this case, the path is also the request-message
        factory.create(pPath, unlockRequestTopic).awaitResponse(new DistributionMessage(pSyncDir, pPath));
    }

    private boolean lockAcquisitionFailed(final String pSyncDir, final String pPath, final String pMessage, final Exception pCause)
            throws LockException {
        try {
            throw new LockException(pMessage, pCause);
        } finally {
            try {
                releaseGlobalFileLock(pSyncDir, pPath);
            } catch (final ResponseException | TimeoutException e) {
                LOG.warn(e.getMessage(), e);
            } finally {
                locks.unlock(toGlobalPath(pSyncDir, pPath));
            }
        }
    }

    public String toGlobalPath(final String pSyncDir, final String pPath) {
        return format("%s:%s", requireNonNull(pSyncDir, "syncdir is null"),
                requireNonNull(pPath, "path is null"));
    }

    public boolean tryLock(final String pSyncDir, final String pPath) throws LockException {
        try {
            if (locks.tryLock(toGlobalPath(pSyncDir, pPath))) {
                acquireGlobalFileLock(pSyncDir, pPath);
                return true;
            } else {
                return lockAcquisitionFailed(pSyncDir, pPath, format("Lock acquisition timed out after %d %s",
                        config.lockTimeout(), config.lockTimeoutUnit()), null);
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
            return lockAcquisitionFailed(pSyncDir, pPath, format("Lock acquisition interrupted for %s!", pPath), e);
        } catch (final ResponseException | TimeoutException e) {
            return lockAcquisitionFailed(pSyncDir, pPath, format("Lock acquisition failed for %s!", pPath), e);
        }
    }

    public void unlock(final String pSyncDir, final String pPath) throws UnlockException {
        try {
            releaseGlobalFileLock(pSyncDir, pPath);
        } catch (final ResponseException | TimeoutException e) {
            throw new UnlockException(format("Exception occurred while releasing file-lock for %s", pPath), e);
        } finally {
            locks.unlock(toGlobalPath(pSyncDir, pPath));
        }
    }

    @Override
    public void close() {
        locks.close();
    }
}
