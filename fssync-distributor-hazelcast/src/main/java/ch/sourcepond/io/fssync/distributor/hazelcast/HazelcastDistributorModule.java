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
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Store;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Transfer;
import ch.sourcepond.io.fssync.distributor.hazelcast.annotations.Unlock;
import ch.sourcepond.io.fssync.distributor.hazelcast.binding.BindingModule;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.CommonModule;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.MessageListenerRegistration;
import ch.sourcepond.io.fssync.distributor.hazelcast.lock.LockModule;
import ch.sourcepond.io.fssync.distributor.hazelcast.request.RequestModule;
import ch.sourcepond.io.fssync.distributor.hazelcast.response.ResponseModule;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import static com.google.inject.Key.get;
import static com.google.inject.multibindings.Multibinder.newSetBinder;

public class HazelcastDistributorModule extends AbstractModule {
    private final Config config;

    public HazelcastDistributorModule(final Config pConfig) {
        config = pConfig;
    }

    @Override
    protected void configure() {
        bind(Config.class).toInstance(config);
        bind(SyncTargets.class);
        install(new BindingModule());
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
}
