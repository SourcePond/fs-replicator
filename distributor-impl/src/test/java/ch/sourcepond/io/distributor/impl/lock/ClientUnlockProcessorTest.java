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

import ch.sourcepond.io.distributor.api.GlobalPath;
import ch.sourcepond.io.distributor.spi.Receiver;
import org.junit.Test;

import java.io.IOException;

import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_PATH;
import static ch.sourcepond.io.distributor.impl.Constants.EXPECTED_PAYLOAD;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ClientUnlockProcessorTest {
    private final Receiver receiver = mock(Receiver.class);
    private final GlobalPath globalPath = mock(GlobalPath.class);
    private final ClientUnlockProcessor processor = new ClientUnlockProcessor(receiver);

    @Test
    public void toPath() {
        assertEquals(EXPECTED_PATH, processor.toPath(EXPECTED_PATH));
    }

    @Test
    public void processSuccess() throws IOException {
        processor.processMessage(globalPath, EXPECTED_PAYLOAD);
        verify(receiver).unlockLocally(globalPath);
    }
}
