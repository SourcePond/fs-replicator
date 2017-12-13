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
package ch.sourcepond.io.fssync.impl;

import ch.sourcepond.io.fssync.distributor.api.DeletionException;
import ch.sourcepond.io.fssync.distributor.api.DiscardException;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.distributor.api.LockException;
import ch.sourcepond.io.fssync.distributor.api.StoreException;
import ch.sourcepond.io.fssync.distributor.api.TransferException;
import ch.sourcepond.io.fssync.distributor.api.UnlockException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CompoundDistributor implements ServiceListener, Distributor {
    private final ConcurrentMap<ServiceReference<Distributor>, Distributor> targets = new ConcurrentHashMap<>();

    void register() {

    }

    @Override
    public void serviceChanged(final ServiceEvent pServiceEvent) {
        final ServiceReference<Distributor> reference = (ServiceReference<Distributor>) pServiceEvent.getServiceReference();

        switch (pServiceEvent.getType()) {
            case ServiceEvent.UNREGISTERING:
            case ServiceEvent.MODIFIED_ENDMATCH: {
                targets.remove(reference);
                break;
            }
            case ServiceEvent.REGISTERED: {
                registerService(reference);
                break;
            }
            default: {
                // noop
            }
        }
    }

    @Override
    public boolean tryLock(String pSyncDir, String pPath) throws LockException {
        return false;
    }

    @Override
    public void unlock(String pSyncDir, String pPath) throws UnlockException {

    }

    @Override
    public void delete(String pSyncDir, String pPath) throws DeletionException {

    }

    @Override
    public void transfer(String pSyncDir, String pPath, ByteBuffer pData) throws TransferException {

    }

    @Override
    public void discard(String pSyncDir, String pPath, IOException pFailure) throws DiscardException {

    }

    @Override
    public void store(String pSyncDir, String pPath, byte[] pChecksum) throws StoreException {

    }

    @Override
    public byte[] getChecksum(String pSyncDir, String pPath) {
        return new byte[0];
    }
}
