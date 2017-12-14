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

import ch.sourcepond.io.fssync.compound.Configurable;
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
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

import static java.util.Objects.requireNonNull;

final class HazelcastDistributor extends Configurable<Config> implements Distributor {
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

    public String getHazelcastInstanceName() {
        return hci.getName();
    }

    @Override
    public boolean tryLock(final String pSyncDir, final String pPath) throws LockException {
        return lockManager.tryLock(requireNonNull(pSyncDir, "syncdir is null"),
                requireNonNull(pPath, "path is null"));
    }

    @Override
    public void unlock(final String pSyncDir, final String pPath) throws UnlockException {
        lockManager.unlock(requireNonNull(pSyncDir, "syncdir is null"),
                requireNonNull(pPath, "path is null"));
    }

    @Override
    public void delete(final String pSyncDir, final String pPath) throws DeletionException {
        requestDistributor.delete(requireNonNull(pSyncDir, "syncdir is null"),
                requireNonNull(pPath, "path is null"));
    }

    @Override
    public void transfer(final String pSyncDir, final String pPath, final ByteBuffer pData) throws TransferException {
        requestDistributor.transfer(requireNonNull(pSyncDir, "syncdir is null"),
                requireNonNull(pPath, "path is null"), requireNonNull(pData, "buffer is null"));
    }

    @Override
    public void discard(final String pSyncDir, final String pPath, final IOException pFailure) throws DiscardException {
        requestDistributor.discard(requireNonNull(pSyncDir, "syncdir is null"),
                requireNonNull(pPath, "path is null"), requireNonNull(pFailure, "failure is null"));
    }

    @Override
    public void store(final String pSyncDir, final String pPath, final byte[] pChecksum) throws StoreException {
        requireNonNull(pSyncDir, "syncdir is null");
        requireNonNull(pChecksum, "checksum is null");
        requestDistributor.store(requireNonNull(pSyncDir, "syncdir is null"),
                requireNonNull(pPath, "path is null"));

        // Do only update the checksum when the store operation was successful
        checksums.put(lockManager.toGlobalPath(pSyncDir, pPath), pChecksum);
    }

    @Override
    public byte[] getChecksum(final String pSyncDir, final String pPath) {
        final byte[] checksum = checksums.get(lockManager.toGlobalPath(pSyncDir, pPath));
        return checksum == null ? EMPTY_CHECKSUM : checksum;
    }

    @Override
    public void close() {
        super.close();
        listenerRegistrations.forEach(r -> r.close());
        lockManager.close();
    }
}
