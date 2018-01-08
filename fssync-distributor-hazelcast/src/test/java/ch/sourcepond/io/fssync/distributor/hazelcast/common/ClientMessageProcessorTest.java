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

import ch.sourcepond.io.fssync.target.api.NodeInfo;
import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import com.hazelcast.core.Endpoint;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_LOCAL_NODE;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class ClientMessageProcessorTest<M extends DistributionMessage, T extends ClientMessageProcessor<M>> {
    protected final SyncTarget syncTarget = mock(SyncTarget.class);
    protected final SyncPath syncPath = mock(SyncPath.class);
    protected final HazelcastInstance hci = mock(HazelcastInstance.class);
    protected final Endpoint endpoint = mock(Endpoint.class);
    protected final NodeInfo nodeInfo = mock(NodeInfo.class);
    protected M message;
    protected T processor;

    @Before
    public void setup() {
        message = createMessage();
        processor = createProcessor();
        when(hci.getLocalEndpoint()).thenReturn(endpoint);
        when(endpoint.getUuid()).thenReturn(EXPECTED_LOCAL_NODE);
    }

    public abstract void processMessage() throws IOException;

    @Test
    public void verifyReceiver() {
        assertSame(syncTarget, processor.syncTarget);
    }

    protected abstract M createMessage();

    protected abstract T createProcessor();
}
