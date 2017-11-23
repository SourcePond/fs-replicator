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

import ch.sourcepond.io.distributor.impl.annotations.Response;
import ch.sourcepond.io.distributor.impl.binding.TimeoutConfig;
import ch.sourcepond.io.distributor.impl.common.StatusMessage;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

final class ClusterResponseBarrierImpl<T extends Serializable> implements MessageListener<StatusMessage>, MembershipListener,
        ClusterResponseBarrier<T> {
    private final Lock lock = new ReentrantLock();
    private final Condition answerReceived = lock.newCondition();
    private final String path;
    private final HazelcastInstance hci;
    private final ITopic<StatusMessage> responseTopic;
    private final ITopic<T> requestTopic;
    private final TimeoutConfig responseTimeoutConfig;
    private final Map<Member, Object> responses = new HashMap<>();

    ClusterResponseBarrierImpl(final String pPath,
                               final HazelcastInstance pHci,
                               final ITopic<StatusMessage> pResponseTopic,
                               final ITopic<T> pRequestTopic,
                               @Response final TimeoutConfig pResponseTimeoutConfig) {
        path = pPath;
        hci = pHci;
        responseTopic = pResponseTopic;
        requestTopic = pRequestTopic;
        responseTimeoutConfig = pResponseTimeoutConfig;
        for (final Member member : pHci.getCluster().getMembers()) {
            responses.put(member, null);
        }
    }

    @Override
    public final void memberRemoved(final MembershipEvent membershipEvent) {
        lock.lock();
        try {
            responses.remove(membershipEvent.getMember());
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

    private boolean hasOpenAnswers() {
        if (responses.isEmpty()) {
            return false;
        }
        for (final Object e : responses.values()) {
            if (e == null) {
                return true;
            }
        }
        return false;
    }

    private Map<Member, IOException> collectMemberExceptions() {
        Map<Member, IOException> exceptions = null;
        for (final Map.Entry<Member, Object> e : responses.entrySet()) {
            final Object value = e.getValue();
            if (value instanceof IOException) {
                if (exceptions == null) {
                    exceptions = new HashMap<>();
                }
                exceptions.put(e.getKey(), (IOException) value);
            }
        }
        return exceptions == null ? emptyMap() : exceptions;
    }

    private void validateAnswers() throws ResponseException {
        final Map<Member, IOException> memberExceptions = collectMemberExceptions();
        if (!memberExceptions.isEmpty()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("\nFailures:\n\t");
            for (final Map.Entry<Member, IOException> entry : memberExceptions.entrySet()) {
                builder.append(entry.getKey()).append(": ").append(entry.getValue().getMessage()).append("\n\t");
            }
            builder.append("See logs on members for further information.");
            throw new ResponseException(builder.toString());
        }
    }

    private void awaitNodeAnswers() throws TimeoutException, ResponseException {
        lock.lock();
        try {
            try {
                while (hasOpenAnswers()) {
                    if (!answerReceived.await(responseTimeoutConfig.getTimeout(), responseTimeoutConfig.getUnit())) {
                        throw new TimeoutException(format("Waiting for node responses timed-out after %d %s",
                                responseTimeoutConfig.getTimeout(), responseTimeoutConfig.getTimeout()));
                    }
                }
            } catch (final InterruptedException e) {
                currentThread().interrupt();
                throw new ResponseException("Wait for response interrupted!", e);
            }
            validateAnswers();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final void onMessage(final Message<StatusMessage> pMessage) {
        // Only do something if the path matches
        if (this.path.equals(pMessage.getMessageObject().getPath())) {
            lock.lock();
            try {
                final StatusMessage message = pMessage.getMessageObject();
                final IOException failure = message.getFailureOrNull();
                responses.replace(pMessage.getPublishingMember(), failure == null ? TRUE : failure);
            } finally {
                answerReceived.signalAll();
                lock.unlock();
            }
        }
    }

    @Override
    public void awaitResponse(final T pMessage) throws TimeoutException, ResponseException {
        requireNonNull(pMessage, "message is null");
        final Cluster cluster = hci.getCluster();
        final String membershipId = cluster.addMembershipListener(this);
        try {
            final String registrationId = responseTopic.addMessageListener(this);
            try {
                requestTopic.publish(pMessage);
                awaitNodeAnswers();
            } finally {
                responseTopic.removeMessageListener(registrationId);
            }
        } finally {
            cluster.removeMembershipListener(membershipId);
        }
    }
}
