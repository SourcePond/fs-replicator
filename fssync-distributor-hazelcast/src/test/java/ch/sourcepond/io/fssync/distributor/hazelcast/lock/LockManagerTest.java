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

import ch.sourcepond.io.fssync.distributor.api.LockException;
import ch.sourcepond.io.fssync.distributor.api.UnlockException;
import ch.sourcepond.io.fssync.distributor.hazelcast.Config;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ClusterResponseBarrier;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ClusterResponseBarrierFactory;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ResponseException;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.ITopic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_GLOBAL_PATH;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_PATH;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_SYNC_DIR;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE;
import static ch.sourcepond.io.fssync.distributor.hazelcast.lock.LockManager.DEFAULT_LEASE_TIMEOUT;
import static ch.sourcepond.io.fssync.distributor.hazelcast.lock.LockManager.DEFAULT_LEASE_UNIT;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LockManagerTest {
    private static final TimeUnit EXPECTED_TIME_UNIT = MILLISECONDS;
    private static final long EXPECTED_TIMEOUT = 500;
    private final HazelcastInstance hci = mock(HazelcastInstance.class);
    private final ILock lock = mock(ILock.class);
    private final Config config = mock(Config.class);
    private final ClusterResponseBarrier<DistributionMessage> lockListener = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrier<DistributionMessage> unlockListener = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrierFactory factory = mock(ClusterResponseBarrierFactory.class);
    private final ITopic<DistributionMessage> lockRequestTopic = mock(ITopic.class);
    private final ITopic<DistributionMessage> unlockRequestTopic = mock(ITopic.class);
    private final LockManager manager = new LockManager(factory, hci, config, lockRequestTopic, unlockRequestTopic);

    @Before
    public void setup() throws Exception {
        when(config.lockTimeoutUnit()).thenReturn(EXPECTED_TIME_UNIT);
        when(config.lockTimeout()).thenReturn(EXPECTED_TIMEOUT);
        when(hci.getLock(EXPECTED_GLOBAL_PATH)).thenReturn(lock);
        when(factory.create(EXPECTED_PATH, lockRequestTopic)).thenReturn(lockListener);
        when(factory.create(EXPECTED_PATH, unlockRequestTopic)).thenReturn(unlockListener);
        setup(lock);
    }

    @After
    public void tearDown() throws Exception {
        interrupted();
    }

    private ILock setup(final ILock pLock) throws Exception {
        when(pLock.tryLock(EXPECTED_TIMEOUT, EXPECTED_TIME_UNIT, DEFAULT_LEASE_TIMEOUT, DEFAULT_LEASE_UNIT)).thenReturn(true);
        return pLock;
    }

    @Test(expected = NullPointerException.class)
    public void toGlobalPathSyncDirIsNull() {
        manager.toGlobalPath(null, EXPECTED_PATH);
    }

    @Test(expected = NullPointerException.class)
    public void toGlobalPathPathIsNull() {
        manager.toGlobalPath(EXPECTED_SYNC_DIR, null);
    }

    @Test
    public void toGlobalPath() {
        assertEquals("someDir:somePath", manager.toGlobalPath(EXPECTED_SYNC_DIR, EXPECTED_PATH));
    }

    @Test
    public void verifyUnlockWhenReleaseFileLockFails() throws Exception {
        when(lock.tryLock(EXPECTED_TIMEOUT, EXPECTED_TIME_UNIT, DEFAULT_LEASE_TIMEOUT, DEFAULT_LEASE_UNIT)).thenReturn(false);
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(unlockListener).awaitResponse(argThat(IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE));
        try {
            manager.lock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
            fail("Exception expected");
        } catch (final LockException e) {
            assertNull(e.getCause());
        }
        verify(lock).unlock();
    }

    private void verifyLockReleaseAfterFailure() throws Exception {
        verify(unlockListener).awaitResponse(argThat(IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE));
        verify(lock).unlock();
    }

    @Test
    public void tryLockReturnedFalse() throws Exception {
        when(lock.tryLock(EXPECTED_TIMEOUT, EXPECTED_TIME_UNIT, DEFAULT_LEASE_TIMEOUT, DEFAULT_LEASE_UNIT)).thenReturn(false);
        try {
            manager.lock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
            fail("Exception expected");
        } catch (final LockException e) {
            assertNull(e.getCause());
        }
        verifyLockReleaseAfterFailure();
        assertFalse(currentThread().isInterrupted());
    }

    @Test
    public void tryLockInterrupted() throws Exception {
        final InterruptedException expected = new InterruptedException();
        doThrow(expected).when(lock).tryLock(EXPECTED_TIMEOUT, EXPECTED_TIME_UNIT, DEFAULT_LEASE_TIMEOUT, DEFAULT_LEASE_UNIT);
        try {
            manager.lock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
            fail("Exception expected");
        } catch (final LockException e) {
            assertSame(expected, e.getCause());
        }
        verifyLockReleaseAfterFailure();
        assertTrue(currentThread().isInterrupted());
    }

    @Test
    public void acquireGlobalFileLockFailed() throws Exception {
        final TimeoutException expected = new TimeoutException();
        doThrow(expected).when(lockListener).awaitResponse(argThat(IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE));

        try {
            manager.lock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
            fail("Exception expected");
        } catch (final LockException e) {
            assertSame(expected, e.getCause());
        }
        verifyLockReleaseAfterFailure();
        assertFalse(currentThread().isInterrupted());
    }

    @Test
    public void releaseGlobalFileLockFailed() throws Exception {
        final TimeoutException expected = new TimeoutException();
        doThrow(expected).when(unlockListener).awaitResponse(argThat(IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE));

        try {
            manager.unlock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
            fail("Exception expected");
        } catch (final UnlockException e) {
            assertSame(expected, e.getCause());
        }
        verifyLockReleaseAfterFailure();
        assertFalse(currentThread().isInterrupted());
    }

    @Test
    public void verifyIsLocked() throws Exception {
        when(lock.isLocked()).thenReturn(true).thenReturn(false);
        assertTrue(manager.isLocked(EXPECTED_SYNC_DIR, EXPECTED_PATH));
        assertFalse(manager.isLocked(EXPECTED_SYNC_DIR, EXPECTED_PATH));
    }

    @Test
    public void lockUnlock() throws Exception {
        manager.lock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        manager.unlock(EXPECTED_SYNC_DIR, EXPECTED_PATH);
        final InOrder order = inOrder(lock, lockListener, unlockListener);
        order.verify(lock).tryLock(EXPECTED_TIMEOUT, EXPECTED_TIME_UNIT, DEFAULT_LEASE_TIMEOUT, DEFAULT_LEASE_UNIT);
        order.verify(lockListener).awaitResponse(argThat(IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE));
        order.verify(unlockListener).awaitResponse(argThat(IS_EQUAL_TO_EXPECTED_DISTRIBUTION_MESSAGE));
        order.verify(lock).unlock();
    }
}
