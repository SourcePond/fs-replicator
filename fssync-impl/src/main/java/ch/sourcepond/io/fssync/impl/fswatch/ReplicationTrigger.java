/*Copyright (C) 2017 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.fssync.impl.fswatch;

import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.distributor.api.LockException;
import ch.sourcepond.io.fssync.distributor.api.UnlockException;
import ch.sourcepond.io.fssync.impl.Constants;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static ch.sourcepond.io.fssync.impl.Constants.TMP_FILE_PREFIX;
import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.StandardOpenOption.READ;
import static org.slf4j.LoggerFactory.getLogger;

class ReplicationTrigger {
    private static final Logger LOG = getLogger(ReplicationTrigger.class);
    private final Distributor distributor;

    public ReplicationTrigger(final Distributor pDistributor) {
        distributor = pDistributor;
    }

    private void transfer(final Path pSource) throws IOException {
        final String path = pSource.toString();
        try (final ReadableByteChannel source = open(pSource, READ)) {
            final ByteBuffer buffer = allocate(1024);
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try {
                while (source.read(buffer) != -1) {
                    buffer.rewind();
                    distributor.transfer(path, buffer);
                    buffer.rewind();
                    digest.update(buffer);
                    buffer.rewind();
                }
                distributor.store(path, digest.digest());
            } catch (final IOException e) {
                distributor.discard(path, e);
            }
        } catch (final NoSuchAlgorithmException e) {
            // This should never happen
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void lock(final Path pPath) throws LockException {
        distributor.lock(pPath.toString());
    }

    private void unlock(final Path pPath) throws UnlockException {
        distributor.unlock(pPath.toString());
    }

    private boolean isRegularFile(final Path pFile) {
        return !pFile.getFileName().startsWith(TMP_FILE_PREFIX);
    }

    void delete(final Path pFile) throws IOException {
        if (isRegularFile(pFile)) {
            final String path = pFile.toString();
            lock(pFile);
            try {
                distributor.delete(path);
            } finally {
                unlock(pFile);
            }
        }
    }

    void modify(final Path pSource, final byte[] pChecksum) throws IOException {
        if (isRegularFile(pSource)) {
            final String target = pSource.toString();
            if (!Arrays.equals(distributor.getChecksum(target), pChecksum)) {
                lock(pSource);
                try {
                    transfer(pSource);
                } finally {
                    unlock(pSource);
                }
            }
        }
    }
}
