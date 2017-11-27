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

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;

public class MessageListenerRegistration {
    private final String registrationId;
    private final ITopic<?> topic;

    private MessageListenerRegistration(final String pRegistrationId, final ITopic<?> pTopic) {
        registrationId = pRegistrationId;
        topic = pTopic;
    }

    public void close() {
        topic.removeMessageListener(registrationId);
    }

    public static <T> MessageListenerRegistration register(final ITopic<T> pTopic, final MessageListener<T> pListener) {
        return new MessageListenerRegistration(pTopic.addMessageListener(pListener), pTopic);
    }
}
