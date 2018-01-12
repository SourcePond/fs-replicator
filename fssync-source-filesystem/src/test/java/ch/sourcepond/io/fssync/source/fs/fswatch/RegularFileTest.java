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

import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.ResourceProducer;
import ch.sourcepond.io.checksum.api.UpdateObserver;
import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.common.api.SyncPathFactory;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static java.nio.file.StandardOpenOption.READ;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegularFileTest {
    private final DigestingChannelFactory digestingChannelFactory = mock(DigestingChannelFactory.class);
    private final SyncPathFactory syncPathFactory = mock(SyncPathFactory.class);
    private final ResourceProducer resourceProducer = mock(ResourceProducer.class);
    private final Resource resource = mock(Resource.class);
    private final DigestingChannel channel = mock(DigestingChannel.class);
    private final SyncPath syncPath = mock(SyncPath.class);
    private final Path syncDir = mock(Path.class);
    private final Path absolutePath = mock(Path.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final FileChannel fileChannel = mock(FileChannel.class);
    private final UpdateObserver observer = mock(UpdateObserver.class);
    private RegularFile regularFile;

    @Before
    public void setup() throws Exception {
        when(digestingChannelFactory.create()).thenReturn(channel);
        when(resourceProducer.create(SHA256, absolutePath)).thenReturn(resource);
        when(syncPathFactory.create(syncDir, absolutePath)).thenReturn(syncPath);
        when(absolutePath.getFileSystem()).thenReturn(fs);
        when(fs.provider()).thenReturn(provider);
        when(provider.newFileChannel(same(absolutePath), argThat(s -> s.size() == 1 && s.contains(READ)))).thenReturn(fileChannel);
        when(channel.lock(fileChannel)).thenReturn(channel);
        regularFile = new RegularFileFactory(digestingChannelFactory,
                syncPathFactory, resourceProducer).create(syncDir, absolutePath);
    }

    @Test
    public void startDigest() throws Exception {
        assertSame(channel, regularFile.startDigest());
    }

    @Test
    public void getSyncPath() {
        assertSame(syncPath, regularFile.getSyncPath());
    }

    @Test
    public void update() throws Exception {
        regularFile.update(observer);
        verify(resource).update(observer);
    }
}
