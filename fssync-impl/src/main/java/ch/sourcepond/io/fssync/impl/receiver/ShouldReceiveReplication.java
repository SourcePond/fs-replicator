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
package ch.sourcepond.io.fssync.impl.receiver;

import ch.sourcepond.io.fssync.distributor.api.Distributor;
import ch.sourcepond.io.fssync.distributor.api.GlobalPath;
import ch.sourcepond.io.fssync.distributor.spi.Receiver;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.slf4j.LoggerFactory.getLogger;

public class ShouldReceiveReplication implements Receiver {

    private static final WritableByteChannel NOOP_CHANNEL = new WritableByteChannel() {

        @Override
        public int write(final ByteBuffer src) throws IOException {
            return 0;
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public void close() {
            // noop
        }
    };

    private static final Logger LOG = getLogger(ShouldReceiveReplication.class);
    private final Map<String, Map<String, WritableByteChannel>> channnels = new HashMap<>();
    private final Map<String, Map<String, IOException>> failures = new HashMap<>();
    private final Distributor distributor;
    private final FileSystem fileSystem;

    public ShouldReceiveReplication(final Distributor pDistributor, final FileSystem pFileSystem) {
        distributor = pDistributor;
        fileSystem = pFileSystem;
    }

    private Map<String, WritableByteChannel> getNodeChannels(final GlobalPath pPath) {
        return channnels.computeIfAbsent(pPath.getPath(), n -> new HashMap<>());
    }

    private Map<String, IOException> getNodeFailures(final GlobalPath pPath) {
        return failures.computeIfAbsent(pPath.getPath(), n -> new HashMap<>());
    }

    private boolean isRemoteNode(final GlobalPath pPath) {
        return !pPath.getSendingNode().equals(distributor.getLocalNode());
    }

    private Path toPath(final GlobalPath pPath) {
        return fileSystem.getPath(pPath.getPath());
    }

    @Override
    public void lockLocally(final GlobalPath pPath) throws IOException {
        if (isRemoteNode(pPath)) {
            synchronized (channnels) {
                if (channnels.containsKey(pPath)) {
                    throw new IOException(format("%s is already locked!", pPath));
                } else {
                    final FileChannel ch = open(toPath(pPath), CREATE, TRUNCATE_EXISTING);
                    ch.lock();
                    getNodeChannels(pPath).put(pPath.getPath(), ch);
                }
            }
        }
    }

    @Override
    public void kill(final String pSendingNode) {
        synchronized (channnels) {
            final Map<String, WritableByteChannel> storagesPerNode = channnels.remove(pSendingNode);
            if (storagesPerNode != null && !storagesPerNode.isEmpty()) {
                storagesPerNode.values().forEach(s -> close(s));
            }
        }
    }

    private static void close(final WritableByteChannel pChannel) {
        try {
            pChannel.close();
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    public void unlockLocally(final GlobalPath pPath) throws IOException {
        if (isRemoteNode(pPath)) {
            final WritableByteChannel ch;
            synchronized (channnels) {
                ch = getNodeChannels(pPath).remove(pPath.getPath());
            }
            if (ch == null) {
                LOG.warn("unlockLocally: no storage registered for {}", pPath);
            } else {
                close(ch);
            }
        }
    }

    private WritableByteChannel getChannel(final GlobalPath pPath) {
        WritableByteChannel ch = null;
        if (isRemoteNode(pPath)) {
            synchronized (channnels) {
                ch = getNodeChannels(pPath).get(pPath.getPath());
            }
            if (ch == null) {
                LOG.warn("getChannel: no storage registered for {}", pPath);
            }
        }
        return ch;
    }

    @Override
    public void delete(final GlobalPath pPath) throws IOException {
        synchronized (channnels) {
            final WritableByteChannel ch = getChannel(pPath);
            try {
                if (ch == null) {
                    LOG.warn("delete: no storage registered for {}", pPath);
                } else {
                    ch.close();
                }
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            } finally {
                getNodeChannels(pPath).put(pPath.getPath(), NOOP_CHANNEL);
                Files.delete(toPath(pPath));
            }
        }
    }

    @Override
    public void transfer(final GlobalPath pPath, final ByteBuffer pBuffer) {
        final WritableByteChannel ch = getChannel(pPath);
        synchronized (failures) {
            final String path = pPath.getPath();
            final Map<String, IOException> failures = getNodeFailures(pPath);
            if (!failures.containsKey(path)) {
                if (ch == null) {
                    failures.put(path, new IOException(format("No open channel for %s found!", pPath)));
                } else {
                    try {
                        ch.write(pBuffer);
                    } catch (final IOException e) {
                        failures.put(path, e);
                    }
                }
            }
        }
    }

    @Override
    public void discard(final GlobalPath pPath, final IOException pFailure) throws IOException {
        synchronized (failures) {

        }
    }

    @Override
    public void store(final GlobalPath pPath) throws IOException {
        synchronized (failures) {

        }
    }
}
