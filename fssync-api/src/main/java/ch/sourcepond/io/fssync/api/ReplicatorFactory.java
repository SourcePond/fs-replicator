package ch.sourcepond.io.fssync.api;

import java.io.IOException;
import java.nio.file.Path;

public interface ReplicatorFactory {

    Replicator create(Path pDirectory) throws IOException;
}
