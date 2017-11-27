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
import ch.sourcepond.io.distributor.api.Distributor;
import ch.sourcepond.io.distributor.api.LockException;
import ch.sourcepond.io.distributor.api.TransferException;
import ch.sourcepond.io.distributor.api.StoreException;
import ch.sourcepond.io.distributor.api.UnlockException;
import ch.sourcepond.io.distributor.impl.common.MessageListenerRegistration;
import ch.sourcepond.io.distributor.impl.lock.LockManager;
import ch.sourcepond.io.distributor.impl.request.RequestDistributor;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import static java.util.Objects.requireNonNull;

final class HazelcastDistributor implements Distributor {
    static final byte[] EMPTY_CHECKSUM = new byte[0];
    private final HazelcastInstance hci;
    private final IMap<String, byte[]> checksums;
    private final LockManager lockManager;
    private final RequestDistributor requestDistributor;
    private final Set<MessageListenerRegistration> listenerRegistrations;

    @Inject
    HazelcastDistributor(final HazelcastInstance pHci,
                         final IMap<String, byte[]> pChecksums,
                         final LockManager pLockManager,
                         final RequestDistributor pRequestDistributor,
                         final Set<MessageListenerRegistration> pListenerRegistrations) {
        hci = pHci;
        checksums = pChecksums;
        lockManager = pLockManager;
        requestDistributor = pRequestDistributor;
        listenerRegistrations = pListenerRegistrations;
    }

    @Override
    public void lock(final String pPath) throws LockException {
        lockManager.lock(requireNonNull(pPath, "path is null"));
    }

    @Override
    public boolean isLocked(final String pPath) {
        return lockManager.isLocked(requireNonNull(pPath, "path is null"));
    }

    @Override
    public void unlock(final String pPath) throws UnlockException {
        lockManager.unlock(requireNonNull(pPath, "path is null"));
    }

    @Override
    public void delete(final String pPath) throws DeletionException {
        requestDistributor.delete(requireNonNull(pPath, "path is null"));
    }

    @Override
    public void transfer(final String pPath, final ByteBuffer pData) throws TransferException {
        requestDistributor.transfer(requireNonNull(pPath, "path is null"), requireNonNull(pData, "buffer is null"));
    }

    @Override
    public void store(final String pPath, final byte[] pChecksum, final IOException pFailureOrNull) throws StoreException {
        requireNonNull(pChecksum, "checksum is null");
        requestDistributor.store(requireNonNull(pPath, "path is null"), pFailureOrNull);

        // Do only update the checksum when the store operation was successful
        checksums.put(pPath, pChecksum);
    }

    @Override
    public String getLocalNode() {
        return hci.getLocalEndpoint().getUuid();
    }

    @Override
    public byte[] getChecksum(final String pPath) {
        final byte[] checksum = checksums.get(requireNonNull(pPath, "path is null"));
        return checksum == null ? EMPTY_CHECKSUM : checksum;
    }

    @Override
    public void close() {
        listenerRegistrations.forEach(r -> r.close());
    }
}
