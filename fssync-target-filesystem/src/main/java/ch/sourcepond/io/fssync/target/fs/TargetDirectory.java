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

import ch.sourcepond.io.fssync.common.api.SyncPath;
import ch.sourcepond.io.fssync.target.api.NodeInfo;
import ch.sourcepond.io.fssync.target.api.SyncTarget;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.String.format;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.FileSystems.getDefault;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isSameFile;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.osgi.service.component.annotations.ConfigurationPolicy.OPTIONAL;
import static org.slf4j.LoggerFactory.getLogger;

@Component(configurationPolicy = OPTIONAL, service = SyncTarget.class)
@Designate(ocd = Config.class, factory = true)
public class TargetDirectory implements SyncTarget {
    private static final Logger LOG = getLogger(TargetDirectory.class);
    private static final Set<String> SYNC_DIRS = new HashSet<>();
    private final ConcurrentMap<SyncPath, FileHandle> handles = new ConcurrentHashMap<>();
    private final FileSystem fs;
    private final ScheduledExecutorService watchDogExecutor;
    private volatile Config config;
    private Path syncDir;

    public TargetDirectory() {
        fs = getDefault();
        watchDogExecutor = newSingleThreadScheduledExecutor();
    }

    private FileHandle createHandle(final NodeInfo pNodeInfo, final SyncPath pPath) throws IOException {
        final Path syncDir = fs.getPath(pPath.getSyncDir());
        final Path targetFile = syncDir.resolve(pPath.getRelativePath());

        if (!targetFile.startsWith(syncDir)) {
            throw new IOException(format("%s is not relative! File-handle could not be created for node-info %s and sync-path %s",
                    pPath.getRelativePath(), pNodeInfo, pPath));
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

    @Activate
    public void activate(final Config pConfig) {
        synchronized (SYNC_DIRS) {
            if (!SYNC_DIRS.add(pConfig.syncDir())) {
                throw new IllegalArgumentException(format("Sync-dir %s is already used by another component!", pConfig.syncDir()));
            }
        }
        config = pConfig;
        watchDogExecutor.scheduleAtFixedRate(() -> handles.values().removeIf(value -> value.closeExpired(config)),
                pConfig.forceUnlockTimeout(),
                pConfig.forceUnlockSchedulePeriod(),
                pConfig.forceUnlockSchedulePeriodUnit());
        syncDir = fs.getPath(pConfig.syncDir());
    }

    @Deactivate
    public void deactivate() throws InterruptedException {
        synchronized (SYNC_DIRS) {
            SYNC_DIRS.remove(config.syncDir());
        }
        watchDogExecutor.shutdown();
        while (!watchDogExecutor.isTerminated()) {
            watchDogExecutor.awaitTermination(100, MILLISECONDS);
        }
        handles.values().forEach(ch -> ch.close());
    }
}
