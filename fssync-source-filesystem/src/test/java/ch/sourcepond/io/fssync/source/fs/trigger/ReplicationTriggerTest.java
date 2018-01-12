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
import ch.sourcepond.io.fssync.source.fs.fswatch.DigestingChannel;
import ch.sourcepond.io.fssync.source.fs.fswatch.RegularFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.concurrent.ScheduledExecutorService;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static ch.sourcepond.io.checksum.api.Checksum.toHexString;
import static ch.sourcepond.io.fssync.source.fs.Constants.EXPECTED_CHECKSUM;
import static ch.sourcepond.io.fssync.source.fs.Constants.TEST_DATA_FILE;
import static java.lang.Thread.sleep;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.StandardOpenOption.READ;
import static java.security.MessageDigest.getInstance;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReplicationTriggerTest {
    private static final int BUFFER_SIZE = 32;
    private static final byte[] DISTRIBUTOR_CHECKSUM = new byte[]{1, 2, 3, 4};
    private static final byte[] DIFFERENT_CHECKSUM = new byte[]{5, 6, 7, 8};
    private final Config config = mock(Config.class);
    private final Distributor distributor = mock(Distributor.class);
    private final ScheduledExecutorService executorService = newSingleThreadScheduledExecutor();
    private final SyncTriggerFactory syncTriggerFactory = new SyncTriggerFactory(distributor, executorService, config);
    private final DigestingChannel channel = mock(DigestingChannel.class);
    private final SyncPath syncPath = mock(SyncPath.class);
    private final RegularFile regularFile = mock(RegularFile.class);
    private final ReplicationTrigger trigger = new ReplicationTrigger(distributor, syncTriggerFactory, executorService, config);
    private MessageDigest digest;
    private FileChannel testDataInput;
    private byte[] checksum;

    @Before
    public void setup() throws Exception {
        digest = getInstance(SHA256.toString());
        when(distributor.tryLock(syncPath)).thenReturn(true);
        when(distributor.getChecksum(syncPath)).thenReturn(DISTRIBUTOR_CHECKSUM);
        when(regularFile.startDigest()).thenReturn(channel);
        when(regularFile.getSyncPath()).thenReturn(syncPath);
        when(config.readBufferSize()).thenReturn(BUFFER_SIZE);
        doAnswer(inv -> {
            final ByteBuffer buffer = inv.getArgument(1);
            digest.update(buffer);
            return null;
        }).when(distributor).transfer(eq(syncPath), notNull());
        doAnswer(inv -> {
            checksum = digest.digest();
            return null;
        }).when(channel).close();
        when(channel.digest()).thenReturn(DISTRIBUTOR_CHECKSUM);
        when(channel.read(notNull())).thenAnswer(inv -> testDataInput.read((ByteBuffer) inv.getArgument(0)));
        testDataInput = open(TEST_DATA_FILE, READ);
    }

    @After
    public void tearDown() throws Exception {
        testDataInput.close();
        executorService.shutdown();
    }

    @Test
    public void modify() throws Exception {
        trigger.modify(regularFile, DIFFERENT_CHECKSUM);
        verify(distributor, timeout(5000)).store(eq(syncPath), notNull());
        assertEquals(EXPECTED_CHECKSUM, toHexString(checksum));
    }

    @Test
    public void modifyNothingChanged() throws Exception {
        trigger.modify(regularFile, DISTRIBUTOR_CHECKSUM);
        sleep(1000);
        verify(distributor,  never()).store(eq(syncPath), notNull());
    }

    @Test
    public void modifyIOExceptionOccurred() throws Exception {
        final IOException expected = new IOException();
        doThrow(expected).when(channel).read(notNull());
        trigger.modify(regularFile, DIFFERENT_CHECKSUM);
        verify(distributor, timeout(5000)).discard(syncPath, expected);
    }

    @Test
    public void delete() throws IOException {
        trigger.delete(regularFile);
        verify(distributor, timeout(5000)).delete(syncPath);
    }
}
