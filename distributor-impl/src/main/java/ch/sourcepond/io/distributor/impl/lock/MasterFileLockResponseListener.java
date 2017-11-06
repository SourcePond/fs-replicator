package ch.sourcepond.io.distributor.impl.lock;

import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.Message;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyMap;

class MasterFileLockResponseListener extends MasterResponseListener<LockMessage> implements MembershipListener {
    private final Map<Member, Object> responses = new HashMap<>();

    MasterFileLockResponseListener(final String pPath,
                                   final Collection<Member> pMembers) {
        super(pPath);
        for (final Member member : pMembers) {
            responses.put(member, null);
        }
    }

    @Override
    protected void memberRemoved(final Member pRemovedMember) {
        responses.remove(pRemovedMember);
    }

    @Override
    protected String toPath(final LockMessage pMessage) {
        return pMessage.getPath();
    }

    @Override
    protected boolean hasOpenAnswers() {
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

    @Override
    protected void validateAnswers() throws FileLockException {
        final Map<Member, IOException> memberExceptions = collectMemberExceptions();
        if (!memberExceptions.isEmpty()) {
            final StringBuilder builder = new StringBuilder("Acquiring file-locks failed! Failures:\n\t");
            for (final Map.Entry<Member, IOException> entry : memberExceptions.entrySet()) {
                builder.append(entry.getKey()).append(": ").append(entry.getValue().getMessage()).append("\n\t");
            }
            builder.append("See logs on members for further information.");
            throw new FileLockException(builder.toString());
        }
    }

    @Override
    protected void processMessage(final Message<LockMessage> pMessage) {
        final LockMessage message = pMessage.getMessageObject();
        final IOException failure = message.getFailureOrNull();
        responses.replace(pMessage.getPublishingMember(), failure == null ? TRUE : failure);
    }
}
