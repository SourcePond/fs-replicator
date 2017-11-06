package ch.sourcepond.io.fsreplicator.api;

import java.io.IOException;
import java.nio.file.Path;

public interface ReplicatorFactory {

    Replicator create(Path pDirectory) throws IOException;
}
