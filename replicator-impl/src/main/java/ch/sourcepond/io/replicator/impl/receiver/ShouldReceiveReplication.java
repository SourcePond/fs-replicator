package ch.sourcepond.io.replicator.impl.receiver;

import ch.sourcepond.io.distributor.api.Distributor;
import ch.sourcepond.io.distributor.spi.LockException;
import ch.sourcepond.io.distributor.spi.Receiver;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.slf4j.LoggerFactory.getLogger;

public class ShouldReceiveReplication implements Receiver {

    private static final Store EMPTY_STORE = new Store() {
        @Override
        public void store(byte[] pData) throws IOException {
            // noop
        }

        @Override
        public void unlockLocally() {
            // noop
        }

        @Override
        public void close() throws IOException {
            // noop
        }
    };

    /**
     *
     */
    private class StoreImpl implements Store {
        private final ByteBuffer buffer = allocate(1024);
        private final FileChannel channel;
        private final String path;

        public StoreImpl(final String pPath, final FileChannel pChannel) {
            path = pPath;
            channel = pChannel;
        }

        @Override
        public void store(final byte[] pData) throws IOException {


        }

        @Override
        public void close() throws IOException {
            try {
                channel.close();
            } finally {
                stores.remove(path);
            }
        }
    }

    private static final Logger LOG = getLogger(ShouldReceiveReplication.class);
    private final Map<String, Store> stores = new HashMap<>();
    private final Distributor distributor;
    private final FileSystem fileSystem;

    public ShouldReceiveReplication(final Distributor pDistributor, final FileSystem pFileSystem) {
        distributor = pDistributor;
        fileSystem = pFileSystem;
    }

    public Path toPath(final String pPath) {
        return fileSystem.getPath(pPath);
    }

    public void lockLocally(final String pPath) throws IOException, LockException {
        synchronized (stores) {
            if (stores.containsKey(pPath)) {
                throw new LockException(format("%s is already locked!", pPath));
            }
            final FileChannel ch = open(toPath(pPath), CREATE, TRUNCATE_EXISTING);
            ch.lock();
            final StoreImpl store = new StoreImpl(pPath, ch);
            stores.put(pPath, store);
        }
    }

    public void unlockLocally(final String pPath) throws IOException, LockException {
        synchronized (stores) {
            final Store store = stores.
        }
    }

    @Override
    public Store getStore(final String pSendingNode, final String pPath) throws IOException {
        if (pSendingNode.equals(distributor.getLocalNode())) {
            return EMPTY_STORE;
        }

        return stores.computeIfAbsent(pPath, p -> {
            try {
                final FileChannel ch = open(toPath(p), CREATE, TRUNCATE_EXISTING);
                ch.lock();
                return new StoreImpl(p, ch);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
