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
package ch.sourcepond.io.distributor.impl.lock;

import ch.sourcepond.io.distributor.impl.annotations.Lock;
import ch.sourcepond.io.distributor.impl.annotations.Unlock;
import ch.sourcepond.io.distributor.impl.common.ClientMessageListenerFactory;
import ch.sourcepond.io.distributor.impl.common.MessageListenerRegistration;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

import javax.inject.Singleton;

import static ch.sourcepond.io.distributor.impl.common.MessageListenerRegistration.register;

public class LockModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ClientLockProcessor.class);
        bind(ClientUnlockProcessor.class);
        bind(LockManager.class);
    }

    @Provides
    @Singleton
    @Lock
    public MessageListener<String> lockListener(final ClientMessageListenerFactory pFactory, final ClientLockProcessor pProcessor) {
        return pFactory.createListener(pProcessor);
    }

    @Provides
    @Singleton
    @Unlock
    public MessageListener<String> unlockListener(final ClientMessageListenerFactory pFactory, final ClientUnlockProcessor pProcessor) {
        return pFactory.createListener(pProcessor);
    }

    @Provides
    @Singleton
    @Lock
    public MessageListenerRegistration registerLockListener(final @Lock ITopic<String> pLockTopic, final @Lock MessageListener<String> pLockListener) {
        return register(pLockTopic, pLockListener);
    }

    @Provides
    @Singleton
    @Unlock
    public MessageListenerRegistration registerUnlockListener(final @Unlock ITopic<String> pUnlockTopic, final @Unlock MessageListener<String> pUnlockListener) {
        return register(pUnlockTopic, pUnlockListener);
    }
}
