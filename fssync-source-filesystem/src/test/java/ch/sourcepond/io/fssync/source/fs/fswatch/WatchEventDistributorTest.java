/*Copyright (C) 2018 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.fssync.source.fs.fswatch;

import ch.sourcepond.io.checksum.api.ResourceProducer;
import ch.sourcepond.io.fssync.source.fs.trigger.ReplicationTrigger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WatchEventDistributorTest {
    private final ResourceProducer resourceProducer = mock(ResourceProducer.class);
    private final WatchService watchService = mock(WatchService.class);
    private final WatchKey watchKey = mock(WatchKey.class);
    private final ReplicationTrigger replicationTrigger = mock(ReplicationTrigger.class);
    private final Path syncDir = mock(Path.class);
    private final WatchEventDistributor distributor = new WatchEventDistributor(resourceProducer, watchService,
            replicationTrigger, syncDir);

    @Before
    public void setup() throws Exception {
        when(syncDir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)).thenReturn(watchKey);
    }

    @Test
    public void preVisitDirectoryExceptionOccurred() throws Exception {
        final IOException expected = new IOException();
        doThrow(expected).when(syncDir).register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        try {
            distributor.preVisitDirectory(syncDir, null);
            fail("Exception expected");
        } catch (final IOException e) {
            assertSame(expected, e.getCause().getCause());
        }
    }
}
