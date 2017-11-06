package ch.sourcepond.io.distributor.impl;

import ch.sourcepond.io.distributor.impl.lock.LockMessage;
import com.hazelcast.core.ILock;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Boolean.TRUE;
import static java.util.concurrent.TimeUnit.SECONDS;

class DistributionLock implements MessageListener<LockMessage>, MembershipListener {
    private static final int MAX_TRIALS = 5;
    private final Lock lock = new ReentrantLock();
    private final Condition allNodesResponded = lock.newCondition();
    private final Map<Member, Object> responses = new HashMap<>();
    private final String path;
    private final ILock globalLock;
    private final ITopic<LockMessage> lockResponseTopic;
    private int trials;

    public DistributionLock(final String pPath,
                            final ILock pGlobalLock,
                            final ITopic<LockMessage> pLockResponseTopic,
                            final Collection<Member> pMembers) {
        path = pPath;
        globalLock = pGlobalLock;
        lockResponseTopic = pLockResponseTopic;
        for (final Member node : pMembers) {
            responses.put(node, null);
        }
    }

    @Override
    public void memberAdded(final MembershipEvent membershipEvent) {
        // noop
    }

    @Override
    public void memberRemoved(final MembershipEvent membershipEvent) {
        lock.lock();
        try {
            responses.remove(membershipEvent.getMember());
        } finally {
            allNodesResponded.signal();
            lock.unlock();
        }
    }

    @Override
    public void memberAttributeChanged(final MemberAttributeEvent memberAttributeEvent) {
        // noop
    }

    @Override
    public void onMessage(final Message<LockMessage> message) {
        final LockMessage response = message.getMessageObject();
        if (path.equals(response.getPath())) {
            lock.lock();
            try {
                responses.put(message.getPublishingMember(), response.getFailureOrNull() == null ? TRUE : response.getFailureOrNull());
            } finally {
                trials = 0;
                allNodesResponded.signal();
                lock.unlock();
            }
        }
    }

    private boolean allResponsesReceived() throws TimeoutException {
        if (trials++ > MAX_TRIALS) {
            throw new TimeoutException();
        }
        for (final Object value : responses.values()) {
            if (value == null) {
                return false;
            }
        }
        return true;
    }

    public void awaitLocalLocks() throws InterruptedException, TimeoutException {
        lock.lock();
        try {
            while (!allResponsesReceived()) {
                allNodesResponded.await(300, SECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    public void releaseLocalLocks() {

    }
}
