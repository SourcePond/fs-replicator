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
package ch.sourcepond.io.fssync.source.fs.trigger;

import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.source.fs.Config;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

import static java.nio.ByteBuffer.allocate;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.StandardOpenOption.READ;
import static org.slf4j.LoggerFactory.getLogger;

public class ReplicationTrigger {
    private static final Logger LOG = getLogger(ReplicationTrigger.class);
    private final Distributor distributor;
    private final SyncTriggerFactory syncTriggerFactory;
    private final ScheduledExecutorService executor;
    private final MessageDigestFactory messageDigestFactory;
    private final Config config;

    @Inject
    public ReplicationTrigger(final Distributor pDistributor,
                              final SyncTriggerFactory pSyncTriggerFactory,
                              final ScheduledExecutorService pExecutor,
                              final MessageDigestFactory pMessageDigesterFactory,
                              final Config pConfig) {
        distributor = pDistributor;
        syncTriggerFactory = pSyncTriggerFactory;
        executor = pExecutor;
        messageDigestFactory = pMessageDigesterFactory;
        config = pConfig;
    }

    private void transfer(final SyncPath pPath) throws IOException {
        try (final ReadableByteChannel source = open(pPath.getPath(), READ)) {
            final ByteBuffer buffer = allocate(config.readBufferSize());
            final MessageDigest digest = messageDigestFactory.create();
            try {
                while (source.read(buffer) != -1) {
                    buffer.rewind();
                    distributor.transfer(pPath.getSyncDirAsString(), pPath.getPathAsString(), buffer);
                    buffer.rewind();
                    digest.update(buffer);
                    buffer.rewind();
                }
                distributor.store(pPath.getSyncDirAsString(), pPath.getPathAsString(), digest.digest());
            } catch (final IOException e) {
                distributor.discard(pPath.getSyncDirAsString(), pPath.getPathAsString(), e);
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    public void delete(final Path pSyncDir, final Path pFile) {
        executor.execute(syncTriggerFactory.create(new SyncPath(pSyncDir, pFile), p ->
                distributor.delete(p.getSyncDirAsString(), p.getPathAsString())));
    }

    public void modify(final Path pSyncDir, final Path pFile, final byte[] pChecksum) {
        final SyncPath path = new SyncPath(pSyncDir, pFile);
        if (!Arrays.equals(distributor.getChecksum(path.getSyncDirAsString(), path.getPathAsString()), pChecksum)) {
            executor.execute(syncTriggerFactory.create(path, this::transfer));
        }
    }
}
