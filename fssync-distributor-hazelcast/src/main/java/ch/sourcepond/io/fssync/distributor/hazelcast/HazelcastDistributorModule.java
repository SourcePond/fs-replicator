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

import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Delete;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Discard;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Lock;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Response;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Store;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Transfer;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Unlock;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.CommonModule;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.MessageListenerRegistration;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.StatusMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.config.DistributorConfig;
import ch.sourcepond.io.fssync.distributor.hazelcast.lock.LockModule;
import ch.sourcepond.io.fssync.distributor.hazelcast.request.RequestModule;
import ch.sourcepond.io.fssync.distributor.hazelcast.request.TransferRequest;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ResponseModule;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;

import javax.inject.Singleton;

import static com.google.inject.Key.get;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static com.hazelcast.core.Hazelcast.newHazelcastInstance;

public class HazelcastDistributorModule extends AbstractModule {
    static final String MAP_NAME = "checksum";
    private final DistributorConfig config;
    private final Config hazelcastConfig;
    private final SyncTarget compoundSyncTarget;

    public HazelcastDistributorModule(final DistributorConfig pConfig, final Config pHazelcastConfig, final SyncTarget pCompoundSyncTarget) {
        config = pConfig;
        hazelcastConfig = pHazelcastConfig;
        compoundSyncTarget = pCompoundSyncTarget;
    }

    @Override
    protected void configure() {
        bind(DistributorConfig.class).toInstance(config);
        bind(SyncTarget.class).toInstance(compoundSyncTarget);
        install(new CommonModule());
        install(new LockModule());
        install(new RequestModule());
        install(new ResponseModule());

        Multibinder<MessageListenerRegistration> registrations = newSetBinder(binder(), MessageListenerRegistration.class);
        registrations.addBinding().to(get(MessageListenerRegistration.class, Lock.class));
        registrations.addBinding().to(get(MessageListenerRegistration.class, Unlock.class));
        registrations.addBinding().to(get(MessageListenerRegistration.class, Delete.class));
        registrations.addBinding().to(get(MessageListenerRegistration.class, Transfer.class));
        registrations.addBinding().to(get(MessageListenerRegistration.class, Discard.class));
        registrations.addBinding().to(get(MessageListenerRegistration.class, Store.class));

        bind(HazelcastDistributor.class);
    }

    @Provides
    @Singleton
    HazelcastInstance hazelcastInstance() {
        return newHazelcastInstance(hazelcastConfig);
    }

    @Provides
    @Singleton
    @Response
    ITopic<StatusMessage> responseTopic(final HazelcastInstance pHci) {
        return pHci.getTopic(Response.NAME);
    }

    @Provides
    @Singleton
    @Delete
    ITopic<DistributionMessage> deleteRequestTopic(final HazelcastInstance pHci) {
        return pHci.getTopic(Delete.NAME);
    }

    @Provides
    @Singleton
    @Transfer
    ITopic<TransferRequest> transferRequestTopic(final HazelcastInstance pHci) {
        return pHci.getTopic(Transfer.NAME);
    }

    @Provides
    @Singleton
    @Discard
    ITopic<StatusMessage> discardRequestTopic(final HazelcastInstance pHci) {
        return pHci.getTopic(Discard.NAME);
    }

    @Provides
    @Singleton
    @Store
    ITopic<DistributionMessage> storeRequestTopic(final HazelcastInstance pHci) {
        return pHci.getTopic(Store.NAME);
    }

    @Provides
    @Singleton
    @Lock
    ITopic<DistributionMessage> lockRequestTopic(final HazelcastInstance pHci) {
        return pHci.getTopic(Lock.NAME);
    }

    @Provides
    @Singleton
    @Unlock
    ITopic<DistributionMessage> unlockRequestTopic(final HazelcastInstance pHci) {
        return pHci.getTopic(Unlock.NAME);
    }

    @Provides
    @Singleton
    IMap<String, byte[]> checksumMap(final HazelcastInstance pHci) {
        return pHci.getMap(MAP_NAME);
    }
}
