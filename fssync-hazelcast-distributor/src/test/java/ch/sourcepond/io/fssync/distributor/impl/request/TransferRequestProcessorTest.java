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
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransferRequestProcessorTest extends ClientMessageProcessorTest<TransferRequest, TransferRequestProcessor> {
    private final byte[] EXPECTED_DATA = new byte[]{1, 2, 3, 4, 5};

    @Override
    protected TransferRequestProcessor createProcessor() {
        return new TransferRequestProcessor(client);
    }

    @Override
    protected TransferRequest createMessage() {
        final TransferRequest message = mock(TransferRequest.class);
        when(message.getPath()).thenReturn(EXPECTED_PATH);
        when(message.getData()).thenReturn(EXPECTED_DATA);
        return message;
    }

    @Test
    @Override
    public void processMessage() throws IOException {
        processor.processMessage(path, message);
        verify(client).transfer(eq(path), argThat(data -> Arrays.equals(EXPECTED_DATA, data.array())));
    }
}
