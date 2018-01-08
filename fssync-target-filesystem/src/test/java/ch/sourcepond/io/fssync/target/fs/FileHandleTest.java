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
package ch.sourcepond.io.fssync.target.fs;

import ch.sourcepond.io.fssync.target.api.NodeInfo;
import ch.sourcepond.io.fssync.common.api.SyncPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.nio.ByteBuffer.wrap;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.readAllLines;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class FileHandleTest {

    private static abstract class TestFileChannel extends FileChannel {

        public TestFileChannel() {
        }

        @Override
        public abstract void implCloseChannel() throws IOException;
    }

    private static final String EXPECTED_CONTENT = "some content";
    private final TargetDirectory syncTarget = mock(TargetDirectory.class);
    private final NodeInfo nodeInfo = mock(NodeInfo.class);
    private final SyncPath syncPath = mock(SyncPath.class);
    private final Path path = getDefault().getPath(format("%s/target/test.txt", getProperty("user.dir")));
    private final Config config = mock(Config.class);
    private FileChannel channel;
    private FileHandle handle;

    @Before
    public void setup() throws Exception {
        createDirectories(path.getParent());
        createFile(path);
        when(config.forceUnlockTimeout()).thenReturn(200L);
        when(config.forceUnlockTimoutUnit()).thenReturn(MILLISECONDS);
        channel = FileChannel.open(path, StandardOpenOption.WRITE);
        handle = new FileHandle(syncTarget, nodeInfo, syncPath, channel, path);
    }

    @After
    public void tearDown() throws Exception {
        try {
            channel.close();
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    public void closeNotExpired() {
        assertFalse(handle.closeExpired(config));
        assertTrue(channel.isOpen());
    }

    @Test
    public void closeExpired() throws Exception {
        sleep(1000);
        assertTrue(handle.closeExpired(config));
        assertFalse(channel.isOpen());
    }

    @Test
    public void delete() throws Exception {
        handle.delete();
        assertFalse(Files.exists(path));
        assertFalse(channel.isOpen());
    }

    @Test
    public void deleteHandleClosed() throws Exception {
        handle.close();
        try {
            handle.delete();
            fail("Exception expected");
        } catch (final IOException e) {
            assertTrue(e.getMessage().contains(path.toString()));
        }
    }

    @Test
    public void transfer() throws Exception {
        handle.transfer(wrap(EXPECTED_CONTENT.getBytes()));
        handle.close();
        assertEquals(EXPECTED_CONTENT, readAllLines(path).get(0));
    }

    @Test
    public void transferHandleClosed() throws Exception {
        handle.close();
        try {
            handle.transfer(wrap(EXPECTED_CONTENT.getBytes()));
            fail("Exception expected");
        } catch (final IOException e) {
            assertTrue(e.getMessage().contains(path.toString()));
        }
    }

    @Test
    public void getNodeInfo() {
        assertSame(nodeInfo, handle.getNodeInfo());
    }

    @Test
    public void close() {
        handle.close();

        // This should have no effect
        handle.close();

        assertFalse(channel.isOpen());
        verify(syncTarget).remove(syncPath);
    }

    @Test
    public void closeFailed() throws IOException {
        final TestFileChannel channel = mock(TestFileChannel.class, withSettings().useConstructor());
        doThrow(IOException.class).when(channel).implCloseChannel();
        final FileHandle handle = new FileHandle(syncTarget, nodeInfo, syncPath, channel, path);

        // This should not cause an exception
        handle.close();
    }

    @Test
    public void verifyToString() {
        assertTrue(handle.toString().contains(path.toString()));
    }
}
