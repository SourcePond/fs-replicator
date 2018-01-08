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
package ch.sourcepond.io.fssync.distributor.hazelcast.request;

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Delete;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Discard;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Store;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Transfer;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.StatusMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.DeletionException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.DiscardException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.StoreException;
import ch.sourcepond.io.fssync.distributor.hazelcast.exception.TransferException;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ClusterResponseBarrierFactory;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ResponseException;
import com.hazelcast.core.ITopic;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

public class RequestDistributor {
    private final ClusterResponseBarrierFactory clusterResponseBarrierFactory;
    private final ITopic<DistributionMessage> deleteRequestTopic;
    private final ITopic<TransferRequest> transferRequestTopic;
    private final ITopic<StatusMessage> discardRequestTopic;
    private final ITopic<DistributionMessage> storeRequestTopic;

    @Inject
    RequestDistributor(final ClusterResponseBarrierFactory pClusterResponseBarrierFactory,
                       @Delete final ITopic<DistributionMessage> pDeleteRequestTopic,
                       @Transfer final ITopic<TransferRequest> pTransferRequestTopic,
                       @Discard final ITopic<StatusMessage> pDiscardRequestTopic,
                       @Store final ITopic<DistributionMessage> pStoreRequestTopic) {
        clusterResponseBarrierFactory = pClusterResponseBarrierFactory;
        deleteRequestTopic = pDeleteRequestTopic;
        transferRequestTopic = pTransferRequestTopic;
        discardRequestTopic = pDiscardRequestTopic;
        storeRequestTopic = pStoreRequestTopic;
    }

    public void transfer(final SyncPath pPath, final ByteBuffer pData) throws TransferException {
        // Transfer data into a byte array...
        final byte[] data = new byte[pData.limit()];
        pData.get(data);

        try {
            // ...and distribute it
            clusterResponseBarrierFactory.create(pPath, transferRequestTopic).awaitResponse(
                    new TransferRequest(pPath, data));
        } catch (final TimeoutException | ResponseException e) {
            throw new TransferException(format("Modification of %s failed on some node!", pPath), e);
        }
    }

    public void discard(final SyncPath pPath, final IOException pFailureOrNull) throws DiscardException {
        try {
            clusterResponseBarrierFactory.create(pPath, discardRequestTopic).awaitResponse(
                    new StatusMessage(pPath, pFailureOrNull));
        } catch (final TimeoutException | ResponseException e) {
            throw new DiscardException(format("Storing or reverting %s failed on some node!", pPath), e);
        }
    }

    public void store(final SyncPath pPath) throws StoreException {
        try {
            clusterResponseBarrierFactory.create(pPath, storeRequestTopic).awaitResponse(
                    new StatusMessage(pPath));
        } catch (final TimeoutException | ResponseException e) {
            throw new StoreException(format("Storing or reverting %s failed on some node!", pPath), e);
        }
    }

    public void delete(final SyncPath pPath) throws DeletionException {
        try {
            clusterResponseBarrierFactory.create(pPath, deleteRequestTopic).awaitResponse(
                    new StatusMessage(pPath));
        } catch (final TimeoutException | ResponseException e) {
            throw new DeletionException(format("Deletion of %s failed on some node!", pPath), e);
        }
    }
}
