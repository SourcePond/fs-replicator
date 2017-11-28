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
import ch.sourcepond.io.fssync.distributor.spi.Client;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public abstract class ClientMessageProcessorTest<M, T extends ClientMessageProcessor<M>> {
    public static final String EXPECTED_PATH = "somePath";
    protected final Client client = mock(Client.class);
    protected final GlobalPath path = new GlobalPath("any", EXPECTED_PATH);
    protected M message;
    protected T processor;

    @Before
    public void setup() {
        message = createMessage();
        processor = createProcessor();
    }

    public abstract void processMessage() throws IOException;

    @Test
    public void verifyReceiver() {
        assertSame(client, processor.client);
    }

    protected abstract M createMessage();

    protected abstract T createProcessor();

    @Test
    public void toPath() {
        assertEquals(EXPECTED_PATH, processor.toPath(message));
    }
}
