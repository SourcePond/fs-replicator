package ch.sourcepond.io.fssync.source.fs.fswatch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static java.security.MessageDigest.getInstance;

public class DigestingChannel implements ReadableByteChannel {
    private final Lock lock;
    private final MessageDigest digest;
    private ReadableByteChannel channel;

    DigestingChannel() {
        this(new ReentrantLock());
    }

    // Constructor for testing
    DigestingChannel(final Lock pLock) {
        lock = pLock;
        try {
            digest = getInstance(SHA256.toString());
        } catch (final NoSuchAlgorithmException e) {
            // This should never happen
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    DigestingChannel lock(final ReadableByteChannel pChannel) {
        lock.lock();
        channel = pChannel;
        return this;
    }

    @Override
    public void close() throws IOException {
        try {
            channel.close();
        } finally {
            lock.unlock();
        }
    }

    public byte[] digest() {
        return digest.digest();
    }

    @Override
    public int read(final ByteBuffer dst) throws IOException {
        final int numBytes = channel.read(dst);
        dst.flip();
        digest.update(dst);
        return numBytes;
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }
}
