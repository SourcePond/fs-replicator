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
import ch.sourcepond.io.fssync.distributor.hazelcast.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_LEASE_TIME;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_LEASE_TIME_UNIT;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_LOCK_TIMEOUT;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_LOCK_TIMEOUT_UNIT;
import static java.lang.Thread.interrupted;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LocksTest {
    private static final String EXPECTED_KEY = "expectedKey";
    private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
    private final HazelcastInstance hci = mock(HazelcastInstance.class);
    private final ILock globalLock = mock(ILock.class);
    private final Config config = mock(Config.class);
    private final CountDownLatch latch = new CountDownLatch(1);
    private final Locks locks = new Locks(hci, config);
    private volatile Exception expectedException;

    @Before
    public void setup() throws Exception {
        when(config.lockTimeout()).thenReturn(EXPECTED_LOCK_TIMEOUT);
        when(config.lockTimeoutUnit()).thenReturn(EXPECTED_LOCK_TIMEOUT_UNIT);
        when(config.leaseTime()).thenReturn(EXPECTED_LEASE_TIME);
        when(config.leaseTimeUnit()).thenReturn(EXPECTED_LEASE_TIME_UNIT);
        when(hci.getLock(EXPECTED_KEY)).thenReturn(globalLock);
        when(globalLock.tryLock(EXPECTED_LOCK_TIMEOUT, EXPECTED_LOCK_TIMEOUT_UNIT, EXPECTED_LEASE_TIME, EXPECTED_LEASE_TIME_UNIT)).thenReturn(true);
    }

    @After
    public void tearDown() {
        interrupted();
        executor.shutdown();
    }

    @Test
    public void unlockNoSuchLock() {
        locks.unlock("unknown");
        verifyZeroInteractions(globalLock);
    }

    @Test
    public void tryLock() throws Exception {
        assertTrue(locks.tryLock(EXPECTED_KEY));
    }

    @Test
    public void tryLockNotSuccessful() throws Exception {
        when(globalLock.tryLock(EXPECTED_LOCK_TIMEOUT, EXPECTED_LOCK_TIMEOUT_UNIT, EXPECTED_LEASE_TIME, EXPECTED_LEASE_TIME_UNIT)).thenReturn(false);
        assertFalse(locks.tryLock(EXPECTED_KEY));
    }

    @Test(timeout = 1000)
    public void tryLockDuringShutdown() throws Exception {
        executor.schedule(() -> {
            try {
                locks.tryLock(EXPECTED_KEY);
            } catch (Exception e) {
                expectedException = e;
            } finally {
                locks.unlock(EXPECTED_KEY);
            }
        }, 500, MILLISECONDS);
        locks.tryLock(EXPECTED_KEY);
        locks.close();
        assertNotNull(expectedException);
        assertEquals(LockException.class, expectedException.getClass());
    }

    @Test(timeout = 1000)
    public void closeInterrupted() throws Exception {
        when(config.leaseTimeUnit()).thenReturn(MILLISECONDS);
        when(config.leaseTime()).thenReturn(1000L);
        when(globalLock.tryLock(EXPECTED_LOCK_TIMEOUT, EXPECTED_LOCK_TIMEOUT_UNIT, 1000, MILLISECONDS)).thenReturn(true);
        final Thread main = Thread.currentThread();
        executor.schedule(() -> main.interrupt(), 200, MILLISECONDS);
        locks.tryLock(EXPECTED_KEY);
        locks.close();
        assertTrue(main.isInterrupted());
    }

    @Test(timeout = 1000)
    public void forceCloseAfterTimeout() throws Exception {
        when(config.leaseTimeUnit()).thenReturn(MILLISECONDS);
        when(config.leaseTime()).thenReturn(500L);
        when(globalLock.tryLock(EXPECTED_LOCK_TIMEOUT, EXPECTED_LOCK_TIMEOUT_UNIT, 1000, MILLISECONDS)).thenReturn(true);
        locks.tryLock(EXPECTED_KEY);
        locks.close();
    }
}
