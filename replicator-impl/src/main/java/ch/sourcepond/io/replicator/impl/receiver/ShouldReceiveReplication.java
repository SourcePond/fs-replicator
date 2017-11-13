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
package ch.sourcepond.io.replicator.impl.receiver;

import ch.sourcepond.io.distributor.api.Distributor;
import ch.sourcepond.io.distributor.spi.LockException;
import ch.sourcepond.io.distributor.spi.Receiver;
import ch.sourcepond.io.distributor.spi.Storage;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.nio.channels.FileChannel.open;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.slf4j.LoggerFactory.getLogger;

public class ShouldReceiveReplication implements Receiver {

    private static final Storage EMPTY_STORAGE = new Storage() {

        @Override
        public void store(final ByteBuffer pBuffer) {
            LOG.debug("noop EMPTY_STORAGE.store(ByteBuffer)");
        }

        @Override
        public void store(final byte[] pData) {
            LOG.debug("noop EMPTY_STORAGE::store(byte[])");
        }
    };

    private static final Logger LOG = getLogger(ShouldReceiveReplication.class);
    private final Map<String, Map<String, StorageImpl>> storages = new HashMap<>();
    private final Distributor distributor;
    private final FileSystem fileSystem;

    public ShouldReceiveReplication(final Distributor pDistributor, final FileSystem pFileSystem) {
        distributor = pDistributor;
        fileSystem = pFileSystem;
    }

    private Map<String, StorageImpl> getNodeStorage(final String pNode) {
        return storages.computeIfAbsent(pNode, n -> new HashMap<>());
    }

    private boolean isLocalNode(final String pNode) {
        return pNode.equals(distributor.getLocalNode());
    }

    public void lockLocally(final String pSendingNode, final String pPath) throws IOException {
        if (!isLocalNode(pSendingNode)) {
            synchronized (storages) {
                if (storages.containsKey(pPath)) {
                    throw new LockException(format("%s/%s is already locked!", pSendingNode, pPath));
                }
                final FileChannel ch = open(fileSystem.getPath(pPath), CREATE, TRUNCATE_EXISTING);
                ch.lock();
                getNodeStorage(pSendingNode).put(pPath, new StorageImpl(ch));
            }
        }
    }

    @Override
    public void unlockAllLocally(final String pSendingNode) {
        synchronized (storages) {
            final Map<String, StorageImpl> storagesPerNode = storages.remove(pSendingNode);
            if (storagesPerNode != null && !storagesPerNode.isEmpty()) {
                storagesPerNode.values().forEach(s -> close(s));
            }
        }
    }

    private static void close(final StorageImpl pStorage) {
        try {
            pStorage.close();
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    public void unlockLocally(final String pSendingNode, final String pPath) {
        if (!isLocalNode(pSendingNode)) {
            final StorageImpl storage;
            synchronized (storages) {
                storage = getNodeStorage(pSendingNode).remove(pPath);
            }
            if (storage == null) {
                LOG.warn("unlockLocally: no storage registered for {}/{}", pSendingNode, pPath);
            } else {
                close(storage);
            }
        }
    }

    @Override
    public Storage getStorage(final String pSendingNode, final String pPath) {
        Storage storage;
        if (isLocalNode(pSendingNode)) {
            storage = EMPTY_STORAGE;
        } else {
            synchronized (storages) {
                storage = getNodeStorage(pSendingNode).get(pPath);
            }
            if (storage == null) {
                storage = EMPTY_STORAGE;
                LOG.warn("getStorage: no storage registered for {}/{}", pSendingNode, pPath);
            }
        }
        return storage;
    }
}
