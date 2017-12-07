package ch.sourcepond.io.fssync.impl.fswatch;

import ch.sourcepond.io.checksum.api.ResourceProducer;
import ch.sourcepond.io.fssync.api.FileSystemSync;
import ch.sourcepond.io.fssync.api.FileSystemSyncFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;

import static java.nio.file.Files.walkFileTree;

public class FileSystemSyncFactoryImpl implements FileSystemSyncFactory {
    private final ResourceProducer resourceProducer;
    private final ReplicationTrigger trigger;

    public FileSystemSyncFactoryImpl(ResourceProducer resourceProducer, ReplicationTrigger trigger) {
        this.resourceProducer = resourceProducer;
        this.trigger = trigger;
    }

    @Override
    public FileSystemSync create(final Path pDirectory) throws IOException {
        final WatchService ws = pDirectory.getFileSystem().newWatchService();
        final WatchServiceInstaller wsi = new WatchServiceInstaller(resourceProducer, ws, trigger);
        walkFileTree(pDirectory, wsi);

        return null;
    }
}
