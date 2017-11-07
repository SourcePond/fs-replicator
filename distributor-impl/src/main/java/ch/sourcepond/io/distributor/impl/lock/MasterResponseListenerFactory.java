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

import com.hazelcast.core.Cluster;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;

import java.util.Collection;

/**
 * Factory for creating {@link MasterResponseListener} instances.
 */
class MasterResponseListenerFactory {
    private Cluster cluster;
    private ITopic<String> sendFileLockRequestTopic;
    private ITopic<LockMessage> receiveFileLockResponseTopic;
    private ITopic<String> sendFileUnlockRequstTopic;
    private ITopic<String> receiveFileUnlockResponseTopic;

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(final Cluster pCluster) {
        cluster = pCluster;
    }

    public ITopic<String> getSendFileLockRequestTopic() {
        return sendFileLockRequestTopic;
    }

    public void setSendFileLockRequestTopic(ITopic<String> sendFileLockRequestTopic) {
        this.sendFileLockRequestTopic = sendFileLockRequestTopic;
    }

    public ITopic<LockMessage> getReceiveFileLockResponseTopic() {
        return receiveFileLockResponseTopic;
    }

    public void setReceiveFileLockResponseTopic(ITopic<LockMessage> receiveFileLockResponseTopic) {
        this.receiveFileLockResponseTopic = receiveFileLockResponseTopic;
    }

    public ITopic<String> getSendFileUnlockRequstTopic() {
        return sendFileUnlockRequstTopic;
    }

    public void setSendFileUnlockRequstTopic(ITopic<String> sendFileUnlockRequstTopic) {
        this.sendFileUnlockRequstTopic = sendFileUnlockRequstTopic;
    }

    public ITopic<String> getReceiveFileUnlockResponseTopic() {
        return receiveFileUnlockResponseTopic;
    }

    public void setReceiveFileUnlockResponseTopic(ITopic<String> receiveFileUnlockResponseTopic) {
        this.receiveFileUnlockResponseTopic = receiveFileUnlockResponseTopic;
    }

    /**
     * Creates a new instance of {@link MasterFileLockResponseListener}.
     *
     * @param pPath Path to be locked, must not be {@code null}
     * @return New instance, never {@code null}
     */
    public MasterFileLockResponseListener createLockListener(final String pPath) {
        assert pPath != null : "pPath is null";
        assert cluster != null : "cluster is null";
        return new MasterFileLockResponseListener(pPath, cluster.getMembers());
    }

    /**
     * Creates a new instance of {@link MasterFileUnlockResponseListener}.
     *
     * @param pPath Path to be unlocked, must not be {@code null}
     * @return New instance, never {@code null}
     */
    public MasterFileUnlockResponseListener createUnlockListener(final String pPath) {
        assert pPath != null : "pPath is null";
        assert cluster != null : "cluster is null";
        return new MasterFileUnlockResponseListener(pPath, cluster.getMembers());
    }
}
