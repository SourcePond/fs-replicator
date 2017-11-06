package ch.sourcepond.io.distributor.api;

import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;

public interface Distributor {

    String getLocalNode();

    /**
     * Deletes the path specified cluster wide.
     *
     * @param pPath
     */
    void delete(String pPath);

    /**
     * Opens an output stream which can be used to write the modified data into.
     * This data will then be replicated into the cluster. During the time the
     * stream is open, the path specified is locked throughout the cluster, i.e.
     * the best effort is made that no other process on any node can change
     * the currently replicated file.
     *
     * @param pPath
     * @return
     */
    WritableByteChannel openChannel(String pPath);

    void lockGlobally(String pPath, TimeUnit pTimeoutUnit, long pTimeout) throws GlobalLockException;

    void unlockGlobally(String pPath);

    byte[] getGlobalChecksum(String pFile);
}
