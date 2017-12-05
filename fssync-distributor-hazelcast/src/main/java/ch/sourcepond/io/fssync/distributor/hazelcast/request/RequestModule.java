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

import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Delete;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Discard;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Store;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Transfer;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.ClientMessageListenerFactory;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.MessageListenerRegistration;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.StatusMessage;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

import javax.inject.Singleton;

import static ch.sourcepond.io.fssync.distributor.hazelcast.common.MessageListenerRegistration.register;

public class RequestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DeleteRequestProcessor.class);
        bind(TransferRequestProcessor.class);
        bind(DiscardRequestProcessor.class);
        bind(StoreRequestProcessor.class);
        bind(RequestDistributor.class);
    }

    @Provides
    @Singleton
    @Delete
    MessageListener<DistributionMessage> deleteListener(final ClientMessageListenerFactory pFactory, final DeleteRequestProcessor pProcessor) {
        return pFactory.createListener(pProcessor);
    }

    @Provides
    @Singleton
    @Transfer
    MessageListener<TransferRequest> transferListener(final ClientMessageListenerFactory pFactory, final TransferRequestProcessor pProcessor) {
        return pFactory.createListener(pProcessor);
    }

    @Provides
    @Singleton
    @Store
    MessageListener<DistributionMessage> storeListener(final ClientMessageListenerFactory pFactory, final StoreRequestProcessor pProcessor) {
        return pFactory.createListener(pProcessor);
    }

    @Provides
    @Singleton
    @Discard
    MessageListener<StatusMessage> discardListener(final ClientMessageListenerFactory pFactory, final DiscardRequestProcessor pProcessor) {
        return pFactory.createListener(pProcessor);
    }

    @Provides
    @Singleton
    @Delete
    MessageListenerRegistration registerDeleteListener(final @Delete ITopic<String> pDeleteTopic, final @Delete MessageListener<String> pDeleteListener) {
        return register(pDeleteTopic, pDeleteListener);
    }

    @Provides
    @Singleton
    @Transfer
    MessageListenerRegistration registerTransferListener(final @Transfer ITopic<TransferRequest> pTransferTopic, final @Transfer MessageListener<TransferRequest> pTransferListener) {
        return register(pTransferTopic, pTransferListener);
    }

    @Provides
    @Singleton
    @Discard
    MessageListenerRegistration registerDiscardListener(final @Discard ITopic<StatusMessage> pDiscardTopic, final @Discard MessageListener<StatusMessage> pDiscardListener) {
        return register(pDiscardTopic, pDiscardListener);
    }

    @Provides
    @Singleton
    @Store
    MessageListenerRegistration registerStoreListener(final @Store ITopic<String> pStoreTopic, final @Store MessageListener<String> pStoreListener) {
        return register(pStoreTopic, pStoreListener);
    }
}
