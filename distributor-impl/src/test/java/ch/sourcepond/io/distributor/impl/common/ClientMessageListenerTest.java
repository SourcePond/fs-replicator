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

import ch.sourcepond.io.distributor.api.GlobalPath;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;

import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_EXCEPTION;
import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_NODE;
import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_PATH;
import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_PAYLOAD;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientMessageListenerTest {
    private static final ArgumentMatcher<GlobalPath> IS_EQUAL_TO_EXPECTED_GLOBAL_PATH = gp -> EXPECTED_NODE.equals(gp.getSendingNode()) && EXPECTED_PATH.equals(gp.getPath());
    private final ClientMessageProcessor<String> processor = mock(ClientMessageProcessor.class);
    private final Message<String> message = mock(Message.class);
    private final Member member = mock(Member.class);
    private final ITopic<StatusMessage> sendResponseTopic = mock(ITopic.class);
    private final ClientMessageListenerFactory factory = new ClientMessageListenerFactory(sendResponseTopic);
    private final MessageListener<String> listener = factory.createListener(processor);

    @Before
    public void setup() {
        when(processor.toPath(EXPECTED_PAYLOAD)).thenReturn(EXPECTED_PATH);
        when(member.getUuid()).thenReturn(EXPECTED_NODE);
        when(message.getMessageObject()).thenReturn(EXPECTED_PAYLOAD);
        when(message.getPublishingMember()).thenReturn(member);
    }

    @Test
    public void onMessageSuccess() throws IOException {
        listener.onMessage(message);
        verify(processor).processMessage(argThat(IS_EQUAL_TO_EXPECTED_GLOBAL_PATH), same(EXPECTED_PAYLOAD));
        verify(sendResponseTopic).publish(argThat(sm -> EXPECTED_PATH.equals(sm.getPath()) && sm.getFailureOrNull() == null));
    }

    @Test
    public void onMessageFailure() throws IOException {
        doThrow(EXPECTED_EXCEPTION).when(processor).processMessage(argThat(IS_EQUAL_TO_EXPECTED_GLOBAL_PATH), same(EXPECTED_PAYLOAD));
        listener.onMessage(message);
        verify(processor).processMessage(argThat(IS_EQUAL_TO_EXPECTED_GLOBAL_PATH), same(EXPECTED_PAYLOAD));
        verify(sendResponseTopic).publish(argThat(sm -> EXPECTED_PATH.equals(sm.getPath()) && EXPECTED_EXCEPTION.equals(sm.getFailureOrNull())));
    }
}
