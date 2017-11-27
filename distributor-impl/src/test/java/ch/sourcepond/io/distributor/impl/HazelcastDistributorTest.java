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
package ch.sourcepond.io.distributor.impl;

import ch.sourcepond.io.distributor.api.DeletionException;
import ch.sourcepond.io.distributor.api.LockException;
import ch.sourcepond.io.distributor.api.StoreException;
import ch.sourcepond.io.distributor.api.TransferException;
import ch.sourcepond.io.distributor.api.UnlockException;
import ch.sourcepond.io.distributor.impl.common.MessageListenerRegistration;
import ch.sourcepond.io.distributor.impl.lock.LockManager;
import ch.sourcepond.io.distributor.impl.request.RequestDistributor;
import com.hazelcast.core.Endpoint;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import static ch.sourcepond.io.distributor.impl.HazelcastDistributor.EMPTY_CHECKSUM;
import static java.nio.ByteBuffer.wrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class HazelcastDistributorTest {
    private static final String EXPECTED_ENDPOINT_UUID = "someUuid";
    private static final String EXPECTED_PATH = "somePath";
    private static final byte[] EXPECTED_DATA = new byte[0];
    private static final byte[] EXPECTED_CHECKSUM = new byte[0];
    private static final IOException EXPECTED_FAILURE = new IOException();
    private final HazelcastInstance hci = mock(HazelcastInstance.class);
    private final IMap<String, byte[]> checksums = mock(IMap.class);
    private final LockManager lockManager = mock(LockManager.class);
    private final RequestDistributor requestDistributor = mock(RequestDistributor.class);
    private final MessageListenerRegistration registration = mock(MessageListenerRegistration.class);
    private final Set<MessageListenerRegistration> registrations = new HashSet<>();
    private final Endpoint endpoint = mock(Endpoint.class);
    private final HazelcastDistributor distributor = new HazelcastDistributor(hci, checksums, lockManager, requestDistributor, registrations);

    @Before
    public void setup() {
        when(hci.getLocalEndpoint()).thenReturn(endpoint);
        when(endpoint.getUuid()).thenReturn(EXPECTED_ENDPOINT_UUID);
        registrations.add(registration);
    }

    @Test(expected = NullPointerException.class)
    public void lockPathIsNull() throws LockException {
        distributor.lock(null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void lock() throws LockException {
        distributor.lock(EXPECTED_PATH);
        verify(lockManager).lock(EXPECTED_PATH);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void isLockPathIsNull() {
        distributor.isLocked(null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void isLocked() throws LockException {
        when(distributor.isLocked(EXPECTED_PATH)).thenReturn(true);
        assertTrue(distributor.isLocked(EXPECTED_PATH));
        verify(lockManager).isLocked(EXPECTED_PATH);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void unlockPathIsNull() throws UnlockException {
        distributor.unlock(null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void unlock() throws UnlockException {
        distributor.unlock(EXPECTED_PATH);
        verify(lockManager).unlock(EXPECTED_PATH);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void deletePathIsNull() throws DeletionException {
        distributor.delete(null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void delete() throws DeletionException {
        distributor.delete(EXPECTED_PATH);
        verify(requestDistributor).delete(EXPECTED_PATH);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void transferPathIsNull() throws TransferException {
        distributor.transfer(null, wrap(EXPECTED_DATA));
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void transferBufferIsNull() throws TransferException {
        distributor.transfer(EXPECTED_PATH, null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void transfer() throws TransferException {
        final ByteBuffer buffer = wrap(EXPECTED_DATA);
        distributor.transfer(EXPECTED_PATH, buffer);
        verify(requestDistributor).transfer(EXPECTED_PATH, buffer);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void storePathIsNull() throws StoreException {
        distributor.store(null, EXPECTED_CHECKSUM, EXPECTED_FAILURE);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test(expected = NullPointerException.class)
    public void storeChecksumIsNull() throws StoreException {
        distributor.store(EXPECTED_PATH, null, EXPECTED_FAILURE);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void storeChecksumFailureNull() throws StoreException {
        distributor.store(EXPECTED_PATH, EXPECTED_CHECKSUM, null);
        verify(requestDistributor).store(EXPECTED_PATH, null);
        verify(checksums).put(EXPECTED_PATH, EXPECTED_CHECKSUM);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void storeChecksum() throws StoreException {
        distributor.store(EXPECTED_PATH, EXPECTED_CHECKSUM, EXPECTED_FAILURE);
        verify(requestDistributor).store(EXPECTED_PATH, EXPECTED_FAILURE);
        verify(checksums).put(EXPECTED_PATH, EXPECTED_CHECKSUM);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void getLocalNode() {
        assertEquals(EXPECTED_ENDPOINT_UUID, distributor.getLocalNode());
    }

    @Test(expected = NullPointerException.class)
    public void getChecksumPathIsNull() {
        distributor.getChecksum(null);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void getChecksumNoDefinedYet() {
        assertSame(EMPTY_CHECKSUM, distributor.getChecksum(EXPECTED_PATH));
        verify(checksums).get(EXPECTED_PATH);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void getChecksum() {
        when(checksums.get(EXPECTED_PATH)).thenReturn(EXPECTED_CHECKSUM);
        assertSame(EXPECTED_CHECKSUM, distributor.getChecksum(EXPECTED_PATH));
        verify(checksums).get(EXPECTED_PATH);
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }

    @Test
    public void close() {
        distributor.close();
        verify(registration).close();
        verifyNoMoreInteractions(lockManager, hci, checksums, requestDistributor, requestDistributor, registration);
    }
}
