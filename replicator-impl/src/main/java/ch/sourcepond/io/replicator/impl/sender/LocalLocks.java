package ch.sourcepond.io.replicator.impl.sender;

import ch.sourcepond.io.distributor.spi.LockException;
import ch.sourcepond.io.replicator.impl.receiver.ShouldReceiveReplication;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.slf4j.LoggerFactory.getLogger;

public class LocalLocks {
    private static final Logger LOG = getLogger(ShouldReceiveReplication.class);
    private final Map<String, FileChannel> locks = new HashMap<>();
    private final FileSystem fileSystem;

    public LocalLocks(final FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public Path toPath(final String pPath) {
        return fileSystem.getPath(pPath);
    }

    public void lockLocally(final String pPath) throws LockException {
        synchronized (locks) {
            if (locks.containsKey(pPath)) {
                throw new LockException(format("Local file-lock for path %s is alreay held", pPath));
            }
            try {
                final Path path = toPath(pPath);
                createDirectories(path.getParent());

                final FileChannel ch = open(path, CREATE, TRUNCATE_EXISTING);
                ch.lock();
                locks.put(pPath, ch);
            } catch (final Exception e) {
                throw new LockException(format("Lock could not be acquired for path %s"), e);
            }
        }
    }

    public FileChannel getChannel(final String pPath) throws IOException {
        final FileChannel ch;
        synchronized (locks) {
            ch = locks.get(pPath);
        }
        if (ch == null) {
            throw new IOException(format("No open channel found for path %s", pPath));
        }
        return ch;
    }

    public void unlockLocally(final String pPath) {
        synchronized (locks) {
            final FileChannel lock = locks.remove(pPath);
            if (lock == null) {
                LOG.warn("No active lockLocally found for path {}", pPath);
            } else {
                try {
                    lock.close();
                } catch (final IOException e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }
    }
}
