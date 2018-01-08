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
package ch.sourcepond.io.fssync.distributor.hazelcast;

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.MessageListenerRegistration;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.DeletionException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.DiscardException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.LockException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.StoreException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.TransferException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.UnlockException;
import ch.sourcepond.io.fssync.distributor.hazelcast.lock.LockManager;
import ch.sourcepond.io.fssync.distributor.hazelcast.request.RequestDistributor;
import com.hazelcast.core.Endpoint;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_DATA;
import static ch.sourcepond.io.fssync.distributor.hazelcast.HazelcastDistributor.EMPTY_CHECKSUM;
import static java.nio.ByteBuffer.wrap;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class HazelcastDistributorTest {
    private static final String EXPECTED_ENDPOINT_UUID = "someUuid";
    private static final byte[] EXPECTED_CHECKSUM = new byte[0];
    private static final IOException EXPECTED_FAILURE = new IOException();
    private final HazelcastInstance hci = mock(HazelcastInstance.class);
    private final IMap<SyncPath, byte[]> checksums = mock(IMap.class);
    private final LockManager lockManager = mock(LockManager.class);
    private final RequestDistributor requestDistributor = mock(RequestDistributor.class);
    private final MessageListenerRegistration registration = mock(MessageListenerRegistration.class);
    private final Set<MessageListenerRegistration> registrations = new HashSet<>();
    private final Endpoint endpoint = mock(Endpoint.class);
    private final SyncPath path = mock(SyncPath.class);
    private final ServiceRegistration<Distributor> serviceRegistration = mock(ServiceRegistration.class);
    private final HazelcastDistributor distributor = new HazelcastDistributor(checksums, lockManager, requestDistributor, registrations);

    @Before
    public void setup() {
        when(hci.getLocalEndpoint()).thenReturn(endpoint);
        when(endpoint.getUuid()).thenReturn(EXPECTED_ENDPOINT_UUID);
        when(checksums.get(path)).thenReturn(EXPECTED_CHECKSUM);
        registrations.add(registration);
        distributor.setRegistration(serviceRegistration);
    }

    @Test(expected = NullPointerException.class)
    public void tryLockPathIsNull() throws LockException {
        distributor.tryLock(null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void tryLock() throws LockException {
        when(lockManager.tryLock(path)).thenReturn(true);
        assertTrue(distributor.tryLock(path));
        verify(lockManager).tryLock(path);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void unlockPathIsNull() throws UnlockException {
        distributor.unlock(null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void unlock() throws UnlockException {
        distributor.unlock(path);
        verify(lockManager).unlock(path);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void deletePathIsNull() throws DeletionException {
        distributor.delete(null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void delete() throws DeletionException {
        distributor.delete(path);
        verify(requestDistributor).delete(path);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void transferPathIsNull() throws TransferException {
        distributor.transfer(null, wrap(EXPECTED_DATA));
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void transferBufferIsNull() throws TransferException {
        distributor.transfer(path, null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void transfer() throws TransferException {
        final ByteBuffer buffer = wrap(EXPECTED_DATA);
        distributor.transfer(path, buffer);
        verify(requestDistributor).transfer(path, buffer);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void discardPathIsNull() throws DiscardException {
        distributor.discard(null, EXPECTED_FAILURE);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void discardFailureIsNull() throws DiscardException {
        distributor.discard(path, null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void discard() throws StoreException {
        distributor.store(path, EXPECTED_CHECKSUM);
        verify(requestDistributor).store(path);
        verify(checksums).put(path, EXPECTED_CHECKSUM);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void storePathIsNull() throws StoreException {
        distributor.store(null, EXPECTED_CHECKSUM);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void storeChecksumIsNull() throws StoreException {
        distributor.store(path, null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void store() throws StoreException {
        distributor.store(path, EXPECTED_CHECKSUM);
        verify(requestDistributor).store(path);
        verify(checksums).put(path, EXPECTED_CHECKSUM);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void getChecksumNoDefinedYet() {
        when(checksums.get(path)).thenReturn(null);
        assertSame(EMPTY_CHECKSUM, distributor.getChecksum(path));
        verify(checksums).get(path);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void getChecksum() {
        assertSame(EXPECTED_CHECKSUM, distributor.getChecksum(path));
        verify(checksums).get(path);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void close() {
        distributor.close();
        verify(registration).close();
        verify(lockManager).close();
        verify(serviceRegistration).unregister();
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }
}