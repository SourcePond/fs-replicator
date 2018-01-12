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

import ch.sourcepond.io.checksum.api.Update;
import ch.sourcepond.io.fssync.source.fs.trigger.ReplicationTrigger;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.slf4j.LoggerFactory.getLogger;

class WatchEventDistributor extends SimpleFileVisitor<Path> implements Closeable {
    private static final Logger LOG = getLogger(WatchEventDistributor.class);
    private final ConcurrentMap<Path, Object> tree = new ConcurrentHashMap<>();
    private final RegularFileFactory regularFileFactory;
    private final WatchService watchService;
    private final ReplicationTrigger trigger;
    private final Path syncDir;

    @Inject
    WatchEventDistributor(final RegularFileFactory pRegularFileFactory,
                          final WatchService pWatchService,
                          final ReplicationTrigger pTrigger,
                          final Path pSyncDir) {
        regularFileFactory = pRegularFileFactory;
        watchService = pWatchService;
        trigger = pTrigger;
        syncDir = pSyncDir;
    }

    @Override
    public void close() {
        tree.values().forEach(c -> {
            if (c instanceof WatchKey) {
                ((WatchKey) c).cancel();
            }
        });
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        try {
            registerDirectory(dir);
        } catch (final UncheckedIOException e) {
            throw new IOException(e);
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        getRegularFile(file).update(update -> updateResource(update, file));
        return CONTINUE;
    }

    private void updateResource(final Update pUpdate, final Path pFile) {
        if (pUpdate.hasChanged()) {
            final Object regularFile = tree.get(pFile);
            if (regularFile instanceof RegularFile) {
                trigger.modify((RegularFile) regularFile, pUpdate.getCurrent().toByteArray());
            } else {
                LOG.warn("{} is not a regular file, update cannot be performed!", regularFile);
            }
        }
    }

    private RegularFile computeRegularFile(final Path pFile) {
        return regularFileFactory.create(syncDir, pFile);
    }

    private RegularFile getRegularFile(final Path pFile) {
        return (RegularFile) tree.computeIfAbsent(pFile, this::computeRegularFile);
    }

    private void registerDirectory(final Path pPath) {
        tree.computeIfAbsent(pPath, d -> {
            try {
                return pPath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void delete(final Path pPath) {
        final Object obj = tree.remove(pPath);
        if (obj instanceof WatchKey) {  // Was a directory
            ((WatchKey) obj).cancel();
        } else if (obj instanceof RegularFile) { // Was a regular file
            trigger.delete((RegularFile) obj);
        } else {
            LOG.warn("No watch-key nor a resource registered for {}", pPath);
        }
    }

    public void create(final Path pPath) throws IOException {
        if (isDirectory(pPath)) {
            registerDirectory(pPath);
        } else {
            modify(pPath);
        }
    }

    public void modify(final Path pPath) throws IOException {
        if (isRegularFile(pPath)) {
            getRegularFile(pPath).update(update -> updateResource(update, pPath));
        }
    }
}
