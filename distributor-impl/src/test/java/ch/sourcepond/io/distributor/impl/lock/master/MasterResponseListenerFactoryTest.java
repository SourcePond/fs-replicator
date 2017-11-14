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

import ch.sourcepond.io.distributor.impl.MasterResponseListener;
import ch.sourcepond.io.distributor.impl.StatusResponseMessage;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.ITopic;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MasterResponseListenerFactoryTest {
    private static final String ANY_PATH = "anyPath";
    private final Cluster cluster = mock(Cluster.class);
    private ITopic<String> sendFileLockRequestTopic = mock(ITopic.class);
    private ITopic<StatusResponseMessage> receiveFileLockResponseTopic = mock(ITopic.class);
    private ITopic<String> sendFileUnlockRequstTopic = mock(ITopic.class);
    private ITopic<StatusResponseMessage> receiveFileUnlockResponseTopic = mock(ITopic.class);
    private final MasterResponseListenerFactory factory = new MasterResponseListenerFactory();

    @Before
    public void setup() {
        factory.setCluster(cluster);
    }

    @Test
    public void getCluster() {
        assertSame(cluster, factory.getCluster());
    }

    @Test
    public void getSetReceiveFileLockResponseTopic() {
        factory.setReceiveFileLockResponseTopic(receiveFileLockResponseTopic);
        assertSame(receiveFileLockResponseTopic, factory.getReceiveFileLockResponseTopic());
    }

    @Test
    public void getSetReceiveFileUnlockResponseTopic() {
        factory.setReceiveFileUnlockResponseTopic(receiveFileUnlockResponseTopic);
        assertSame(receiveFileUnlockResponseTopic, factory.getReceiveFileUnlockResponseTopic());
    }

    @Test
    public void getSetSendFileLockRequestTopic() {
        factory.setSendFileLockRequestTopic(sendFileLockRequestTopic);
        assertSame(sendFileLockRequestTopic, factory.getSendFileLockRequestTopic());
    }

    @Test
    public void getSetSendFileUnlockRequstTopic() {
        factory.setSendFileUnlockRequstTopic(sendFileUnlockRequstTopic);
        assertSame(sendFileUnlockRequstTopic, factory.getSendFileUnlockRequestTopic());
    }

    private void verifyInstances(final MasterResponseListener<?> l1, final MasterResponseListener<?> l2) {
        assertNotNull(l1);
        assertNotNull(l2);
        assertSame(l1.getClass(), l2.getClass());
        assertNotSame(l1, l2);
        verify(cluster, times(2)).getMembers();
    }

    @Test(expected = AssertionError.class)
    public void createLockListenerPathIsNull() {
        factory.createLockListener(null);
    }

    @Test(expected = AssertionError.class)
    public void createUnlockListenerPathIsNull() {
        factory.createUnlockListener(null);
    }

    @Test
    public void createLockListener() {
        verifyInstances(factory.createLockListener(ANY_PATH), factory.createLockListener(ANY_PATH));
    }

    @Test
    public void createUnlockListener() {
        verifyInstances(factory.createUnlockListener(ANY_PATH), factory.createUnlockListener(ANY_PATH));
    }
}
