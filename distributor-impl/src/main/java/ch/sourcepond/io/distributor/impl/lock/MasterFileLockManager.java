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
package ch.sourcepond.io.distributor.impl.lock;

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
class MasterFileLockManager {

    @FunctionalInterface
    private interface ClusterAction<T> {

        void perform(BaseMasterResponseListener<T> pListener, ITopic<String> pSenderTopic, ITopic<T> pReceiverTopic);
    }

    private static final Logger LOG = getLogger(MasterFileLockManager.class);
    private final MasterResponseListenerFactory factory;

    /**
     * Creates a new instance of this class.
     *
     * @param pFactory Listener-factory, must not be {@code null}
     */
    MasterFileLockManager(final MasterResponseListenerFactory pFactory) {
        assert pFactory != null : "pFactory is null";
        factory = pFactory;
    }

    private <E> void performAction(final ITopic<String> pSenderTopic,
                                   final ITopic<E> pReceiverTopic,
                                   final String pPath,
                                   final MasterResponseListener<E> pListener)
            throws TimeoutException, FileLockException {
        requireNonNull(pPath, "Path is null");
        final String membershipId = factory.getCluster().addMembershipListener(pListener);
        try {
            final String registrationId = pReceiverTopic.addMessageListener(pListener);
            try {
                pSenderTopic.publish(pPath);
                pListener.awaitNodeAnswers();
            } finally {
                pReceiverTopic.removeMessageListener(registrationId);
            }
        } finally {
            factory.getCluster().removeMembershipListener(membershipId);
        }
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
        performAction(factory.getSendFileLockRequestTopic(), factory.getReceiveFileLockResponseTopic(), pPath,
                factory.createLockListener(pPath));
    }

    /**
     * Releases on all known cluster-nodes the file-locks for the path specified (see {@link FileLock#release()}). If
     * no locks exist, nothing happens.
     *
     * @param pPath Path to be released on all nodes, must not be {@code null}
     */
    public void releaseGlobalFileLock(final String pPath) {
        try {
            performAction(factory.getSendFileUnlockRequstTopic(), factory.getReceiveFileUnlockResponseTopic(), pPath,
                    factory.createUnlockListener(pPath));
        } catch (final TimeoutException | FileLockException e) {
            LOG.warn(format("Exception occurred while releasing file-lock for %s", pPath), e);
        }
    }
}
