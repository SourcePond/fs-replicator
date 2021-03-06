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

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.target.api.NodeInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.walkFileTree;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TargetDirectoryTest {
    private static final String EXPECTED_CONTEXT = "Some expected content";
    private final NodeInfo nodeInfo = mock(NodeInfo.class);
    private final SyncPath syncPath = new SyncPath(File.separator, format("%s/build/testsync", getProperty("user.dir")), "org/foo/bar.txt");
    private final Config config = mock(Config.class);
    private final Path expectedPath = getDefault().getPath(syncPath.getSyncDir(), syncPath.getRelativePath());
    private TargetDirectory syncTarget;

    @Before
    public void setup() throws IOException {
        when(config.syncDir()).thenReturn(syncPath.toAbsolutePath());
        when(config.forceUnlockSchedulePeriod()).thenReturn(100L);
        when(config.forceUnlockSchedulePeriodUnit()).thenReturn(MILLISECONDS);
        syncTarget = new TargetDirectory();
        syncTarget.activate(config);
        syncTarget.lock(nodeInfo, syncPath);
    }

    @After
    public void tearDown() throws Exception {
        try {
            syncTarget.deactivate();
        } finally {
            walkFileTree(getDefault().getPath(syncPath.getSyncDir()), new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    deleteIfExists(file);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    delete(dir);
                    return CONTINUE;
                }
            });
        }
    }

    @Test
    public void verifyLock() {
        assertTrue(isRegularFile(expectedPath));
    }

    @Test
    public void fileAlreadyLocked() throws IOException {
        try {
            syncTarget.lock(nodeInfo, syncPath);
            fail("Exception expected");
        } catch (final IOException expected) {
            assertTrue(expected.getMessage().contains(syncPath.getSyncDir()));
            assertTrue(expected.getMessage().contains(syncPath.getRelativePath()));
        }
    }

    @Test
    public void unlock() throws IOException {
        syncTarget.unlock(nodeInfo, syncPath);
        syncTarget.lock(nodeInfo, syncPath);
        syncTarget.unlock(nodeInfo, syncPath);
        syncTarget.lock(nodeInfo, syncPath);
    }

    @Test
    public void verifyDelete() throws IOException {
        assertTrue(Files.exists(expectedPath));
        syncTarget.delete(nodeInfo, syncPath);
        assertFalse(Files.exists(expectedPath));
    }

    @Test
    public void transfer() throws IOException {
        syncTarget.transfer(nodeInfo, syncPath, ByteBuffer.wrap(format("%s\n", EXPECTED_CONTEXT).getBytes()));
        syncTarget.transfer(nodeInfo, syncPath, ByteBuffer.wrap(format("%s\n", EXPECTED_CONTEXT).getBytes()));
        final List<String> lines = Files.readAllLines(expectedPath);
        assertEquals(2, lines.size());
        assertEquals(EXPECTED_CONTEXT, lines.get(0));
        assertEquals(EXPECTED_CONTEXT, lines.get(1));
    }

    @Test
    public void store() throws IOException {
        syncTarget.store(nodeInfo, syncPath);

        // Handle should have been closed, so locking should be allowed again
        syncTarget.lock(nodeInfo, syncPath);
    }

    @Test
    public void discard() throws IOException {
        // Only a log message should be printed
        syncTarget.discard(nodeInfo, syncPath, new IOException());
        assertTrue(Files.exists(expectedPath));
    }

    @Test(timeout = 3000)
    public void forceUnlock() throws Exception {
        when(config.forceUnlockTimeout()).thenReturn(1L);
        when(config.forceUnlockTimoutUnit()).thenReturn(SECONDS);

        sleep(1500);

        // Lock should be possible now
        syncTarget.lock(nodeInfo, syncPath);
    }

    @Test
    public void cancel() throws Exception {
        syncTarget.cancel(nodeInfo);

        // Handle should have been closed, so locking should be allowed again
        syncTarget.lock(nodeInfo, syncPath);
    }

    @Test
    public void ignoreLockOnLocalNodeAndSameTarget() throws Exception {
        when(nodeInfo.isLocalNode()).thenReturn(true);
        syncTarget.lock(nodeInfo, syncPath);

        // Nothing should have been happened, so calling this method again has no effect
        syncTarget.lock(nodeInfo, syncPath);
    }

    @Test
    public void ignoreUnlockOnLocalNodeAndSameTarget() throws Exception {
        when(nodeInfo.isLocalNode()).thenReturn(true);
        syncTarget.unlock(nodeInfo, syncPath);
        when(nodeInfo.isLocalNode()).thenReturn(false);
        try {
            syncTarget.lock(nodeInfo, syncPath);
            fail("Exception expected");
        } catch (final IOException e) {
            // expected
        }
    }

    @Test
    public void ignoreDeleteOnLocalNodeAndSameTarget() throws Exception {
        when(nodeInfo.isLocalNode()).thenReturn(true);
        syncTarget.delete(nodeInfo, syncPath);
        assertTrue(exists(expectedPath));
    }

    @Test
    public void ignoreStoreOnLocalNodeAndSameTarget() throws Exception {
        when(nodeInfo.isLocalNode()).thenReturn(true);
        syncTarget.store(nodeInfo, syncPath);
        when(nodeInfo.isLocalNode()).thenReturn(false);
        try {
            syncTarget.lock(nodeInfo, syncPath);
            fail("Exception expected");
        } catch (final IOException e) {
            // expected
        }
    }

    @Test
    public void deleteWithoutLock() throws Exception {
        syncTarget.unlock(nodeInfo, syncPath);
        try {
            syncTarget.delete(nodeInfo, syncPath);
            fail("Exception expected");
        } catch (final IOException expected) {
            // noop
        }
    }

    @Test
    public void transferWithoutLock() throws Exception {
        syncTarget.unlock(nodeInfo, syncPath);
        try {
            syncTarget.transfer(nodeInfo, syncPath, ByteBuffer.wrap(EXPECTED_CONTEXT.getBytes()));
            fail("Exception expected");
        } catch (final IOException expected) {
            // noop
        }
    }

    @Test
    public void discardWithoutLock() throws Exception {
        syncTarget.unlock(nodeInfo, syncPath);
        try {
            syncTarget.discard(nodeInfo, syncPath, new IOException());
            fail("Exception expected");
        } catch (final IOException expected) {
            // noop
        }
    }

    @Test
    public void storeWithoutLock() throws Exception {
        syncTarget.unlock(nodeInfo, syncPath);
        try {
            syncTarget.store(nodeInfo, syncPath);
            fail("Exception expected");
        } catch (final IOException expected) {
            // noop
        }
    }
}
