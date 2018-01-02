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
package ch.sourcepond.io.fssync.distributor.hazelcast.response;

import ch.sourcepond.io.fssync.distributor.hazelcast.common.StatusMessage;
import ch.sourcepond.io.fssync.distributor.hazelcast.config.DistributorConfig;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_PATH;
import static ch.sourcepond.io.fssync.distributor.hazelcast.Constants.EXPECTED_SYNC_DIR;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ClusterResponseBarrierImplTest {
    private static final String EXPECTED_FAILURE_MESSAGE = "someMessage";
    private static final String EXPECTED_MEMBERSHIP_ID = "someMembershipId";
    private static final String EXPECTED_REGISTRATION_ID = "someRegistrationId";
    private static final long EXPECTED_TIMEOUT = 500;
    private static final TimeUnit EXPECTED_UNIT = MILLISECONDS;
    private final ITopic<String> requestTopic = mock(ITopic.class);
    private final ITopic<StatusMessage> responseTopic = mock(ITopic.class);
    private final DistributorConfig config = mock(DistributorConfig.class);
    private final HazelcastInstance hci = mock(HazelcastInstance.class);
    private final Member member = mock(Member.class);
    private final Cluster cluster = mock(Cluster.class);
    private final Set<Member> members = new HashSet<>(asList(member));
    private final Message<StatusMessage> message = mock(Message.class);
    private final MembershipEvent event = mock(MembershipEvent.class);
    private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
    private StatusMessage payload = new StatusMessage(EXPECTED_SYNC_DIR, EXPECTED_PATH);
    private final ClusterResponseBarrierFactory factory = new ClusterResponseBarrierFactory(hci, config, responseTopic);
    private ClusterResponseBarrierImpl<String> listener;
    private volatile boolean run;

    @Before
    public void setup() {
        when(hci.getCluster()).thenReturn(cluster);
        when(cluster.getMembers()).thenReturn(members);
        when(config.responseTimeout()).thenReturn(EXPECTED_TIMEOUT);
        when(config.responseTimeoutUnit()).thenReturn(EXPECTED_UNIT);
        when(message.getPublishingMember()).thenReturn(member);
        when(message.getMessageObject()).thenReturn(payload);
        when(event.getMember()).thenReturn(member);
        listener = (ClusterResponseBarrierImpl<String>) factory.create(EXPECTED_PATH, requestTopic);
        when(cluster.addMembershipListener(listener)).thenReturn(EXPECTED_MEMBERSHIP_ID);
        when(responseTopic.addMessageListener(listener)).thenReturn(EXPECTED_REGISTRATION_ID);
    }

    @After
    public void tearDown() {
        executor.shutdown();
        interrupted();
    }

    @Test//(timeout = 2000)
    public void memberRemoved() throws Exception {
        executor.schedule(() -> {
            listener.memberRemoved(event);
            run = true;
        }, 200, MILLISECONDS);
        listener.awaitResponse(EXPECTED_PATH);
        assertTrue(run);
    }

    @Test
    public void memberAdded() {
        listener.memberAdded(event);
        verifyZeroInteractions(event);
    }

    @Test
    public void memberAttributeChanged() {
        final MemberAttributeEvent event = mock(MemberAttributeEvent.class);
        listener.memberAttributeChanged(event);
        verifyZeroInteractions(event);
    }

    @Test(timeout = 2000)
    public void awaitNodeAnswersWaitInterrupted() throws Exception {
        final Thread thread = currentThread();
        executor.schedule(() -> thread.interrupt(), 200, MILLISECONDS);
        try {
            listener.awaitResponse(EXPECTED_PATH);
            fail("Exception expected");
        } catch (final ResponseException e) {
            final Throwable cause = e.getCause();
            assertNotNull(cause);
            assertSame(InterruptedException.class, cause.getClass());
        }
    }

    @Test(timeout = 2000)
    public void validateAnswers() throws Exception {
        final IOException expected = new IOException(EXPECTED_FAILURE_MESSAGE);
        payload = new StatusMessage(EXPECTED_SYNC_DIR, EXPECTED_PATH, expected);
        when(message.getMessageObject()).thenReturn(payload);
        listener.onMessage(message);
        try {
            listener.awaitResponse(EXPECTED_PATH);
            fail("Exception expected");
        } catch (final ResponseException e) {
            assertTrue(expected.getMessage().contains(EXPECTED_FAILURE_MESSAGE));
        }
    }

    @Test(timeout = 5000, expected = TimeoutException.class)
    public void awaitNodeAnswersWaitTimedOut() throws Exception {
        listener.awaitResponse(EXPECTED_PATH);
    }

    @Test(expected = NullPointerException.class)
    public void awaitResponseMessageIsNull() throws Exception {
        listener.awaitResponse(null);
    }

    @Test
    public void onMessage() throws Exception {
        executor.schedule(() -> {
            listener.onMessage(message);
            run = true;
        }, 200, MILLISECONDS);
        listener.awaitResponse(EXPECTED_PATH);
        assertTrue(run);
        final InOrder order = inOrder(cluster, responseTopic);
        verify(cluster).addMembershipListener(listener);
        verify(responseTopic).addMessageListener(listener);
        verify(responseTopic).removeMessageListener(EXPECTED_REGISTRATION_ID);
        verify(cluster).removeMembershipListener(EXPECTED_MEMBERSHIP_ID);
    }
}
