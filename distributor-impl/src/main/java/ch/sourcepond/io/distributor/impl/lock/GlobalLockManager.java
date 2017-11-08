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

import ch.sourcepond.io.distributor.api.GlobalLockException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.slf4j.LoggerFactory.getLogger;

public class GlobalLockManager {
    private static final Logger LOG = getLogger(GlobalLockManager.class);
    static final TimeUnit DEFAULT_LEASE_UNIT = MINUTES;
    static final long DEFAULT_LEASE_TIMEOUT = 15;
    private final ConcurrentMap<String, ILock> globalLocks = new ConcurrentHashMap<>();
    private final HazelcastInstance hci;
    private final MasterFileLockManager mflm;

    public GlobalLockManager(final HazelcastInstance pHci, final MasterFileLockManager pMflm) {
        hci = pHci;
        mflm = pMflm;
    }

    private void lockAcquisitionFailed(final String pPath, final String pMessage, final Exception pCause)
            throws GlobalLockException {
        try {
            throw new GlobalLockException(pMessage, pCause);
        } finally {
            try {
                mflm.releaseGlobalFileLock(pPath);
            } finally {
                final ILock globalLock = globalLocks.remove(pPath);
                assert globalLock != null : "globalLock is null";
                globalLock.unlock();
            }
        }
    }

    public void lockGlobally(final String pPath, final TimeUnit pTimeoutUnit, final long pTimeout)
            throws GlobalLockException {
        final ILock globalLock = globalLocks.computeIfAbsent(pPath, p -> hci.getLock(p));

        try {
            if (globalLock.tryLock(pTimeout, pTimeoutUnit, DEFAULT_LEASE_TIMEOUT, DEFAULT_LEASE_UNIT)) {
                mflm.acquireGlobalFileLock(pPath);
            } else {
                lockAcquisitionFailed(pPath, format("Lock acquisition timed out after %d %s", pTimeout, pTimeoutUnit), null);
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
            lockAcquisitionFailed(pPath, format("Lock acquisition interrupted for %s!", pPath), e);
        } catch (final FileLockException | TimeoutException e) {
            lockAcquisitionFailed(pPath, format("Lock acquisition failed for %s!", pPath), e);
        }
    }

    public void unlockGlobally(final String pPath) {
        final ILock lock = globalLocks.remove(pPath);
        if (lock != null) {
            try {
                mflm.releaseGlobalFileLock(pPath);
            } finally {
                lock.unlock();
            }
        } else {
            LOG.warn("No global lock registered for {}, nothing unlocked!", pPath);
        }
    }
}
