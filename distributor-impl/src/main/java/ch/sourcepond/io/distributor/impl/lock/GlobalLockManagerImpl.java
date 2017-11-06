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
import static org.slf4j.LoggerFactory.getLogger;

final class GlobalLockManagerImpl implements GlobalLockManager {
    private static final Logger LOG = getLogger(GlobalLockManagerImpl.class);
    private final ConcurrentMap<String, ILock> globalLocks = new ConcurrentHashMap<>();
    private final HazelcastInstance hci;
    private final MasterFileLockManager mflm;

    public GlobalLockManagerImpl(final HazelcastInstance pHci, final MasterFileLockManager pMflm) {
        hci = pHci;
        mflm = pMflm;
    }

    private void lockAcquisitionFailed(final String pPath, final String pMessage, final Exception pCause)
            throws GlobalLockException {
        try {
            throw new GlobalLockException(pMessage, pCause);
        } finally {
            final ILock globalLock = globalLocks.remove(pPath);
            assert globalLock != null : "globalLock is null";
            globalLock.unlock();
        }
    }

    @Override
    public void lockGlobally(final String pPath, final TimeUnit pTimeoutUnit, final long pTimeout)
            throws GlobalLockException {
        final ILock globalLock = globalLocks.computeIfAbsent(pPath, p -> hci.getLock(p));

        try {
            if (globalLock.tryLock(pTimeout, pTimeoutUnit, 10, TimeUnit.MINUTES)) {
                mflm.acquireGlobalFileLock(pPath);
            } else {
                lockAcquisitionFailed(pPath, format("Lock acquisition timed out after %d %s", pTimeout, pTimeoutUnit), null);
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
            lockAcquisitionFailed(pPath, format("Lock acquisition interrupted for %s!", pPath), e);
        } catch (final TimeoutException e) {
            lockAcquisitionFailed(pPath, format("Lock acquisition timed out for %s!", pPath), e);
        } catch (final FileLockException e) {
            lockAcquisitionFailed(pPath, format("File lock acquisition failed for %s!", pPath), e);
        }
    }

    @Override
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
