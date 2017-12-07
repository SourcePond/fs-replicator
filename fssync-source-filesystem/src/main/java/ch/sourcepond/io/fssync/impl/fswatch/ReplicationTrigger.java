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
import ch.sourcepond.io.fssync.impl.config.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

import static ch.sourcepond.io.fssync.impl.Constants.TMP_FILE_PREFIX;
import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.StandardOpenOption.READ;
import static org.slf4j.LoggerFactory.getLogger;

class ReplicationTrigger {
    private static final Logger LOG = getLogger(ReplicationTrigger.class);
    private final Distributor distributor;
    private final ScheduledExecutorService executor;
    private final Config config;

    public ReplicationTrigger(final Distributor pDistributor,
                              final ScheduledExecutorService pExecutor,
                              final Config pConfig) {
        distributor = pDistributor;
        executor = pExecutor;
        config = pConfig;
    }

    private void transfer(final Path pSyncDir, final Path pSource) throws IOException {
        final String syncDir = pSyncDir.toString();
        final String path = pSyncDir.relativize(pSource).toString();
        try (final ReadableByteChannel source = open(pSource, READ)) {
            final ByteBuffer buffer = allocate(1024);
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try {
                while (source.read(buffer) != -1) {
                    buffer.rewind();
                    distributor.transfer(syncDir, path, buffer);
                    buffer.rewind();
                    digest.update(buffer);
                    buffer.rewind();
                }
                distributor.store(syncDir, path, digest.digest());
            } catch (final IOException e) {
                distributor.discard(syncDir, path, e);
            }
        } catch (final NoSuchAlgorithmException e) {
            // This should never happen
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private boolean isRegularFile(final Path pFile) {
        return !pFile.getFileName().startsWith(TMP_FILE_PREFIX);
    }

    public void delete(final Path pSyncDir, final Path pFile) throws IOException {
        final String syncDir = pSyncDir.toString();
        final String path = pSyncDir.relativize(pFile).toString();

        if (isRegularFile(pFile)) {
            executor.execute(new SyncTrigger(executor, distributor, config, syncDir, path, (s, p) -> {
                distributor.delete(s, p);
            }));
        }
    }

    public void modify(final Path pSyncDir, final Path pSource, final byte[] pChecksum) throws IOException {
        final String syncDir = pSyncDir.toString();
        final String path = pSyncDir.relativize(pSource).toString();

        if (isRegularFile(pSource) && !Arrays.equals(distributor.getChecksum(syncDir, path), pChecksum)) {
            executor.execute(new SyncTrigger(executor, distributor, config, syncDir, path, (s, p) -> {
                transfer(pSyncDir, pSource);
            }));
        }
    }
}
