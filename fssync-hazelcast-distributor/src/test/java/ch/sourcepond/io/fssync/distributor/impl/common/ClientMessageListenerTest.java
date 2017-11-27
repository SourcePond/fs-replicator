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
package ch.sourcepond.io.fssync.distributor.impl.common;

import ch.sourcepond.io.fssync.distributor.api.GlobalPath;
import ch.sourcepond.io.fssync.distributor.impl.Constants;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientMessageListenerTest {
    private static final ArgumentMatcher<GlobalPath> IS_EQUAL_TO_EXPECTED_GLOBAL_PATH = gp -> Constants.EXPECTED_NODE.equals(gp.getSendingNode()) && Constants.EXPECTED_PATH.equals(gp.getPath());
    private final ClientMessageProcessor<String> processor = mock(ClientMessageProcessor.class);
    private final Message<String> message = mock(Message.class);
    private final Member member = mock(Member.class);
    private final ITopic<StatusMessage> sendResponseTopic = mock(ITopic.class);
    private final ClientMessageListenerFactory factory = new ClientMessageListenerFactory(sendResponseTopic);
    private final MessageListener<String> listener = factory.createListener(processor);

    @Before
    public void setup() {
        when(processor.toPath(Constants.EXPECTED_PAYLOAD)).thenReturn(Constants.EXPECTED_PATH);
        when(member.getUuid()).thenReturn(Constants.EXPECTED_NODE);
        when(message.getMessageObject()).thenReturn(Constants.EXPECTED_PAYLOAD);
        when(message.getPublishingMember()).thenReturn(member);
    }

    @Test
    public void onMessageSuccess() throws IOException {
        listener.onMessage(message);
        verify(processor).processMessage(argThat(IS_EQUAL_TO_EXPECTED_GLOBAL_PATH), same(Constants.EXPECTED_PAYLOAD));
        verify(sendResponseTopic).publish(argThat(sm -> Constants.EXPECTED_PATH.equals(sm.getPath()) && sm.getFailureOrNull() == null));
    }

    @Test
    public void onMessageFailure() throws IOException {
        Mockito.doThrow(Constants.EXPECTED_EXCEPTION).when(processor).processMessage(argThat(IS_EQUAL_TO_EXPECTED_GLOBAL_PATH), same(Constants.EXPECTED_PAYLOAD));
        listener.onMessage(message);
        verify(processor).processMessage(argThat(IS_EQUAL_TO_EXPECTED_GLOBAL_PATH), same(Constants.EXPECTED_PAYLOAD));
        verify(sendResponseTopic).publish(argThat(sm -> Constants.EXPECTED_PATH.equals(sm.getPath()) && Constants.EXPECTED_EXCEPTION.equals(sm.getFailureOrNull())));
    }
}
