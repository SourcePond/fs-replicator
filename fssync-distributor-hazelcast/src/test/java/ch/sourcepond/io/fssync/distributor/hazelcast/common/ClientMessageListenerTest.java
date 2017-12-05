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

import ch.sourcepond.io.fssync.distributor.hazelcast.Constants;
import com.hazelcast.core.Endpoint;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_LOCAL_NODE;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_PATH;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_SENDER_NODE;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_SYNC_DIR;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.IS_EQUAL_TO_EXPECTED_GLOBAL_PATH;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.IS_EQUAL_TO_EXPECTED_NODE_INFO;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientMessageListenerTest {
    private final ClientMessageProcessor<DistributionMessage> processor = mock(ClientMessageProcessor.class);
    private final Message<DistributionMessage> message = mock(Message.class);
    private final DistributionMessage payload = mock(DistributionMessage.class);
    private final Member member = mock(Member.class);
    private final ITopic<StatusMessage> sendResponseTopic = mock(ITopic.class);
    private final HazelcastInstance hci = mock(HazelcastInstance.class);
    private final Endpoint endpoint = mock(Endpoint.class);
    private final ClientMessageListenerFactory factory = new ClientMessageListenerFactory(hci, sendResponseTopic);
    private final MessageListener<DistributionMessage> listener = factory.createListener(processor);

    @Before
    public void setup() {
        when(endpoint.getUuid()).thenReturn(EXPECTED_LOCAL_NODE);
        when(hci.getLocalEndpoint()).thenReturn(endpoint);
        when(payload.getSyncDir()).thenReturn(EXPECTED_SYNC_DIR);
        when(payload.getPath()).thenReturn(EXPECTED_PATH);
        when(member.getUuid()).thenReturn(EXPECTED_SENDER_NODE);
        when(message.getMessageObject()).thenReturn(payload);
        when(message.getPublishingMember()).thenReturn(member);
    }

    @Test
    public void onMessageSuccess() throws IOException {
        listener.onMessage(message);
        verify(processor).processMessage(argThat(IS_EQUAL_TO_EXPECTED_NODE_INFO),
                argThat(IS_EQUAL_TO_EXPECTED_GLOBAL_PATH),
                same(payload));
        verify(sendResponseTopic).publish(argThat(sm -> EXPECTED_PATH.equals(sm.getPath()) && sm.getFailureOrNull() == null));
    }

    @Test
    public void onMessageFailure() throws IOException {
        doThrow(Constants.EXPECTED_EXCEPTION).when(processor).processMessage(argThat(IS_EQUAL_TO_EXPECTED_NODE_INFO), argThat(IS_EQUAL_TO_EXPECTED_GLOBAL_PATH), same(payload));
        listener.onMessage(message);
        verify(processor).processMessage(argThat(IS_EQUAL_TO_EXPECTED_NODE_INFO),
                argThat(IS_EQUAL_TO_EXPECTED_GLOBAL_PATH),
                same(payload));
        verify(sendResponseTopic).publish(argThat(sm -> EXPECTED_PATH.equals(sm.getPath()) && Constants.EXPECTED_EXCEPTION.equals(sm.getFailureOrNull())));
    }
}
