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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static ch.sourcepond.io.distributor.impl.lock.GlobalLockManagerImpl.DEFAULT_LEASE_TIMEOUT;
import static ch.sourcepond.io.distributor.impl.lock.GlobalLockManagerImpl.DEFAULT_LEASE_UNIT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GlobalLockManagerImplTest {
    private static final String EXPECTED_PATH = "somePath";
    private static final TimeUnit EXPECTED_TIME_UNIT = MILLISECONDS;
    private static final long EXPECTED_TIMEOUT = 500;
    private final HazelcastInstance hci = mock(HazelcastInstance.class);
    private final ILock lock = mock(ILock.class);
    private final MasterFileLockManager mflm = mock(MasterFileLockManager.class);
    private final GlobalLockManagerImpl manager = new GlobalLockManagerImpl(hci, mflm);

    @Before
    public void setup() throws Exception {
        when(hci.getLock(EXPECTED_PATH)).thenReturn(lock);
        setup(lock);
    }

    private ILock setup(final ILock pLock) throws  Exception {
        when(pLock.tryLock(EXPECTED_TIMEOUT, EXPECTED_TIME_UNIT, DEFAULT_LEASE_TIMEOUT, DEFAULT_LEASE_UNIT)).thenReturn(true);
        return pLock;
    }

    @Test
    public void keepSameLockInstance() throws Exception {
        when(hci.getLock(EXPECTED_PATH)).thenReturn(lock).thenThrow(AssertionError.class);
        manager.lockGlobally(EXPECTED_PATH, EXPECTED_TIME_UNIT, EXPECTED_TIMEOUT);
        manager.lockGlobally(EXPECTED_PATH, EXPECTED_TIME_UNIT, EXPECTED_TIMEOUT);

        // Should have been called exactly once
        verify(hci).getLock(EXPECTED_PATH);
    }

    @Test
    public void freshLockAfterUnlock() throws Exception {
        final ILock l1 = setup(mock(ILock.class));
        final ILock l2 = setup(mock(ILock.class));

        when(hci.getLock(EXPECTED_PATH)).thenReturn(l1).thenReturn(l2);
        manager.lockGlobally(EXPECTED_PATH, EXPECTED_TIME_UNIT, EXPECTED_TIMEOUT);
        manager.unlockGlobally(EXPECTED_PATH);
        verify(l1).unlock();
        manager.lockGlobally(EXPECTED_PATH, EXPECTED_TIME_UNIT, EXPECTED_TIMEOUT);
        manager.unlockGlobally(EXPECTED_PATH);
        verify(l2).unlock();
    }

    @Test
    public void lockGlobally() throws Exception {

    }
}
