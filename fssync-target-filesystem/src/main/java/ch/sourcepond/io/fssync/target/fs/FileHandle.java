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
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static java.lang.String.format;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

class FileHandle implements Closeable {
    private static final Logger LOG = getLogger(FileHandle.class);
    private final NodeInfo nodeInfo;
    private final Path targetFile;
    private final Path tmpFile;
    private final FileChannel channel;
    private final Instant openSince;


    public FileHandle(final NodeInfo pNodeInfo,
                      final FileChannel pChannel,
                      final Path pTargetFile,
                      final Path pTmpFile) {
        nodeInfo = pNodeInfo;
        channel = pChannel;
        targetFile = pTargetFile;
        tmpFile = pTmpFile;
        openSince = now();
    }

    public boolean isExpired(final Config pConfig, final Instant pThreshold) {
        Instant o = openSince.plusMillis(pConfig.forceUnlockTimoutUnit().toMillis(pConfig.forceUnlockTimeout()));
        return pThreshold.isAfter(o);
    }

    public void delete() throws IOException {
        Files.delete(targetFile);
    }

    public void transfer(final ByteBuffer pBuffer) throws IOException {
        try (final FileChannel tmpChannel = open(tmpFile, CREATE, WRITE, APPEND)) {
            tmpChannel.write(pBuffer);
        }
    }

    public void store() throws IOException {
        createDirectories(targetFile.getParent());
        try {
            move(tmpFile, targetFile, ATOMIC_MOVE);
        } catch (final AtomicMoveNotSupportedException e) {
            move(tmpFile, targetFile, REPLACE_EXISTING);
        }
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        } finally {
            try {
                deleteIfExists(tmpFile);
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    public String toString() {
        return format("%s (tmpfile: %s)", targetFile, tmpFile);
    }
}
