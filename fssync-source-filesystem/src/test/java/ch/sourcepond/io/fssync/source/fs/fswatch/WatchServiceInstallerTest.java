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
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.ResourceProducer;
import ch.sourcepond.io.checksum.api.Update;
import ch.sourcepond.io.checksum.api.UpdateObserver;
import ch.sourcepond.io.fssync.source.fs.trigger.ReplicationTrigger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static java.lang.System.getProperty;
import static java.lang.Thread.sleep;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class WatchServiceInstallerTest {
    private static final byte[] EXPECTED_CHECKSUM = new byte[0];
    private final FileSystem fs = getDefault();
    private final Path rootPath = fs.getPath(getProperty("user.dir"));
    private final Path sourcePath = rootPath.resolve("src").resolve("test").resolve("resources").resolve(getClass().getSimpleName());
    private final Path targetPath = rootPath.resolve("build").resolve(getClass().getSimpleName());
    private final Path watchedDirectory = targetPath.resolve(randomUUID().toString());
    private final Path expectedPath = watchedDirectory.resolve("test.txt");
    private final Update update = mock(Update.class);
    private final Checksum checksum = mock(Checksum.class);
    private final ResourceProducer resourceProducer = mock(ResourceProducer.class);
    private final Resource resource = mock(Resource.class, withSettings().defaultAnswer(inv -> {
        final Object[] args = inv.getArguments();
        ((UpdateObserver) args[args.length - 1]).done(update);
        return null;
    }));
    private final ReplicationTrigger replicationTrigger = mock(ReplicationTrigger.class);
    private WatchEventDistributor watchEventDistributor;
    private WatchService watchService;
    private WatchServiceInstaller installer;

    @Before
    public void setup() throws Exception {
        assertTrue(isDirectory(rootPath));
        assertTrue(isDirectory(sourcePath));
        copyDirectory(sourcePath.toFile(), watchedDirectory.toFile());
        assertTrue(exists(expectedPath));
        when(resourceProducer.create(SHA256, expectedPath)).thenReturn(resource);
        when(update.getCurrent()).thenReturn(checksum);
        when(update.hasChanged()).thenReturn(true);
        when(checksum.toByteArray()).thenReturn(EXPECTED_CHECKSUM);
        watchService = getDefault().newWatchService();
        watchEventDistributor = new WatchEventDistributor(resourceProducer, watchService, replicationTrigger, watchedDirectory);
        installer = new WatchServiceInstaller(watchEventDistributor, watchService, watchedDirectory);
        installer.start();
        sleep(1000);
    }

    @After
    public void tearDown() throws Exception {
        deleteDirectory(targetPath.toFile());
        installer.close();
    }

    @Test
    public void startFailed() throws IOException {
        installer.close();
        deleteDirectory(targetPath.toFile());
        installer = new WatchServiceInstaller(watchEventDistributor, watchService, watchedDirectory);

        // This should not throw an exception
        installer.start();
    }

    private void changeFile() throws Exception {
        try (final Writer writer = newBufferedWriter(expectedPath)) {
            writer.write(randomUUID().toString());
        }
        verify(replicationTrigger, timeout(15000).atLeastOnce()).
                modify(watchedDirectory, expectedPath, EXPECTED_CHECKSUM);
    }

    @Test
    public void modify() throws Exception {
        reset(replicationTrigger);
        changeFile();
    }

    @Test
    public void deleteCreate() throws Exception {
        reset(replicationTrigger);
        delete(expectedPath);
        verify(replicationTrigger, timeout(15000)).delete(watchedDirectory, expectedPath);
        changeFile();
    }

    @Test
    public void close() throws Exception {
        installer.close();

        try {
            watchService.poll();
            fail("Exception expected here");
        } catch (final ClosedWatchServiceException e) {
            // expected
        }
    }
}
