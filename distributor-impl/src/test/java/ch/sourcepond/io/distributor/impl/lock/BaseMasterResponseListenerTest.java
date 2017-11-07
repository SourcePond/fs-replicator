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

import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
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

public abstract class BaseMasterResponseListenerTest<T> {
    protected static final String EXPECTED_PATH = "anyPath";
    protected static final long EXPECTED_TIMOUT = 500;
    protected static final TimeUnit EXPECTED_UNIT = MILLISECONDS;
    protected final Member member = mock(Member.class);
    protected final Collection<Member> members = asList(member);
    private final MembershipEvent event = mock(MembershipEvent.class);
    private ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
    private BaseMasterResponseListener<T> listener;
    private volatile boolean run;

    @Before
    public void setup() {
        when(event.getMember()).thenReturn(member);
        listener = createListener();
        interrupted();
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    protected abstract BaseMasterResponseListener<T> createListener();

    protected abstract T createMessagePayload();

    public abstract void verifyMemberRemoved();

    public abstract void verifyHasOpenAnswers();

    public abstract void verifyToPath();

    public abstract void verifyProcessMessage();

    @Test(timeout = 2000)
    public void memberRemoved() throws Exception {
        executor.schedule(() -> {
            listener.memberRemoved(event);
            run = true;
        }, 500, MILLISECONDS);
        listener.awaitNodeAnswers();
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
            listener.awaitNodeAnswers();
            fail("Exception expected");
        } catch (final FileLockException e) {
            final Throwable cause = e.getCause();
            assertNotNull(cause);
            assertSame(InterruptedException.class, cause.getClass());
        }
    }


    @Test(timeout = 5000, expected = TimeoutException.class)
    public void awaitNodeAnswersWaitTimedOut() throws Exception {
        listener.awaitNodeAnswers();
    }

    @Test
    public void validateAnswers() throws Exception {
        listener.validateAnswers();
        verifyZeroInteractions(member, event);
    }

    @Test
    public void onMessage() throws Exception {
        executor.schedule(() -> {
            final Message<T> message = mock(Message.class);
            when(message.getPublishingMember()).thenReturn(member);
            final T payload = createMessagePayload();
            when(message.getMessageObject()).thenReturn(payload);
            listener.onMessage(message);
            run = true;
        }, 500, MILLISECONDS);
        listener.awaitNodeAnswers();
        assertTrue(run);
    }
}
