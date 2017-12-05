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
package ch.sourcepond.io.fssync.impl.fswatch;

import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.ResourceProducer;
import ch.sourcepond.io.checksum.api.Update;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static java.lang.Thread.currentThread;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.slf4j.LoggerFactory.getLogger;

public class WatchServiceInstaller extends SimpleFileVisitor<Path> implements Runnable {
    private static final Logger LOG = getLogger(WatchServiceInstaller.class);
    private final ConcurrentMap<Path, Object> tree = new ConcurrentHashMap<>();
    private final ResourceProducer resourceProducer;
    private final WatchService watchService;
    private final ReplicationTrigger trigger;

    WatchServiceInstaller(final ResourceProducer pResourceProducer,
                          final WatchService pWatchService,
                          final ReplicationTrigger pTrigger) {
        resourceProducer = pResourceProducer;
        watchService = pWatchService;
        trigger = pTrigger;
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
        getResource(file);
        return CONTINUE;
    }

    private void updateResource(final Update pUpdate, final Path pFile) {
        if (pUpdate.hasChanged()) {
            try {
                trigger.modify(pFile, pUpdate.getCurrent().toByteArray());
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private Resource computeResource(final Path pFile) {
        final Resource resource = resourceProducer.create(SHA256, pFile);
        try {
            resource.update(update -> updateResource(update, pFile));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return resource;
    }

    private Resource getResource(final Path pFile) throws IOException {
        try {
            return (Resource) tree.computeIfAbsent(pFile, this::computeResource);
        } catch (final UncheckedIOException e) {
            throw new IOException(e);
        }
    }

    private void delete(final Path pPath) throws IOException {
        final Object obj = tree.remove(pPath);
        if (isDirectory(pPath)) {
            final Directory dirOrNull = (Directory) obj;
            if (dirOrNull != null) {
                dirOrNull.close();
            }
        } else if (isRegularFile(pPath) && obj != null) {
            trigger.delete(pPath);
        }
    }

    private void registerDirectory(final Path pPath) throws IOException {
        tree.computeIfAbsent(pPath, d -> {
            final Path parentPathOrNull = pPath.getParent();
            final Directory parentOrNull = parentPathOrNull == null ? null : (Directory) tree.get(parentPathOrNull);
            try {
                return new Directory(parentOrNull, pPath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void create(final Path pPath) throws IOException {
        if (isDirectory(pPath)) {
            registerDirectory(pPath);
        } else {
            modify(pPath);
        }
    }

    private void modify(final Path pPath) throws IOException {
        if (isRegularFile(pPath)) {
            getResource(pPath).update(update -> updateResource(update, pPath));
        }
    }

    private void processEvents(final WatchKey pWatchKey, final Path pDir) {
        for (final WatchEvent<?> event : pWatchKey.pollEvents()) {

            // Ignore overflow
            if (OVERFLOW == event.kind()) {
                continue;
            }

            final Path path = pDir.resolve((Path) event.context());

            try {
                if (ENTRY_DELETE == event.kind()) {
                    delete(path);
                } else if (ENTRY_CREATE == event.kind()) {
                    create(path);
                } else { // ENTRY_MODIFY
                    modify(path);
                }
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    public void run() {
        try {
            while (!currentThread().isInterrupted()) {
                final WatchKey watchKey = watchService.take();
                processEvents(watchKey, (Path) watchKey.watchable());
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
        }
    }
}
