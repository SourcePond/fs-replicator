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
package ch.sourcepond.io.fssync.source.fs.fswatch;

import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.Files.walkFileTree;
import static org.slf4j.LoggerFactory.getLogger;

public class WatchServiceInstaller extends SimpleFileVisitor<Path> implements Runnable, Closeable {
    private static final Logger LOG = getLogger(WatchServiceInstaller.class);
    private final Thread thread;
    private final WatchEventDistributor watchEventDistributor;
    private final WatchService watchService;
    private final Path watchedDirectory;

    @Inject
    WatchServiceInstaller(final WatchEventDistributor pWatchEventDistributor,
                          final WatchService pWatchService,
                          final Path pWatchDirectory) {
        watchEventDistributor = pWatchEventDistributor;
        watchService = pWatchService;
        watchedDirectory = pWatchDirectory;
        thread = new Thread(this, format("%s: %s", getClass().getSimpleName(), pWatchDirectory));
    }

    @Override
    public void close() throws IOException {
        thread.interrupt();
        watchService.close();
    }

    public void start() {
        try {
            walkFileTree(watchedDirectory, watchEventDistributor);
            thread.start();
        } catch (final IOException e) {
            // TODO: Use translated message
            LOG.error("Watcher thread could not be started!", e);
        }
    }

    @Override
    public void run() {
        try {
            while (!currentThread().isInterrupted()) {
                final WatchKey watchKey = watchService.take();
                watchEventDistributor.processEvents(watchKey, (Path) watchKey.watchable());
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
        }
    }
}
