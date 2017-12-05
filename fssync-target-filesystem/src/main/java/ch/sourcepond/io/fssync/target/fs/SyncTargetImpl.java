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
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.Thread.currentThread;
import static java.time.Instant.now;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.slf4j.LoggerFactory.getLogger;

class SyncTargetImpl implements SyncTarget, Runnable {
    private static final Logger LOG = getLogger(SyncTargetImpl.class);
    private final ConcurrentMap<SyncPath, FileHandle> channels = new ConcurrentHashMap();
    private final ScheduledExecutorService watchDogExecutor = newSingleThreadScheduledExecutor();
    private final FileHandleFactory fileHandleFactory;
    private final Config config;

    public SyncTargetImpl(final FileHandleFactory pFileHandleFactory, final Config pConfig) {
        fileHandleFactory = pFileHandleFactory;
        config = pConfig;
    }

    public void start() {
        watchDogExecutor.scheduleAtFixedRate(this,
                config.forceUnlockTimeoutSeconds(),
                config.forceUnlockTimeoutSeconds(),
                config.forceUnlockTimoutUnit());
    }

    public void stop() {
        watchDogExecutor.shutdownNow();
    }

    @Override
    public void run() {
        while (!currentThread().isInterrupted()) {
            final Instant now = now();
            channels.values().removeIf(value -> {
                boolean remove = value.isExpired(config, now);
                if (remove) {
                    value.close();
                }
                return remove;
            });
        }
    }

    private FileHandle getHandle(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        try {
            return channels.computeIfAbsent(pPath, p -> {
                try {
                    return fileHandleFactory.create(pNodeInfo, pPath);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e.getMessage(), e);
                }
            });
        } catch (final UncheckedIOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public void lock(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        // The file-lock is acquired automatically when the handle is created.
        getHandle(pNodeInfo, pPath);
    }

    @Override
    public void unlock(final NodeInfo pNodeInfo, final SyncPath pPath) {
        channels.computeIfPresent(pPath, (k, v) -> {
            v.close();
            return null;
        });
    }

    @Override
    public void delete(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        getHandle(pNodeInfo, pPath).delete();
    }

    @Override
    public void transfer(final NodeInfo pNodeInfo, final SyncPath pPath, final ByteBuffer pBuffer) throws IOException {
        getHandle(pNodeInfo, pPath).transfer(pBuffer);
    }

    @Override
    public void discard(final NodeInfo pNodeInfo, final SyncPath pPath, final IOException pFailure) throws IOException {
        LOG.warn(String.format("Discard %s because fswatch-node %s transmitted a failure",
                getHandle(pNodeInfo, pPath), pNodeInfo.getSender()), pFailure);
    }

    @Override
    public void store(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        getHandle(pNodeInfo, pPath).store();
    }

    @Override
    public void cancel(final NodeInfo pNodeInfo) {
        channels.entrySet().removeIf(entry -> {
            final boolean remove = pNodeInfo.equals(entry.getValue().getNodeInfo());
            if (remove) {
                entry.getValue().close();
            }
            return remove;
        });
    }
}
