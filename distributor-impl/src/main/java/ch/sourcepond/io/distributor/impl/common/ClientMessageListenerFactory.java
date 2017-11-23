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
package ch.sourcepond.io.distributor.impl.common;

import ch.sourcepond.io.distributor.impl.binding.HazelcastBinding;
import com.hazelcast.core.MessageListener;

import java.io.Serializable;

public class ClientMessageListenerFactory {
    private final HazelcastBinding binding;

    public ClientMessageListenerFactory(final HazelcastBinding pBinding) {
        binding = pBinding;
    }

    public <T extends Serializable> MessageListener<T> createListener(final ClientMessageProcessor<T> pProcessor) {
        return new ClientMessageListener<>(pProcessor, binding.getResponseTopic());
    }
}
