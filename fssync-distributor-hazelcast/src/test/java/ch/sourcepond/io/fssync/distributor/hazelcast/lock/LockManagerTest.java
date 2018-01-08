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

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.config.DistributorConfig;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.LockException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.UnlockException;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ClusterResponseBarrier;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ClusterResponseBarrierFactory;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ResponseException;
import com.hazelcast.core.ITopic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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
    private static final String EXPECTED_ABSOLUTE_PATH = "someExpectedAbsolutePath";
    private final SyncPath path = mock(SyncPath.class);
    private final Locks locks = mock(Locks.class);
    private final DistributorConfig config = mock(DistributorConfig.class);
    private final ClusterResponseBarrier<DistributionMessage> lockListener = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrier<DistributionMessage> unlockListener = mock(ClusterResponseBarrier.class);
    private final ClusterResponseBarrierFactory factory = mock(ClusterResponseBarrierFactory.class);
    private final ITopic<DistributionMessage> lockRequestTopic = mock(ITopic.class);
    private final ITopic<DistributionMessage> unlockRequestTopic = mock(ITopic.class);
    private final ArgumentMatcher<DistributionMessage> isEqualToExpectedDistributionMessage = msg -> path.equals(msg.getPath());
    private final LockManager manager = new LockManager(factory, locks, config, lockRequestTopic, unlockRequestTopic);

    @Before
    public void setup() throws Exception {
        when(path.toAbsolutePath()).thenReturn(EXPECTED_ABSOLUTE_PATH);
        when(config.lockTimeoutUnit()).thenReturn(EXPECTED_TIME_UNIT);
        when(config.lockTimeout()).thenReturn(EXPECTED_TIMEOUT);
        when(factory.create(path, lockRequestTopic)).thenReturn(lockListener);
        when(factory.create(path, unlockRequestTopic)).thenReturn(unlockListener);
        when(locks.tryLock(EXPECTED_ABSOLUTE_PATH)).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        interrupted();
    }

    @Test
    public void verifyUnlockWhenReleaseFileLockFails() throws Exception {
        when(locks.tryLock(EXPECTED_ABSOLUTE_PATH)).thenReturn(false);
        final ResponseException expected = new ResponseException("any");
        doThrow(expected).when(unlockListener).awaitResponse(argThat(isEqualToExpectedDistributionMessage));
        try {
            manager.tryLock(path);
            fail("Exception expected");
        } catch (final LockException e) {
            assertNull(e.getCause());
        }
        verify(locks).unlock(EXPECTED_ABSOLUTE_PATH);
    }

    private void verifyLockReleaseAfterFailure() throws Exception {
        verify(unlockListener).awaitResponse(argThat(isEqualToExpectedDistributionMessage));
        verify(locks).unlock(EXPECTED_ABSOLUTE_PATH);
    }

    @Test
    public void tryLockReturnedFalse() throws Exception {
        when(locks.tryLock(EXPECTED_ABSOLUTE_PATH)).thenReturn(false);
        try {
            manager.tryLock(path);
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
        doThrow(expected).when(locks).tryLock(EXPECTED_ABSOLUTE_PATH);
        try {
            manager.tryLock(path);
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
        doThrow(expected).when(lockListener).awaitResponse(argThat(isEqualToExpectedDistributionMessage));

        try {
            manager.tryLock(path);
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
        doThrow(expected).when(unlockListener).awaitResponse(argThat(isEqualToExpectedDistributionMessage));

        try {
            manager.unlock(path);
            fail("Exception expected");
        } catch (final UnlockException e) {
            assertSame(expected, e.getCause());
        }
        verifyLockReleaseAfterFailure();
        assertFalse(currentThread().isInterrupted());
    }

    @Test
    public void lockUnlock() throws Exception {
        manager.tryLock(path);
        manager.unlock(path);
        final InOrder order = inOrder(locks, lockListener, unlockListener);
        order.verify(locks).tryLock(EXPECTED_ABSOLUTE_PATH);
        order.verify(lockListener).awaitResponse(argThat(isEqualToExpectedDistributionMessage));
        order.verify(unlockListener).awaitResponse(argThat(isEqualToExpectedDistributionMessage));
        order.verify(locks).unlock(EXPECTED_ABSOLUTE_PATH);
    }

    @Test
    public void close() {
        manager.close();
        verify(locks).close();
    }
}
