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
package ch.sourcepond.io.distributor.impl.response;

import ch.sourcepond.io.distributor.impl.common.StatusMessage;
import ch.sourcepond.io.distributor.spi.TimeoutConfig;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class StatusMessageListenerImplTest {
    private static final String EXPECTED_FAILURE_MESSAGE = "someMessage";
    private static final String EXPECTED_PATH = "anyPath";
    private static final long EXPECTED_TIMEOUT = 500;
    private static final TimeUnit EXPECTED_UNIT = MILLISECONDS;
    private final ITopic<String> requestTopic = mock(ITopic.class);
    private final ITopic<StatusMessage> responseTopic = mock(ITopic.class);
    private final TimeoutConfig timeoutConfig = mock(TimeoutConfig.class);
    private final Member member = mock(Member.class);
    private final Cluster cluster = mock(Cluster.class);
    private final Set<Member> members = new HashSet<>(asList(member));
    private final Message<StatusMessage> message = mock(Message.class);
    private final MembershipEvent event = mock(MembershipEvent.class);
    private final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
    private final StatusResponseListenerImpl<String> listener = new StatusResponseListenerImpl<>(
            EXPECTED_PATH, requestTopic, responseTopic, timeoutConfig, cluster);
    private StatusMessage payload = new StatusMessage(EXPECTED_PATH);
    private volatile boolean run;

    @Before
    public void setup() {
        when(timeoutConfig.getLockTimeout()).thenReturn(EXPECTED_TIMEOUT);
        when(timeoutConfig.getLockTimeoutUnit()).thenReturn(EXPECTED_UNIT);
        when(message.getPublishingMember()).thenReturn(member);
        when(message.getMessageObject()).thenReturn(payload);
        when(event.getMember()).thenReturn(member);
        when(cluster.getMembers()).thenReturn(members);
        interrupted();
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    @Test//(timeout = 2000)
    public void memberRemoved() throws Exception {
        executor.schedule(() -> {
            listener.memberRemoved(event);
            run = true;
        }, 500, MILLISECONDS);
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
        executor.schedule(() -> thread.interrupt(), 500, MILLISECONDS);
        try {
            listener.awaitResponse(EXPECTED_PATH);
            fail("Exception expected");
        } catch (final StatusResponseException e) {
            final Throwable cause = e.getCause();
            assertNotNull(cause);
            assertSame(InterruptedException.class, cause.getClass());
        }
    }

    @Test(timeout = 2000)
    public void validateAnswers() throws Exception {
        final IOException expected = new IOException(EXPECTED_FAILURE_MESSAGE);
        payload = new StatusMessage(EXPECTED_PATH, expected);
        when(message.getMessageObject()).thenReturn(payload);
        listener.onMessage(message);
        try {
            listener.awaitResponse(EXPECTED_PATH);
            fail("Exception expected");
        } catch (final StatusResponseException e) {
            assertTrue(expected.getMessage().contains(EXPECTED_FAILURE_MESSAGE));
        }
    }

    @Test(timeout = 5000, expected = TimeoutException.class)
    public void awaitNodeAnswersWaitTimedOut() throws Exception {
        listener.awaitResponse(EXPECTED_PATH);
    }

    @Test
    public void onMessage() throws Exception {
        executor.schedule(() -> {
            listener.onMessage(message);
            run = true;
        }, 500, MILLISECONDS);
        listener.awaitResponse(EXPECTED_PATH);
        assertTrue(run);
    }
}
