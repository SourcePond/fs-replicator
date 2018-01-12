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
package ch.sourcepond.io.fssync.source.fs.trigger;

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.source.fs.Config;
import ch.sourcepond.io.fssync.source.fs.fswatch.RegularFile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SyncTriggerTest {
    private static final long EXPECTED_DELAY = 100;
    private static final TimeUnit EXPECTED_UNIT = SECONDS;
    private final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    private final Distributor distributor = mock(Distributor.class);
    private final Config config = mock(Config.class);
    private final SyncPath syncPath = mock(SyncPath.class);
    private final RegularFile regularFile = mock(RegularFile.class);
    private final SyncTriggerFunction syncTriggerFunction = mock(SyncTriggerFunction.class);
    private final SyncTrigger trigger = new SyncTrigger(executor, distributor, config, regularFile, syncTriggerFunction);

    @Before
    public void setup() throws Exception {
        when(regularFile.getSyncPath()).thenReturn(syncPath);
    }

    @Test
    public void runLockSucessful() throws Exception {
        when(distributor.tryLock(syncPath)).thenReturn(true);
        trigger.run();
        final InOrder order = inOrder(distributor, syncTriggerFunction);
        order.verify(distributor).tryLock(syncPath);
        order.verify(syncTriggerFunction).process(regularFile);
        order.verify(distributor).unlock(syncPath);
    }

    @Test
    public void runRetry() throws Exception {
        when(config.retryAttempts()).thenReturn(1);
        when(config.retryDelay()).thenReturn(EXPECTED_DELAY);
        when(config.retryDelayUnit()).thenReturn(EXPECTED_UNIT);
        trigger.run();
        verify(executor).schedule(trigger, EXPECTED_DELAY, EXPECTED_UNIT);
        trigger.run();
        verifyNoMoreInteractions(executor);
    }

    @Test
    public void runIOException() throws Exception {
        doThrow(IOException.class).when(distributor).tryLock(syncPath);
        trigger.run();
        verifyZeroInteractions(syncTriggerFunction, executor);
    }
}
