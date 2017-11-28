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
package ch.sourcepond.io.fssync.distributor.impl.request;

import ch.sourcepond.io.fssync.distributor.impl.common.ClientMessageProcessorTest;
import ch.sourcepond.io.fssync.distributor.impl.common.StatusMessage;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DiscardRequestProcessorTest extends ClientMessageProcessorTest<StatusMessage, DiscardRequestProcessor> {

    @Override
    protected DiscardRequestProcessor createProcessor() {
        return new DiscardRequestProcessor(client);
    }

    @Override
    protected StatusMessage createMessage() {
        final StatusMessage message = mock(StatusMessage.class);
        when(message.getPath()).thenReturn(EXPECTED_PATH);
        return message;
    }

    @Test
    @Override
    public void processMessage() throws IOException {
        final IOException expected = new IOException();
        when(message.getFailureOrNull()).thenReturn(expected);
        processor.processMessage(path, message);
        verify(client).discard(path, expected);
    }
}