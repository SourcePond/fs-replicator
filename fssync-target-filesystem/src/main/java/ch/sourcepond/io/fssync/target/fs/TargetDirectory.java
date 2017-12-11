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
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.String.format;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isSameFile;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.slf4j.LoggerFactory.getLogger;

class TargetDirectory implements SyncTarget, AutoCloseable, Runnable {
    private static final Logger LOG = getLogger(TargetDirectory.class);
    private final ConcurrentMap<SyncPath, FileHandle> handles = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watchDogExecutor;
    private final SyncTargetConfig syncTargetConfig;
    private final Path syncDir;
    private ServiceRegistration<SyncTarget> registration;

    @Inject
    public TargetDirectory(final SyncTargetConfig pSyncTargetConfig,
                           final ScheduledExecutorService pWatchDogExecutor,
                           final Path pSyncDir) {
        syncTargetConfig = pSyncTargetConfig;
        watchDogExecutor = pWatchDogExecutor;
        syncDir = pSyncDir;
    }

    public void setRegistration(final ServiceRegistration<SyncTarget> pRegistration) {
        registration = pRegistration;
    }

    public void start() {
        watchDogExecutor.scheduleAtFixedRate(this,
                syncTargetConfig.forceUnlockSchedulePeriod(),
                syncTargetConfig.forceUnlockSchedulePeriod(),
                syncTargetConfig.forceUnlockSchedulePeriodUnit());
    }

    @Override
    public void run() {
        handles.values().removeIf(value -> value.closeExpired(syncTargetConfig));
    }

    private FileHandle createHandle(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        final Path syncDir = this.syncDir.getFileSystem().getPath(pPath.getSyncDir());
        final Path targetFile = syncDir.resolve(pPath.getPath());

        if (!targetFile.startsWith(syncDir)) {
            throw new IOException(format("%s is not relative! File-handle could not be created for node-info %s and sync-path %s",
                    pPath.getPath(), pNodeInfo, pPath));
        }

        if (!isDirectory(targetFile.getParent())) {
            createDirectories(targetFile.getParent());
        }

        final FileChannel channel = open(targetFile, CREATE, WRITE);
        try {
            channel.lock();
        } catch (final Exception e) {
            try {
                channel.close();
            } finally {
                throw new IOException(format("%s could not be locked!", targetFile), e);
            }
        }
        return new FileHandle(this, pNodeInfo, pPath, channel, targetFile);
    }

    private FileHandle getHandle(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        final FileHandle handle = handles.get(pPath);
        if (handle == null) {
            throw new IOException(format("%s is not locked!", pPath));
        }
        return handle;
    }

    private void process(final NodeInfo pNodeInfo, final SyncPath pPath, final TargetFunction pFunction) throws IOException {
        if (pNodeInfo.isLocalNode()) {
            final Path sourceSyncDir = syncDir.getFileSystem().getPath(pPath.getSyncDir());
            final Path targetSyncDir = syncDir.resolve(pPath.getSyncDir());

            if (isSameFile(sourceSyncDir, targetSyncDir)) {
                LOG.debug("Ignore local event because [source: {}] points to the same file as [target: {}]", sourceSyncDir, targetSyncDir);
                return;
            }
        }
        pFunction.process();
    }

    @Override
    public void lock(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        // The file-lock is acquired automatically when the handle is created.
        process(pNodeInfo, pPath, () -> handles.put(pPath, createHandle(pNodeInfo, pPath)));
    }

    @Override
    public void unlock(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        process(pNodeInfo, pPath, () -> handles.computeIfPresent(pPath, (k, v) -> {
            v.close();
            return null;
        }));
    }

    @Override
    public void delete(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        process(pNodeInfo, pPath, () -> getHandle(pNodeInfo, pPath).delete());
    }

    @Override
    public void transfer(final NodeInfo pNodeInfo, final SyncPath pPath, final ByteBuffer pBuffer) throws IOException {
        process(pNodeInfo, pPath, () -> getHandle(pNodeInfo, pPath).transfer(pBuffer));
    }

    @Override
    public void discard(final NodeInfo pNodeInfo, final SyncPath pPath, final IOException pFailure) throws IOException {
        process(pNodeInfo, pPath, () -> LOG.warn(format("Discard %s because fswatch-node %s transmitted a failure",
                getHandle(pNodeInfo, pPath), pNodeInfo.getSender()), pFailure));
    }

    @Override
    public void store(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        process(pNodeInfo, pPath, () -> getHandle(pNodeInfo, pPath).close());
    }

    @Override
    public void cancel(final NodeInfo pNodeInfo) {
        handles.entrySet().removeIf(entry -> {
            final boolean remove = pNodeInfo.equals(entry.getValue().getNodeInfo());
            if (remove) {
                entry.getValue().close();
            }
            return remove;
        });
    }

    public void remove(final SyncPath pSyncPath) {
        handles.remove(pSyncPath);
    }

    @Override
    public void close() {
        registration.unregister();
        watchDogExecutor.shutdown();
        handles.values().forEach(ch -> ch.close());
    }
}
