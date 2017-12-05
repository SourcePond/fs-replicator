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
package ch.sourcepond.io.fssync.distributor.hazelcast.common;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import org.junit.Before;
import org.junit.Test;

import static ch.sourcepond.io.fssync.distributor.hazelcast.common.MessageListenerRegistration.register;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageListenerRegistrationTest {
    private static final String EXPECTED_REGISTRATION_ID = "someRegistrationId";
    private final ITopic<String> topic = mock(ITopic.class);
    private final MessageListener<String> listener = mock(MessageListener.class);
    private MessageListenerRegistration registration;

    @Before
    public void setup() {
        when(topic.addMessageListener(listener)).thenReturn(EXPECTED_REGISTRATION_ID);
        registration = register(topic, listener);
    }

    @Test
    public void close() {
        registration.close();
        verify(topic).removeMessageListener(EXPECTED_REGISTRATION_ID);
    }
}
