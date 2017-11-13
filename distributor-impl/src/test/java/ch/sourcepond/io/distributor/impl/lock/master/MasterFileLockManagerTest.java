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

import ch.sourcepond.io.distributor.impl.lock.FileLockMessage;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.ITopic;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MasterFileLockManagerTest {
    private static final String ANY_PATH = "anyPath";
    private static final String ANY_MEMBERSHIP_ID = "anyMembershipId";
    private static final String ANY_LISTENER_ID = "anyListenerId";
    private final Cluster cluster = mock(Cluster.class);
    private final ITopic<String> sendFileLockRequestTopic = mock(ITopic.class);
    private final ITopic<FileLockMessage> receiveFileLockResponseTopic = mock(ITopic.class);
    private final ITopic<String> sendFileUnlockRequstTopic = mock(ITopic.class);
    private final ITopic<String> receiveFileUnlockResponseTopic = mock(ITopic.class);
    private final MasterResponseListener<FileLockMessage> masterFileLockResponseListener = mock(MasterResponseListener.class);
    private final MasterResponseListener<String> masterFileUnlockResponseListener = mock(MasterResponseListener.class);
    private final MasterResponseListenerFactory factory = mock(MasterResponseListenerFactory.class);
    private final MasterFileLockManager manager = new MasterFileLockManager(factory);

    @Before
    public void setup() {
        when(factory.getCluster()).thenReturn(cluster);
        when(factory.getReceiveFileLockResponseTopic()).thenReturn(receiveFileLockResponseTopic);
        when(factory.getReceiveFileUnlockResponseTopic()).thenReturn(receiveFileUnlockResponseTopic);
        when(factory.getSendFileLockRequestTopic()).thenReturn(sendFileLockRequestTopic);
        when(factory.getSendFileUnlockRequestTopic()).thenReturn(sendFileUnlockRequstTopic);
        when(factory.createLockListener(ANY_PATH)).thenReturn(masterFileLockResponseListener);
        when(factory.createUnlockListener(ANY_PATH)).thenReturn(masterFileUnlockResponseListener);
    }

    private <T> void verifyPerformAction(final ITopic<T> receiveTopic, final ITopic<String> sendTopic,
                                   final MasterResponseListener<T> listener) throws Exception {
        final InOrder order = inOrder(cluster, receiveTopic, sendTopic, listener);
        order.verify(cluster).addMembershipListener(listener);
        order.verify(receiveTopic).addMessageListener(listener);
        order.verify(sendTopic).publish(ANY_PATH);
        order.verify(listener).awaitNodeAnswers();
        order.verify(receiveTopic).removeMessageListener(ANY_LISTENER_ID);
        order.verify(cluster).removeMembershipListener(ANY_MEMBERSHIP_ID);
    }

    @Test
    public void acquireGlobalFileLock() throws Exception {
        when(cluster.addMembershipListener(masterFileLockResponseListener)).thenReturn(ANY_MEMBERSHIP_ID);
        when(receiveFileLockResponseTopic.addMessageListener(masterFileLockResponseListener)).thenReturn(ANY_LISTENER_ID);
        manager.acquireGlobalFileLock(ANY_PATH);
        verifyPerformAction(receiveFileLockResponseTopic, sendFileLockRequestTopic, masterFileLockResponseListener);
    }

    @Test
    public void releaseGlobalFileLock() throws Exception {
        when(cluster.addMembershipListener(masterFileUnlockResponseListener)).thenReturn(ANY_MEMBERSHIP_ID);
        when(receiveFileUnlockResponseTopic.addMessageListener(masterFileUnlockResponseListener)).thenReturn(ANY_LISTENER_ID);
        manager.releaseGlobalFileLock(ANY_PATH);
        verifyPerformAction(receiveFileUnlockResponseTopic, sendFileUnlockRequstTopic, masterFileUnlockResponseListener);
    }

    @Test
    public void releaseGlobalFileLockFailureOccurred() throws Exception {
        doThrow(TimeoutException.class).when(masterFileUnlockResponseListener).awaitNodeAnswers();
        when(cluster.addMembershipListener(masterFileUnlockResponseListener)).thenReturn(ANY_MEMBERSHIP_ID);
        when(receiveFileUnlockResponseTopic.addMessageListener(masterFileUnlockResponseListener)).thenReturn(ANY_LISTENER_ID);

        // This should not cause an exception
        manager.releaseGlobalFileLock(ANY_PATH);
    }
}
