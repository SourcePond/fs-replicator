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

import ch.sourcepond.io.distributor.impl.Constants;
import ch.sourcepond.io.distributor.impl.common.StatusMessage;
import ch.sourcepond.io.distributor.spi.Receiver;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_NODE;
import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_PATH;
import static ch.sourcepond.io.distributor.impl.Constants.FAILURE_RESPONSE_ARGUMENT_MATCHER;
import static ch.sourcepond.io.distributor.impl.Constants.GLOBAL_PATH_ARGUMENT_MATCHER;
import static ch.sourcepond.io.distributor.impl.Constants.SUCCESS_RESPONSE_ARGUMENT_MATCHER;
import static com.hazelcast.core.MembershipEvent.MEMBER_REMOVED;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ClientLockListenerTest {
    private final Receiver receiver = mock(Receiver.class);
    private final ITopic<StatusMessage> sendFileLockResponseTopic = mock(ITopic.class);
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
    public void onMessageSuccess() throws Exception {
        listener.onMessage(message);
        final InOrder order = inOrder(receiver, sendFileLockResponseTopic);
        order.verify(receiver).lockLocally(argThat(GLOBAL_PATH_ARGUMENT_MATCHER));
        order.verify(sendFileLockResponseTopic).publish(argThat(SUCCESS_RESPONSE_ARGUMENT_MATCHER));
    }

    @Test
    public void onMessageFailed() throws Exception {
        doThrow(Constants.EXPECTED_EXCEPTION).when(receiver).lockLocally(argThat(GLOBAL_PATH_ARGUMENT_MATCHER));
        listener.onMessage(message);
        final InOrder order = inOrder(receiver, sendFileLockResponseTopic);
        order.verify(receiver).lockLocally(argThat(GLOBAL_PATH_ARGUMENT_MATCHER));
        order.verify(sendFileLockResponseTopic).publish(argThat(FAILURE_RESPONSE_ARGUMENT_MATCHER));
    }

    @Test
    public void memberRemoved() {
        final MembershipEvent event = new MembershipEvent(mock(Cluster.class), member, MEMBER_REMOVED, emptySet());
        listener.memberRemoved(event);
        verify(receiver).kill(EXPECTED_NODE);
    }

    @Test
    public void memberAdded() {
        listener.memberAdded(null);
        verifyZeroInteractions(receiver, sendFileLockResponseTopic, member, message);
    }

    @Test
    public void memberAttributeChanged() {
        listener.memberAttributeChanged(null);
        verifyZeroInteractions(receiver, sendFileLockResponseTopic, member, message);
    }
}
