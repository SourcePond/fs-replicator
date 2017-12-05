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
package ch.sourcepond.io.fssync.distributor.hazelcast.request;

import ch.sourcepond.io.fssync.distributor.hazelcast.common.ClientMessageProcessorTest;
import ch.sourcepond.io.fssync.distributor.hazelcast.common.DistributionMessage;
import org.junit.Test;

import java.io.IOException;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_PATH;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_SYNC_DIR;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StoreRequestProcessorTest extends ClientMessageProcessorTest<DistributionMessage, StoreRequestProcessor> {

    @Override
    protected StoreRequestProcessor createProcessor() {
        return new StoreRequestProcessor(syncTargets);
    }

    @Override
    protected DistributionMessage createMessage() {
        final DistributionMessage message = mock(DistributionMessage.class);
        when(message.getSyncDir()).thenReturn(EXPECTED_SYNC_DIR);
        when(message.getPath()).thenReturn(EXPECTED_PATH);
        return message;
    }

    @Test
    @Override
    public void processMessage() throws IOException {
        processor.processMessage(nodeInfo, syncPath, message);
        verify(syncTargets).store(nodeInfo, syncPath);
    }
}
