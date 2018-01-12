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

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.common.api.SyncPathFactory;
import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.source.fs.Config;
import ch.sourcepond.io.fssync.source.fs.fswatch.DigestingChannel;
import ch.sourcepond.io.fssync.source.fs.fswatch.RegularFile;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

import static java.nio.ByteBuffer.allocate;
import static org.slf4j.LoggerFactory.getLogger;

public class ReplicationTrigger {
    private static final Logger LOG = getLogger(ReplicationTrigger.class);
    private final Distributor distributor;
    private final SyncTriggerFactory syncTriggerFactory;
    private final ScheduledExecutorService executor;
    private final FileSystem fs;
    private final SyncPathFactory syncPathFactory;
    private final Config config;

    @Inject
    public ReplicationTrigger(final Distributor pDistributor,
                              final SyncTriggerFactory pSyncTriggerFactory,
                              final ScheduledExecutorService pExecutor,
                              final FileSystem pFs,
                              final SyncPathFactory pSyncPathFactory,
                              final Config pConfig) {
        distributor = pDistributor;
        syncTriggerFactory = pSyncTriggerFactory;
        executor = pExecutor;
        fs = pFs;
        syncPathFactory = pSyncPathFactory;
        config = pConfig;
    }

    private void transfer(final RegularFile pPath) throws IOException {
        try (final DigestingChannel source = pPath.startDigest()) {
            final SyncPath syncPath = pPath.getSyncPath();
            final ByteBuffer buffer = allocate(config.readBufferSize());
            try {
                while (source.read(buffer) != -1) {
                    distributor.transfer(syncPath, buffer);
                    buffer.rewind();
                }
                distributor.store(syncPath, source.digest());
            } catch (final IOException e) {
                distributor.discard(syncPath, e);
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    public void delete(final RegularFile pFile) {
        executor.execute(syncTriggerFactory.create(pFile, p -> distributor.delete(p.getSyncPath())));
    }

    public void modify(final RegularFile pFile, final byte[] pChecksum) {
        final SyncPath syncPath = pFile.getSyncPath();
        if (!Arrays.equals(distributor.getChecksum(syncPath), pChecksum)) {
            executor.execute(syncTriggerFactory.create(pFile, this::transfer));
        }
    }
}
