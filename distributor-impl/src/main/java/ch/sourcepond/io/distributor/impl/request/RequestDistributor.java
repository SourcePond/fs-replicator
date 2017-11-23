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

import ch.sourcepond.io.distributor.api.exception.DeletionException;
import ch.sourcepond.io.distributor.api.exception.ModificationException;
import ch.sourcepond.io.distributor.api.exception.StoreException;
import ch.sourcepond.io.distributor.impl.ListenerRegistrar;
import ch.sourcepond.io.distributor.impl.binding.HazelcastBinding;
import ch.sourcepond.io.distributor.impl.common.ClientMessageListenerFactory;
import ch.sourcepond.io.distributor.impl.common.StatusMessage;
import ch.sourcepond.io.distributor.impl.response.ClusterResponseBarrierFactory;
import ch.sourcepond.io.distributor.impl.response.ResponseException;
import ch.sourcepond.io.distributor.spi.Receiver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

public class RequestDistributor implements ListenerRegistrar {
    private final ClusterResponseBarrierFactory clusterResponseBarrierFactory;
    private final RequestListenerFactory requestListenerFactory;
    private final HazelcastBinding binding;

    // Constructor for testing
    RequestDistributor(final ClusterResponseBarrierFactory pClusterResponseBarrierFactory,
                       final RequestListenerFactory pRequestListenerFactory,
                       final HazelcastBinding pBinding) {
        clusterResponseBarrierFactory = pClusterResponseBarrierFactory;
        requestListenerFactory = pRequestListenerFactory;
        binding = pBinding;
    }


    public RequestDistributor(final ClusterResponseBarrierFactory pClusterResponseBarrierFactory,
                              final HazelcastBinding pBinding) {
        this(pClusterResponseBarrierFactory,
                new RequestListenerFactory(new ClientMessageListenerFactory(pBinding)),
                pBinding);
    }

    public void transfer(final String pPath, final ByteBuffer pData) throws ModificationException {
        // Transfer data into a byte array...
        final byte[] data = new byte[pData.limit()];
        pData.get(data);

        try {
            // ...and distribute it
            clusterResponseBarrierFactory.create(pPath, binding.getTransferRequestTopic()).awaitResponse(new TransferRequest(pPath, data));
        } catch (final TimeoutException | ResponseException e) {
            throw new ModificationException(format("Modification of %s failed on some node!", pPath), e);
        }
    }

    public void store(final String pPath, final IOException pFailureOrNull) throws StoreException {
        try {
            clusterResponseBarrierFactory.create(pPath, binding.getStoreRequestTopic()).awaitResponse(new StatusMessage(pPath, pFailureOrNull));
        } catch (final TimeoutException | ResponseException e) {
            throw new StoreException(format("Storing or reverting %s failed on some node!", pPath), e);
        }
    }

    public void delete(final String pPath) throws DeletionException {
        try {
            clusterResponseBarrierFactory.create(pPath, binding.getDeleteRequestTopic()).awaitResponse(pPath);
        } catch (final TimeoutException | ResponseException e) {
            throw new DeletionException(format("Deletion of %s failed on some node!", pPath), e);
        }
    }

    @Override
    public void registerListeners(final Receiver pReceiver) {
        binding.getDeleteRequestTopic().addMessageListener(requestListenerFactory.createDeleteListener(pReceiver));
        binding.getTransferRequestTopic().addMessageListener(requestListenerFactory.createTransferListener(pReceiver));
        binding.getStoreRequestTopic().addMessageListener(requestListenerFactory.createStoreListener(pReceiver));
    }
}
