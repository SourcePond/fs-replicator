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
package ch.sourcepond.io.distributor.impl;

import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.Message;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.currentThread;

/**
 * Base implementation of the {@link MasterResponseListener} interface. It is responsible for proper monitor handling
 * and signalling.
 */
public abstract class MasterListener<E extends Exception> implements MasterResponseListener<E> {
    static final int MAX_TRIALS = 5;
    private final Lock lock = new ReentrantLock();
    private final Condition answerReceived = lock.newCondition();
    protected final String path;
    private final long timeout;
    private final TimeUnit unit;
    private int trials;

    protected MasterListener(final String pPath, final long pTimeout, final TimeUnit pUnit) {
        path = pPath;
        timeout = pTimeout;
        unit = pUnit;
    }

    protected abstract void memberRemoved(Member pRemovedMember);

    @Override
    public final void memberRemoved(final MembershipEvent membershipEvent) {
        lock.lock();
        try {
            memberRemoved(membershipEvent.getMember());
        } finally {
            answerReceived.signalAll();
            lock.unlock();
        }
    }

    @Override
    public final void memberAdded(final MembershipEvent membershipEvent) {
        // noop
    }

    @Override
    public final void memberAttributeChanged(final MemberAttributeEvent memberAttributeEvent) {
        // noop
    }

    protected abstract boolean hasOpenAnswers();

    private boolean checkOpenAnswers() throws TimeoutException {
        if (trials++ > MAX_TRIALS) {
            throw new TimeoutException();
        }
        return hasOpenAnswers();
    }

    protected void throwValidationException(Exception pCause) throws E {
        // noop by default
    }

    protected void validateAnswers() throws E {
        // noop by default
    }

    public final void awaitNodeAnswers() throws TimeoutException, E {
        lock.lock();
        try {
            try {
                while (checkOpenAnswers()) {
                    answerReceived.await(timeout, unit);
                }
            } catch (final InterruptedException e) {
                currentThread().interrupt();
                throwValidationException(e);
            }
            validateAnswers();
        } finally {
            lock.unlock();
        }
    }

    protected abstract void processMessage(Message<StatusResponseMessage> pMessage);

    @Override
    public final void onMessage(final Message<StatusResponseMessage> message) {
        final String path = message.getMessageObject().getPath();

        // Only do something if the path matches
        if (this.path.equals(path)) {
            lock.lock();
            try {
                processMessage(message);
            } finally {
                answerReceived.signalAll();
                lock.unlock();
            }
        }
    }
}
