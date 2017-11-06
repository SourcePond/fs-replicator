package ch.sourcepond.io.distributor.impl;

import ch.sourcepond.io.distributor.api.GlobalLockException;
import ch.sourcepond.io.distributor.impl.lock.LockMessage;
import ch.sourcepond.io.distributor.spi.Receiver;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.ITopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;

public class LockManager {
    private static final Logger LOG = LoggerFactory.getLogger(LockManager.class);
    private final ConcurrentMap<String, DistributionLock> locks = new ConcurrentHashMap<>();
    private final HazelcastInstance hci;
    private final ITopic<LockMessage> lockResponseTopic;
    private final ITopic<String> acquireLockTopic;
    private final Receiver receiver;

    public LockManager(final HazelcastInstance pHci,
                       final ITopic<String> pAcquireLockTopic,
                       final ITopic<LockMessage> pLockAcquiredTopic,
                       final Receiver pReceiver) {
        hci = pHci;
        acquireLockTopic = pAcquireLockTopic;
        lockResponseTopic = pLockAcquiredTopic;
        receiver = pReceiver;
    }

    void lockGlobally(final String pPath, final TimeUnit pTimeoutUnit, final long pTimeout) throws GlobalLockException {
        final ILock lock = hci.getLock(pPath);
        try {
            // First of all, lock the following code section cluster-wide for the path specified. Note: the lock
            // will NOT be released when this method finishes under normal conditions. To do so, unlockGlobally must
            // be called
            if (lock.tryLock(pTimeout, pTimeoutUnit)) {

                // Secondly, acquire a file-lock for the path on every active cluster member
                final Cluster cluster = hci.getCluster();
                final DistributionLock distributionLock = new DistributionLock(lock, lockResponseTopic, cluster.getMembers());
                final String lockResponseListenerRegistrationId = lockResponseTopic.addMessageListener(distributionLock);
                final String membershipRegistrationId = cluster.addMembershipListener(distributionLock);

                try {
                    acquireLockTopic.publish(pPath);
                    distributionLock.awaitLocalLocks();
                } catch (final InterruptedException e) {
                    lock.unlock();
                    currentThread().interrupt();
                    throw new GlobalLockException(format("Locking %s cluster-wide failed!", pPath), e);
                } catch (final TimeoutException e) {
                    lock.unlock();
                    throw new GlobalLockException(format("Locking %s cluster-wide failed!", pPath), e);
                } finally {
                    lockResponseTopic.removeMessageListener(lockResponseListenerRegistrationId);
                    cluster.removeMembershipListener(membershipRegistrationId);
                }
            } else {
                throw new GlobalLockException(format("Locking %s cluster-wide failed!", pPath));
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
            throw new GlobalLockException(format("Locking %s cluster-wide failed!", pPath), e);
        }
    }

    void unlockGlobally(final String pPath) {
        final DistributionLock lock = locks.get(pPath);
        if (lock != null) {
            lock.
        } else {
            LOG.warn("No lock registered for {}, nothing unlocked", pPath);
        }
    }
}
