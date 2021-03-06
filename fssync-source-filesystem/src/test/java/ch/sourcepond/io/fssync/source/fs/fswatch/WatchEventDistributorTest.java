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

import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Update;
import ch.sourcepond.io.checksum.api.UpdateObserver;
import ch.sourcepond.io.fssync.source.fs.trigger.ReplicationTrigger;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class WatchEventDistributorTest {
    private static final byte[] EXPECTECTED_CHECKSUM = new byte[0];
    private final RegularFileFactory regularFileFactory = mock(RegularFileFactory.class);
    private final RegularFile regularFile = mock(RegularFile.class);
    private final Update update = mock(Update.class);
    private final Checksum checksum = mock(Checksum.class);
    private final WatchService watchService = mock(WatchService.class);
    private final WatchKey watchKey = mock(WatchKey.class);
    private final ReplicationTrigger replicationTrigger = mock(ReplicationTrigger.class);
    private final Path syncDir = mock(Path.class);
    private final BasicFileAttributes syncDirAttributes = mock(BasicFileAttributes.class);
    private final Path file = mock(Path.class);
    private final BasicFileAttributes fileAttributes = mock(BasicFileAttributes.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final WatchEventDistributor distributor = new WatchEventDistributor(regularFileFactory, watchService,
            replicationTrigger, syncDir);

    @Before
    public void setup() throws Exception {
        doAnswer(inv -> {
            final UpdateObserver obs = inv.getArgument(0);
            obs.done(update);
            return null;
        }).when(regularFile).update(notNull());
        when(update.hasChanged()).thenReturn(true);
        when(update.getCurrent()).thenReturn(checksum);
        when(checksum.toByteArray()).thenReturn(EXPECTECTED_CHECKSUM);
        when(regularFileFactory.create(syncDir, file)).thenReturn(regularFile);
        when(syncDirAttributes.isDirectory()).thenReturn(true);
        when(syncDir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)).thenReturn(watchKey);
        when(fs.provider()).thenReturn(provider);
        when(provider.readAttributes(file, BasicFileAttributes.class)).thenReturn(fileAttributes);
        when(provider.readAttributes(syncDir, BasicFileAttributes.class)).thenReturn(syncDirAttributes);
        when(syncDir.getFileSystem()).thenReturn(fs);
        when(fileAttributes.isRegularFile()).thenReturn(true);
        when(file.getFileSystem()).thenReturn(fs);
    }

    @Test
    public void preVisitDirectory() throws Exception {
        distributor.preVisitDirectory(syncDir, null);
        distributor.close();
        verify(watchKey).cancel();
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

    @Test
    public void visitFile() throws Exception {
        distributor.visitFile(file, null);
        verify(replicationTrigger).modify(regularFile, EXPECTECTED_CHECKSUM);
    }

    @Test
    public void visitAndModifyFile() throws Exception {
        distributor.visitFile(file, null);
        distributor.modify(file);
        verify(replicationTrigger, times(2)).modify(regularFile, EXPECTECTED_CHECKSUM);
    }

    @Test
    public void modifyFileNewResourceCreated() throws Exception {
        distributor.modify(file);
        verify(replicationTrigger).modify(regularFile, EXPECTECTED_CHECKSUM);
    }

    @Test
    public void modifyNothingChanged() throws Exception {
        when(update.hasChanged()).thenReturn(false);
        distributor.modify(file);
        verifyZeroInteractions(replicationTrigger);
    }

    @Test
    public void createDirectory() throws Exception {
        distributor.create(syncDir);
        distributor.close();
        verify(watchKey).cancel();
    }

    @Test
    public void createFile() throws Exception {
        distributor.create(file);
        verify(replicationTrigger).modify(regularFile, EXPECTECTED_CHECKSUM);
    }

    @Test
    public void deleteNoSuchPath() {
        // This should not cause an exception to be thrown
        distributor.delete(mock(Path.class));
    }

    @Test
    public void deleteDirectory() throws Exception {
        distributor.create(syncDir);
        distributor.delete(syncDir);

        // Nothing should happen
        distributor.delete(syncDir);
        verify(watchKey).cancel();
        verifyZeroInteractions(replicationTrigger);
    }

    @Test
    public void deleteFile() throws Exception {
        distributor.create(file);
        distributor.delete(file);

        // Nothing should happen
        distributor.delete(file);
        verify(replicationTrigger).delete(regularFile);
    }
}
