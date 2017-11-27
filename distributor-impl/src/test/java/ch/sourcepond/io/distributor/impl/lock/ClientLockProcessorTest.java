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

import ch.sourcepond.io.distributor.impl.common.ClientMessageProcessorTest;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import org.junit.Test;

import java.io.IOException;

import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_NODE;
import static com.hazelcast.core.MembershipEvent.MEMBER_REMOVED;
import static java.util.Collections.emptySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ClientLockProcessorTest extends ClientMessageProcessorTest<String, ClientLockProcessor> {
    private final Member member = mock(Member.class);

    @Override
    protected ClientLockProcessor createProcessor() {
        return new ClientLockProcessor(receiver);
    }

    @Override
    protected String createMessage() {
        return EXPECTED_PATH;
    }

    @Test
    public void memberRemoved() {
        when(member.getUuid()).thenReturn(EXPECTED_NODE);
        final MembershipEvent event = new MembershipEvent(mock(Cluster.class), member, MEMBER_REMOVED, emptySet());
        processor.memberRemoved(event);
        verify(receiver).kill(EXPECTED_NODE);
    }

    @Test
    public void memberAdded() {
        processor.memberAdded(null);
        verifyZeroInteractions(receiver);
    }

    @Test
    public void memberAttributeChanged() {
        processor.memberAttributeChanged(null);
        verifyZeroInteractions(receiver);
    }

    @Test
    @Override
    public void processMessage() throws IOException {
        processor.processMessage(path, message);
        verify(receiver).lockLocally(path);
    }
}
