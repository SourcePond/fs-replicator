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

import ch.sourcepond.io.distributor.impl.annotations.Delete;
import ch.sourcepond.io.distributor.impl.annotations.Store;
import ch.sourcepond.io.distributor.impl.annotations.Transfer;
import ch.sourcepond.io.distributor.impl.common.ClientMessageListenerFactory;
import ch.sourcepond.io.distributor.impl.common.StatusMessage;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.hazelcast.core.MessageListener;

import javax.inject.Singleton;

public class RequestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(DeleteRequestProcessor.class);
        bind(TransferRequestProcessor.class);
        bind(StoreRequestProcessor.class);
        bind(RequestDistributor.class);
    }

    @Provides
    @Singleton
    @Delete
    MessageListener<String> deleteListener(final ClientMessageListenerFactory pFactory, final DeleteRequestProcessor pProcessor) {
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
    MessageListener<StatusMessage> storeListener(final ClientMessageListenerFactory pFactory, final StoreRequestProcessor pProcessor) {
        return pFactory.createListener(pProcessor);
    }
}
