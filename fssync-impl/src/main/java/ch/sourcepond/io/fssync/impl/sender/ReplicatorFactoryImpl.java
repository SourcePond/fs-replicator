package ch.sourcepond.io.fssync.impl.sender;

import ch.sourcepond.io.checksum.api.ResourceProducer;
import ch.sourcepond.io.fssync.api.Replicator;
import ch.sourcepond.io.fssync.api.ReplicatorFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;

import static java.nio.file.Files.walkFileTree;

public class ReplicatorFactoryImpl implements ReplicatorFactory {
    private final ResourceProducer resourceProducer;
    private final ReplicationTrigger trigger;

    public ReplicatorFactoryImpl(ResourceProducer resourceProducer, ReplicationTrigger trigger) {
        this.resourceProducer = resourceProducer;
        this.trigger = trigger;
    }

    @Override
    public Replicator create(final Path pDirectory) throws IOException {
        final WatchService ws = pDirectory.getFileSystem().newWatchService();
        final WatchServiceInstaller wsi = new WatchServiceInstaller(resourceProducer, ws, trigger);
        walkFileTree(pDirectory, wsi);

        return null;
    }
}
