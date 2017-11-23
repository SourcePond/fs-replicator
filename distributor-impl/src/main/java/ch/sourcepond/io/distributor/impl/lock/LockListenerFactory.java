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

import ch.sourcepond.io.distributor.impl.common.ClientMessageListenerFactory;
import ch.sourcepond.io.distributor.spi.Receiver;
import com.hazelcast.core.MessageListener;

class LockListenerFactory {
    private final ClientMessageListenerFactory clientMessageListenerFactory;

    public LockListenerFactory(final ClientMessageListenerFactory pClientMessageListenerFactory) {
        clientMessageListenerFactory = pClientMessageListenerFactory;
    }

    public MessageListener<String> createLockListener(final Receiver pReceiver) {
        return clientMessageListenerFactory.createListener(new ClientLockProcessor(pReceiver));
    }

    public MessageListener<String> createUnlockListener(final Receiver pReceiver) {
        return clientMessageListenerFactory.createListener(new ClientUnlockProcessor(pReceiver));
    }
}
