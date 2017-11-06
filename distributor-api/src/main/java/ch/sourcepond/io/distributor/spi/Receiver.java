package ch.sourcepond.io.distributor.spi;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver {

    interface Store extends Closeable {

        void store(byte[] pData) throws IOException;
    }

    void lockLocally(String pPath) throws IOException, LockException;

    void unlockLocally(String pPath);

    Store getStore(String pSendingNode, String pPath) throws IOException;
}
