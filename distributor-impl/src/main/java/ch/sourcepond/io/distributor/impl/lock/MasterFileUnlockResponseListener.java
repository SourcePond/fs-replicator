package ch.sourcepond.io.distributor.impl.lock;

import com.hazelcast.core.Member;
import com.hazelcast.core.Message;

import java.util.Set;

class MasterFileUnlockResponseListener extends MasterResponseListener<String> {
    private final Set<Member> members;

    public MasterFileUnlockResponseListener(final String pPath, final Set<Member> pMembers) {
        super(pPath);
        members = pMembers;
    }

    @Override
    protected void memberRemoved(final Member pRemovedMember) {
        members.remove(pRemovedMember);
    }

    @Override
    protected boolean hasOpenAnswers() {
        return !members.isEmpty();
    }

    @Override
    protected String toPath(final String pMessage) {
        return pMessage;
    }

    @Override
    protected void processMessage(final Message<String> pMessage) {
        members.remove(pMessage.getPublishingMember());
    }
}
