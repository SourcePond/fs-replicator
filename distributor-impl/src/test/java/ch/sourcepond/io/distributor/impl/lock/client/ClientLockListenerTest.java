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
package ch.sourcepond.io.distributor.impl.lock.client;

import ch.sourcepond.io.distributor.impl.lock.FileLockMessage;
import ch.sourcepond.io.distributor.impl.lock.client.ClientLockListener;
import ch.sourcepond.io.distributor.spi.Receiver;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClientLockListenerTest {
    private static final String EXPECTED_NODE = "someNode";
    private static final String EXPECTED_PATH = "somePath";
    private final Receiver receiver = mock(Receiver.class);
    private final ITopic<FileLockMessage> sendFileLockResponseTopic = mock(ITopic.class);
    private final Member member = mock(Member.class);
    private final Message<String> message = mock(Message.class);
    private final ClientLockListener listener = new ClientLockListener(receiver, sendFileLockResponseTopic);

    @Before
    public void setup() {
        when(member.getUuid()).thenReturn(EXPECTED_NODE);
        when(message.getPublishingMember()).thenReturn(member);
        when(message.getMessageObject()).thenReturn(EXPECTED_PATH);
    }

    @Test
    public void onMessageFailed() throws Exception {
        listener.onMessage(message);
        final InOrder order = inOrder(receiver, sendFileLockResponseTopic);
        order.verify(receiver).lockLocally(EXPECTED_NODE, EXPECTED_PATH);
        order.verify(sendFileLockResponseTopic).publish(argThat(response -> {
            return EXPECTED_PATH.equals(response.getPath()) && response.getFailureOrNull() == null;
        }));
    }

    @Test
    public void onMessageSuccess() throws Exception {
        final IOException expected = new IOException();
        doThrow(expected).when(receiver).lockLocally(EXPECTED_NODE,EXPECTED_PATH);
        listener.onMessage(message);
        final InOrder order = inOrder(receiver, sendFileLockResponseTopic);
        order.verify(receiver).lockLocally(EXPECTED_NODE,EXPECTED_PATH);
        order.verify(sendFileLockResponseTopic).publish(argThat(response -> {
            return EXPECTED_PATH.equals(response.getPath()) && expected.equals(response.getFailureOrNull());
        }));
    }
}
