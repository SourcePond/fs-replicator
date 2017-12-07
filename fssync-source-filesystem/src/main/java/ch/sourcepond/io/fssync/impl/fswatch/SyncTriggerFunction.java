package ch.sourcepond.io.fssync.impl.fswatch;

import java.io.IOException;

@FunctionalInterface
public interface SyncTriggerFunction {

    void process(String pSyncDir, String pFile) throws IOException;
}
