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
package ch.sourcepond.io.fssync.target.fs;

import ch.sourcepond.io.fssync.target.api.NodeInfo;
import ch.sourcepond.io.fssync.target.api.SyncPath;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static java.lang.String.format;
import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

class FileHandle implements Closeable {
    private static final Logger LOG = getLogger(FileHandle.class);
    private final TargetDirectory syncTarget;
    private final NodeInfo nodeInfo;
    private final SyncPath syncPath;
    private final Path targetFile;
    private final FileChannel channel;
    private final Instant openSince;
    private boolean closed;

    public FileHandle(final TargetDirectory pSyncTarget,
                      final NodeInfo pNodeInfo,
                      final SyncPath pSyncPath,
                      final FileChannel pChannel,
                      final Path pTargetFile) {
        syncTarget = pSyncTarget;
        nodeInfo = pNodeInfo;
        syncPath = pSyncPath;
        channel = pChannel;
        targetFile = pTargetFile;
        openSince = now();
    }

    private void checkOpen() throws IOException {
        if (closed) {
            throw new IOException(format("File-handle for %s has been closed", targetFile));
        }
    }

    public boolean closeExpired(final SyncTargetConfig pSyncTargetConfig) {
        final Instant o = openSince.plusMillis(pSyncTargetConfig.forceUnlockTimoutUnit().toMillis(
                pSyncTargetConfig.forceUnlockTimeout()));
        final boolean expired = now().isAfter(o);
        if (expired) {
            close();
        }
        return expired;
    }

    public synchronized void delete() throws IOException {
        checkOpen();
        try {
            Files.delete(targetFile);
        } finally {
            channel.close();
        }
    }

    public synchronized void transfer(final ByteBuffer pBuffer) throws IOException {
        checkOpen();
        channel.write(pBuffer);
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            try {
                channel.close();
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            } finally {
                syncTarget.remove(syncPath);
            }
        }
    }

    @Override
    public String toString() {
        return format("%s [%s]", getClass().getSimpleName(), targetFile);
    }
}
