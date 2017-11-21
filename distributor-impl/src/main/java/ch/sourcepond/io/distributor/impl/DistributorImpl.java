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

import ch.sourcepond.io.distributor.api.Distributor;
import ch.sourcepond.io.distributor.api.exception.DeletionException;
import ch.sourcepond.io.distributor.api.exception.LockException;
import ch.sourcepond.io.distributor.api.exception.ModificationException;
import ch.sourcepond.io.distributor.api.exception.PathNotLockedException;
import ch.sourcepond.io.distributor.api.exception.StoreException;
import ch.sourcepond.io.distributor.api.exception.UnlockException;
import ch.sourcepond.io.distributor.impl.dataflow.DataflowManager;
import ch.sourcepond.io.distributor.impl.lock.LockManager;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.lang.String.format;

public class DistributorImpl implements Distributor {

    @FunctionalInterface
    private interface WithinLockExecutable<T, E extends Exception> {

        T execute(String pPath) throws E;
    }

    private static final byte[] EMPTY_CHECKSUM = new byte[0];
    private final HazelcastInstance hci;
    private final IMap<String, byte[]> checksums;
    private final LockManager lockManager;
    private final DataflowManager dataflowManager;

    public DistributorImpl(final HazelcastInstance pHci,
                           final IMap<String, byte[]> pChecksum,
                           final LockManager pLockManager,
                           final DataflowManager pDataflowManager) {
        hci = pHci;
        checksums = pChecksum;
        lockManager = pLockManager;
        dataflowManager = pDataflowManager;
    }

    @Override
    public void lock(final String pPath) throws LockException {
        lockManager.lock(pPath);
    }

    @Override
    public void unlock(final String pPath) throws UnlockException {
        lockManager.unlock(pPath);
    }

    private <T, E extends Exception> T executeWithinLock(final String pPath, WithinLockExecutable<T, E> pExecutable) throws E {
        if (!lockManager.isLocked(pPath)) {
            throw new PathNotLockedException(format("%s is not locked!", pPath));
        }
        return pExecutable.execute(pPath);
    }

    @Override
    public void delete(final String pPath) throws DeletionException {
        executeWithinLock(pPath, p -> {
            dataflowManager.delete(p);
            return null;
        });
    }

    @Override
    public void transfer(final String pPath, final ByteBuffer pData) throws ModificationException {
        executeWithinLock(pPath, p -> {
            dataflowManager.transfer(p, pData);
            return null;
        });
    }

    @Override
    public void store(final String pPath, final byte[] pChecksum, final IOException pFailureOrNull) throws StoreException {
        executeWithinLock(pPath, p -> {
            dataflowManager.store(p, pFailureOrNull);

            // Do only update the checksum when the store operation was sucessful
            checksums.put(p, pChecksum);
            return null;
        });
    }

    @Override
    public String getLocalNode() {
        return hci.getLocalEndpoint().getUuid();
    }

    @Override
    public byte[] getChecksum(final String pPath) {
        return executeWithinLock(pPath, p -> {
            final byte[] checksum = checksums.get(p);
            return checksum == null ? EMPTY_CHECKSUM : checksum;
        });
    }
}
