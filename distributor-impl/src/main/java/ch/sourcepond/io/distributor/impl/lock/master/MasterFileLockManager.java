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
package ch.sourcepond.io.distributor.impl.lock.master;

import ch.sourcepond.io.distributor.impl.common.ResponseAwaitingManager;
import ch.sourcepond.io.distributor.impl.common.master.MasterListener;
import ch.sourcepond.io.distributor.impl.common.master.MasterResponseListener;
import ch.sourcepond.io.distributor.impl.common.master.StatusResponse;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.ITopic;
import org.slf4j.Logger;

import java.nio.channels.FileLock;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The singleton instance of this class is responsible for managing file-locks
 * (see {@link java.nio.channels.FileLock}) in a cluster.
 */
public class MasterFileLockManager extends ResponseAwaitingManager {

    @FunctionalInterface
    private interface ClusterAction<T> {

        void perform(MasterListener pListener, ITopic<String> pSenderTopic, ITopic<T> pReceiverTopic);
    }

    private static final Logger LOG = getLogger(MasterFileLockManager.class);
    private final MasterResponseListenerFactory factory;

    /**
     * Creates a new instance of this class.
     *
     * @param pFactory Listener-factory, must not be {@code null}
     */
    MasterFileLockManager(final Cluster pCluster,
                          final ITopic<StatusResponse> pResponseTopic,
                          final MasterResponseListenerFactory pFactory) {
        super(pCluster, pResponseTopic);
        assert pFactory != null : "pFactory is null";
        factory = pFactory;
    }

    /**
     * Acquires on all known cluster-nodes a {@link java.nio.channels.FileLock} for the path specified. This method
     * blocks until all nodes have responded to the request. If the path does not exist on a node, it will be created
     * and locked.
     *
     * @param pPath Path to be locked on all nodes, must not be {@code null}.
     * @throws TimeoutException  Thrown, if the lock acquisition timed out for a node.
     * @throws FileLockException Thrown, if the lock acquisition failed on some node.
     */
    public void acquireGlobalFileLock(final String pPath) throws TimeoutException, FileLockException {
        performAction(factory.getSendFileLockRequestTopic(), pPath,
                factory.createLockListener(pPath, cluster.getMembers()));
    }

    /**
     * Releases on all known cluster-nodes the file-locks for the path specified (see {@link FileLock#release()}). If
     * no locks exist, nothing happens.
     *
     * @param pPath Path to be released on all nodes, must not be {@code null}
     */
    public void releaseGlobalFileLock(final String pPath) {
        try {
            performAction(factory.getSendFileUnlockRequestTopic(), pPath,
                    factory.createUnlockListener(pPath, cluster.getMembers()));
        } catch (final TimeoutException | FileUnlockException e) {
            LOG.warn(format("Exception occurred while releasing file-lock for %s", pPath), e);
        }
    }
}
