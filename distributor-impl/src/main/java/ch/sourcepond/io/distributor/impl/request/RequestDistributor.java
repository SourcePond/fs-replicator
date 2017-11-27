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
package ch.sourcepond.io.distributor.impl.request;

import ch.sourcepond.io.distributor.api.DeletionException;
import ch.sourcepond.io.distributor.api.TransferException;
import ch.sourcepond.io.distributor.api.StoreException;
import ch.sourcepond.io.distributor.impl.annotations.Delete;
import ch.sourcepond.io.distributor.impl.annotations.Store;
import ch.sourcepond.io.distributor.impl.annotations.Transfer;
import ch.sourcepond.io.distributor.impl.common.StatusMessage;
import ch.sourcepond.io.distributor.impl.response.ClusterResponseBarrierFactory;
import ch.sourcepond.io.distributor.impl.response.ResponseException;
import com.hazelcast.core.ITopic;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

public class RequestDistributor {
    private final ClusterResponseBarrierFactory clusterResponseBarrierFactory;
    private final ITopic<String> deleteRequestTopic;
    private final ITopic<TransferRequest> transferRequestTopic;
    private final ITopic<StatusMessage> storeRequestTopic;

    @Inject
    RequestDistributor(final ClusterResponseBarrierFactory pClusterResponseBarrierFactory,
                       @Delete final ITopic<String> pDeleteRequestTopic,
                       @Transfer final ITopic<TransferRequest> pTransferRequestTopic,
                       @Store final ITopic<StatusMessage> pStoreRequestTopic) {
        clusterResponseBarrierFactory = pClusterResponseBarrierFactory;
        deleteRequestTopic = pDeleteRequestTopic;
        transferRequestTopic = pTransferRequestTopic;
        storeRequestTopic = pStoreRequestTopic;
    }

    public void transfer(final String pPath, final ByteBuffer pData) throws TransferException {
        // Transfer data into a byte array...
        final byte[] data = new byte[pData.limit()];
        pData.get(data);

        try {
            // ...and distribute it
            clusterResponseBarrierFactory.create(pPath, transferRequestTopic).awaitResponse(new TransferRequest(pPath, data));
        } catch (final TimeoutException | ResponseException e) {
            throw new TransferException(format("Modification of %s failed on some node!", pPath), e);
        }
    }

    public void store(final String pPath, final IOException pFailureOrNull) throws StoreException {
        try {
            clusterResponseBarrierFactory.create(pPath, storeRequestTopic).awaitResponse(new StatusMessage(pPath, pFailureOrNull));
        } catch (final TimeoutException | ResponseException e) {
            throw new StoreException(format("Storing or reverting %s failed on some node!", pPath), e);
        }
    }

    public void delete(final String pPath) throws DeletionException {
        try {
            clusterResponseBarrierFactory.create(pPath, deleteRequestTopic).awaitResponse(pPath);
        } catch (final TimeoutException | ResponseException e) {
            throw new DeletionException(format("Deletion of %s failed on some node!", pPath), e);
        }
    }
}
