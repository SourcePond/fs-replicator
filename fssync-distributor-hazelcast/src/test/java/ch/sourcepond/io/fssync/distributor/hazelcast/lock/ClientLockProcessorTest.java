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
package ch.sourcepond.io.fssync.distributor.hazelcast.lock;

import ch.sourcepond.io.fssync.distributor.hazelcast.common.ClientMessageProcessorTest;
import ch.sourcepond.io.fssync.distributor.hazelcast.Constants;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_PATH;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_SYNC_DIR;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.IS_EQUAL_TO_EXPECTED_NODE_INFO;
import static com.hazelcast.core.MembershipEvent.MEMBER_REMOVED;
import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientLockProcessorTest extends ClientMessageProcessorTest<DistributionMessage, ClientLockProcessor> {
    private final Member member = mock(Member.class);

    @Override
    protected ClientLockProcessor createProcessor() {
        return new ClientLockProcessor(hci, syncTarget);
    }

    @Override
    protected DistributionMessage createMessage() {
        final DistributionMessage message = mock(DistributionMessage.class);
        when(message.getSyncDir()).thenReturn(EXPECTED_SYNC_DIR);
        when(message.getPath()).thenReturn(EXPECTED_PATH);
        return message;
    }

    @Test
    public void memberRemoved() {
        when(member.getUuid()).thenReturn(Constants.EXPECTED_SENDER_NODE);
        final MembershipEvent event = new MembershipEvent(mock(Cluster.class), member, MEMBER_REMOVED, emptySet());
        processor.memberRemoved(event);
        verify(syncTarget).cancel(argThat(IS_EQUAL_TO_EXPECTED_NODE_INFO));
    }

    @Test
    public void memberAdded() {
        processor.memberAdded(null);
        Mockito.verifyZeroInteractions(syncTarget);
    }

    @Test
    public void memberAttributeChanged() {
        processor.memberAttributeChanged(null);
        Mockito.verifyZeroInteractions(syncTarget);
    }

    @Test
    @Override
    public void processMessage() throws IOException {
        processor.processMessage(nodeInfo, syncPath, message);
        verify(syncTarget).lock(nodeInfo, syncPath);
    }
}
